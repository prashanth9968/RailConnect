package com.railconnect.service;

import com.railconnect.dto.request.*;
import com.railconnect.dto.response.*;
import com.railconnect.entity.*;
import com.railconnect.enums.*;
import com.railconnect.exception.RailConnectException;
import com.railconnect.exception.SeatLockedException;
import com.railconnect.repository.*;
import com.railconnect.client.InventoryClient;
import com.railconnect.util.FareCalculator;
import com.railconnect.util.PNRGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.railconnect.config.KafkaConfig;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final TrainRepository trainRepository;
    private final StationRepository stationRepository;
    private final SeatRepository seatRepository;
    private final TrainRouteRepository routeRepository;
    private final PNRGenerator pnrGenerator;
    private final FareCalculator fareCalculator;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final AutoPromotionService autoPromotionService;
    private final InventoryClient inventoryClient;
    private final TrainAvailabilityRepository trainAvailabilityRepository;

    @Transactional
    @CacheEvict(value = {"availability", "trains"}, allEntries = true)
    public BookingResponse initiateBooking(BookingRequest request, UUID userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RailConnectException("User not found", HttpStatus.NOT_FOUND));
        Train train = trainRepository.findById(request.getTrainId())
            .orElseThrow(() -> new RailConnectException("Train not found", HttpStatus.NOT_FOUND));

        // Ensure physical seats exist in database (REST call to inventory-service)
        inventoryClient.ensureSeatsGenerated(train.getId(), request.getJourneyDate().toString());

        Station source = stationRepository.findByStationCode(request.getSourceStationCode())
            .orElseThrow(() -> new RailConnectException("Source station not found", HttpStatus.NOT_FOUND));
        Station dest = stationRepository.findByStationCode(request.getDestinationStationCode())
            .orElseThrow(() -> new RailConnectException("Destination station not found", HttpStatus.NOT_FOUND));

        // Validate tatkal/premium tatkal timing
        if (request.getQuotaType() == QuotaType.TATKAL && !fareCalculator.isTatkalBookingAllowed(request.getJourneyDate())) {
            throw new RailConnectException("Tatkal booking not yet open. Opens at 10:00 AM one day before journey.", HttpStatus.BAD_REQUEST);
        }
        if (request.getQuotaType() == QuotaType.PREMIUM_TATKAL && !fareCalculator.isPremiumTatkalBookingAllowed(request.getJourneyDate())) {
            throw new RailConnectException("Premium Tatkal booking not yet open. Opens at 11:00 AM one day before journey.", HttpStatus.BAD_REQUEST);
        }

        // Calculate distance
        List<TrainRoute> routes = routeRepository.findRouteSegments(train.getId(),
            List.of(source.getStationCode(), dest.getStationCode()));
        int distanceKm = routes.size() == 2
            ? Math.abs(routes.get(1).getDistanceFromSource() - routes.get(0).getDistanceFromSource()) : 500;

        // Calculate fares
        int count = request.getPassengers().size();
        BigDecimal baseFare = fareCalculator.calculateBaseFare(request.getSeatClass(), distanceKm, count);
        BigDecimal tatkalCharge = BigDecimal.ZERO;
        if (request.getQuotaType() == QuotaType.TATKAL) {
            tatkalCharge = fareCalculator.calculateTatkalCharge(request.getSeatClass(), distanceKm, count);
        } else if (request.getQuotaType() == QuotaType.PREMIUM_TATKAL) {
            tatkalCharge = fareCalculator.calculatePremiumTatkalCharge(request.getSeatClass(), distanceKm, count);
        }
        BigDecimal serviceTax = fareCalculator.calculateServiceTax(baseFare.add(tatkalCharge));
        BigDecimal totalAmount = baseFare.add(tatkalCharge).add(serviceTax);

        // Try to assign seats with optimistic locking
        try {
            List<Seat> availableSeats = seatRepository.findAvailableSeatsWithLock(
                train.getId(), request.getJourneyDate(), request.getSeatClass());

            String pnr = generateUniquePnr();
            Booking booking = Booking.builder()
                .pnrNumber(pnr)
                .user(user)
                .train(train)
                .journeyDate(request.getJourneyDate())
                .sourceStation(source)
                .destinationStation(dest)
                .seatClass(request.getSeatClass())
                .quotaType(request.getQuotaType())
                .baseFare(baseFare)
                .tatkalCharge(tatkalCharge)
                .serviceTax(serviceTax)
                .totalAmount(totalAmount)
                .boardingStationCode(request.getBoardingStationCode() != null
                    ? request.getBoardingStationCode() : source.getStationCode())
                .smsAlert(request.isSmsAlert())
                .emailAlert(request.isEmailAlert())
                .build();

            List<Passenger> passengers = buildPassengers(request, booking, availableSeats);
            booking.setPassengers(passengers);

            // Set booking status based on seat availability
            if (availableSeats.size() >= count) {
                // Try to decrement availability atomically
                int rowsUpdated = trainAvailabilityRepository.decrementAvailability(
                    train.getId(), request.getJourneyDate(), mapToClassType(request.getSeatClass()), request.getQuotaType(), count
                );
                
                if (rowsUpdated == 0) {
                    throw new RailConnectException("Seats fully booked. Please retry.", HttpStatus.BAD_REQUEST);
                }

                booking.setStatus(resolveStatus(request.getQuotaType()));
                // Lock seats
                for (int i = 0; i < count; i++) {
                    availableSeats.get(i).setStatus(SeatStatus.BOOKED);
                    seatRepository.save(availableSeats.get(i));
                }
            } else {
                // Put in waitlist
                int wlCount = bookingRepository.countWaitlistedBookings(
                    train.getId(), request.getJourneyDate(), request.getSeatClass().name());
                booking.setStatus(BookingStatus.WAITLISTED);
                passengers.forEach(p -> {
                    p.setBookingStatus("WL/" + (wlCount + 1));
                    p.setWaitlistNumber(wlCount + 1);
                });
            }

            Booking saved = bookingRepository.save(booking);
            log.info("Booking initiated: PNR={}, status={}", pnr, saved.getStatus());

            try {
                kafkaTemplate.send(KafkaConfig.BOOKING_CREATED_TOPIC, Map.of("bookingId", saved.getId().toString()));
                if (saved.getStatus() != BookingStatus.WAITLISTED) {
                    kafkaTemplate.send(KafkaConfig.SEAT_LOCKED_TOPIC, Map.of("bookingId", saved.getId().toString(), "count", String.valueOf(count)));
                }
            } catch (Exception ex) {
                log.error("Failed to publish booking created event to Kafka: {}", ex.getMessage());
            }

            return toBookingResponse(saved);

        } catch (ObjectOptimisticLockingFailureException e) {
            throw new SeatLockedException();
        }
    }

    @Transactional(readOnly = true)
    public PnrStatusResponse getPnrStatus(String pnr) {
        Booking booking = bookingRepository.findByPnrNumber(pnr)
            .orElseThrow(() -> new RailConnectException("PNR not found: " + pnr, HttpStatus.NOT_FOUND));

        return PnrStatusResponse.builder()
            .pnrNumber(booking.getPnrNumber())
            .trainNumber(booking.getTrain().getTrainNumber())
            .trainName(booking.getTrain().getTrainName())
            .journeyDate(booking.getJourneyDate())
            .fromStation(booking.getSourceStation().getStationName())
            .toStation(booking.getDestinationStation().getStationName())
            .seatClass(booking.getSeatClass().getDisplayName())
            .chartStatus(booking.getStatus())
            .passengers(booking.getPassengers().stream().map(this::toPassengerResponse).toList())
            .boardingPoint(booking.getBoardingStationCode())
            .build();
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> getUserBookings(UUID userId, Pageable pageable) {
        return bookingRepository.findByUserIdOrderByBookedAtDesc(userId, pageable)
            .map(this::toBookingResponse);
    }

    @Transactional
    @CacheEvict(value = {"availability", "trains"}, allEntries = true)
    public BookingResponse cancelBooking(CancellationRequest request, UUID userId) {
        Booking booking = bookingRepository.findByPnrNumber(request.getPnrNumber())
            .orElseThrow(() -> new RailConnectException("PNR not found", HttpStatus.NOT_FOUND));

        if (!booking.getUser().getId().equals(userId)) {
            throw new RailConnectException("Unauthorized to cancel this booking", HttpStatus.FORBIDDEN);
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new RailConnectException("Booking already cancelled", HttpStatus.BAD_REQUEST);
        }
        if (booking.getJourneyDate().isBefore(LocalDate.now())) {
            throw new RailConnectException("Cannot cancel past journey booking", HttpStatus.BAD_REQUEST);
        }

        BigDecimal cancellationCharge = fareCalculator.calculateCancellationCharge(
            booking.getTotalAmount(), booking.getJourneyDate(), LocalDate.now(), booking.getQuotaType());
        BigDecimal refundAmount = booking.getTotalAmount().subtract(cancellationCharge);

        Cancellation cancellation = Cancellation.builder()
            .booking(booking)
            .reason(request.getReason())
            .cancellationCharge(cancellationCharge)
            .refundAmount(refundAmount)
            .status(CancellationStatus.PROCESSING)
            .build();

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setRefundAmount(refundAmount);
        booking.setCancellation(cancellation);
        Booking saved = bookingRepository.save(booking);

        int count = booking.getPassengers().size();

        // Auto-promote or release seats
        booking.getPassengers().forEach(p -> {
            if (p.getSeatNumber() != null) {
                boolean promoted = autoPromotionService.promotePassengers(
                    booking.getTrain().getId(), 
                    booking.getJourneyDate(), 
                    p.getCoachNumber(), 
                    p.getSeatNumber()
                );
                if (!promoted) {
                    // Release the seat back to AVAILABLE
                    Optional<TrainCoach> coachOpt = booking.getTrain().getCoaches().stream()
                        .filter(c -> c.getCoachNumber().equals(p.getCoachNumber()))
                        .findFirst();
                    if (coachOpt.isPresent()) {
                        seatRepository.findByCoachAndDate(coachOpt.get().getId(), booking.getJourneyDate())
                            .forEach(s -> {
                                if (s.getSeatNumber().equals(p.getSeatNumber())) {
                                    s.setStatus(SeatStatus.AVAILABLE);
                                    seatRepository.save(s);
                                }
                            });
                    }
                }
            }
        });

        // Increment availability atomically!
        trainAvailabilityRepository.incrementAvailability(
            booking.getTrain().getId(), booking.getJourneyDate(), mapToClassType(booking.getSeatClass()), booking.getQuotaType(), count
        );

        try {
            kafkaTemplate.send(
                KafkaConfig.TICKET_CONFIRMED_TOPIC,
                Map.of("bookingId", booking.getId().toString(), "status", "CANCELLED", "refundAmount", refundAmount.toString())
            );
        } catch (Exception e) {
            log.error("Failed to publish booking cancellation event to Kafka: {}", e.getMessage());
        }

        log.info("Booking cancelled: PNR={}, refund={}", booking.getPnrNumber(), refundAmount);
        return toBookingResponse(saved);
    }



    private List<Passenger> buildPassengers(BookingRequest req, Booking booking, List<Seat> seats) {
        List<Passenger> passengers = new ArrayList<>();
        for (int i = 0; i < req.getPassengers().size(); i++) {
            PassengerRequest pr = req.getPassengers().get(i);
            Passenger p = new Passenger();
            p.setBooking(booking);
            p.setName(pr.getName());
            p.setAge(pr.getAge());
            p.setGender(pr.getGender());
            p.setIdType(pr.getIdType());
            p.setIdNumber(pr.getIdNumber());
            p.setBerthPreference(pr.getBerthPreference());

            if (i < seats.size()) {
                Seat seat = seats.get(i);
                p.setSeatNumber(seat.getSeatNumber());
                p.setCoachNumber(seat.getCoach().getCoachNumber());
                p.setBerthPreference(seat.getBerthType());
                p.setBookingStatus("CNF");
            } else {
                p.setBookingStatus("WL/" + (i + 1));
                p.setWaitlistNumber(i + 1);
            }
            passengers.add(p);
        }
        return passengers;
    }

    private BookingStatus resolveStatus(QuotaType quota) {
        return switch (quota) {
            case TATKAL -> BookingStatus.TATKAL_CONFIRMED;
            case PREMIUM_TATKAL -> BookingStatus.PREMIUM_TATKAL_CONFIRMED;
            default -> BookingStatus.CONFIRMED;
        };
    }

    private String generateUniquePnr() {
        String pnr;
        do { pnr = pnrGenerator.generate(); } while (bookingRepository.findByPnrNumber(pnr).isPresent());
        return pnr;
    }

    private BookingResponse toBookingResponse(Booking b) {
        return BookingResponse.builder()
            .id(b.getId())
            .pnrNumber(b.getPnrNumber())
            .trainNumber(b.getTrain().getTrainNumber())
            .trainName(b.getTrain().getTrainName())
            .journeyDate(b.getJourneyDate())
            .sourceStation(b.getSourceStation().getStationName())
            .destinationStation(b.getDestinationStation().getStationName())
            .seatClass(b.getSeatClass())
            .quotaType(b.getQuotaType())
            .status(b.getStatus())
            .passengers(b.getPassengers().stream().map(this::toPassengerResponse).toList())
            .totalAmount(b.getTotalAmount())
            .refundAmount(b.getRefundAmount())
            .paymentStatus(b.getPayment() != null ? b.getPayment().getStatus() : PaymentStatus.PENDING)
            .bookedAt(b.getBookedAt())
            .build();
    }

    private PassengerResponse toPassengerResponse(Passenger p) {
        return PassengerResponse.builder()
            .id(p.getId())
            .name(p.getName())
            .age(p.getAge())
            .gender(p.getGender())
            .seatNumber(p.getSeatNumber())
            .coachNumber(p.getCoachNumber())
            .berthType(p.getBerthPreference())
            .bookingStatus(p.getBookingStatus())
            .waitlistNumber(p.getWaitlistNumber())
            .build();
    }

    private ClassType mapToClassType(SeatClass seatClass) {
        return switch (seatClass) {
            case SL -> ClassType.SL;
            case S3 -> ClassType.THIRD_AC;
            case S2 -> ClassType.SECOND_AC;
            case S1 -> ClassType.FIRST_AC;
            case CC -> ClassType.AC_CHAIR_CAR;
            case EC -> ClassType.EXECUTIVE_CHAIR;
            default -> ClassType.SECOND_SEATING;
        };
    }
}
