package com.railconnect.service.impl;
import com.railconnect.dto.request.BookingRequest;
import com.railconnect.dto.request.CancellationRequest;
import com.railconnect.dto.response.BookingResponse;
import com.railconnect.entity.*;
import com.railconnect.enums.*;
import com.railconnect.exception.*;
import com.railconnect.repository.*;
import com.railconnect.util.FareCalculator;
import com.railconnect.util.PnrGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
@Service @Slf4j @RequiredArgsConstructor
public class BookingServiceImpl {
    private final BookingRepository bookingRepository;
    private final TrainScheduleRepository scheduleRepository;
    private final SeatInventoryRepository inventoryRepository;
    private final SeatLockRepository seatLockRepository;
    private final StationRepository stationRepository;
    private final UserRepository userRepository;
    private final PnrGenerator pnrGenerator;
    private final FareCalculator fareCalculator;
    private final NotificationServiceImpl notificationService;

    @Transactional
    public BookingResponse createBooking(BookingRequest req, UUID userId) {
        TrainSchedule schedule = scheduleRepository.findById(req.getScheduleId())
            .orElseThrow(() -> new ResourceNotFoundException("Schedule", "id", req.getScheduleId()));
        Station fromStation = stationRepository.findById(req.getFromStationId())
            .orElseThrow(() -> new ResourceNotFoundException("Station", "id", req.getFromStationId()));
        Station toStation = stationRepository.findById(req.getToStationId())
            .orElseThrow(() -> new ResourceNotFoundException("Station", "id", req.getToStationId()));
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Validate Tatkal booking window
        validateBookingWindow(req.getBookingType(), schedule.getJourneyDate());

        // Check and reserve seats (pessimistic lock)
        SeatInventory inventory = inventoryRepository.findAndLock(
            req.getScheduleId(), req.getClassType(), req.getFromStationId(), req.getToStationId()
        ).orElseThrow(() -> new SeatNotAvailableException("No inventory available for this route/class"));

        int passengers = req.getPassengers().size();
        boolean isWaitlist = inventory.getAvailableSeats() < passengers;

        if (isWaitlist && inventory.getWaitlistedCount() >= 50)
            throw new SeatNotAvailableException("Waitlist is full for this class");

        // Calculate fare
        BigDecimal baseFare = inventory.getFare().multiply(BigDecimal.valueOf(passengers));
        BigDecimal tatkalCharge = BigDecimal.ZERO;
        if (req.getBookingType() == BookingType.TATKAL)
            tatkalCharge = fareCalculator.calculateTatkalCharge(req.getClassType(), passengers);
        else if (req.getBookingType() == BookingType.PREMIUM_TATKAL)
            tatkalCharge = fareCalculator.calculatePremiumTatkalCharge(req.getClassType(), passengers);

        BigDecimal subtotal = baseFare.add(tatkalCharge);
        BigDecimal gst = fareCalculator.calculateGst(subtotal);
        BigDecimal total = subtotal.add(gst).add(fareCalculator.getConvenienceFee());

        // Generate unique PNR
        String pnr;
        do { pnr = pnrGenerator.generate(); } while (bookingRepository.existsByPnr(pnr));

        // Create booking
        Booking booking = Booking.builder()
            .pnr(pnr).user(user).schedule(schedule)
            .fromStation(fromStation).toStation(toStation)
            .classType(req.getClassType()).bookingType(req.getBookingType())
            .bookingStatus(isWaitlist ? BookingStatus.WAITLISTED : BookingStatus.CONFIRMED)
            .totalPassengers(passengers).baseFare(baseFare).tatkalCharge(tatkalCharge)
            .gstAmount(gst).convenienceFee(fareCalculator.getConvenienceFee()).totalAmount(total)
            .paymentStatus(PaymentStatus.PENDING).build();
        bookingRepository.save(booking);

        // Create passengers
        List<BookingPassenger> bookingPassengers = new ArrayList<>();
        int wlNum = inventory.getWaitlistedCount() + 1;
        for (int i = 0; i < req.getPassengers().size(); i++) {
            var p = req.getPassengers().get(i);
            BigDecimal pFare = fareCalculator.applySeniorCitizenDiscount(
                inventory.getFare(), p.isSeniorCitizen(), p.getGender());
            BookingPassenger bp = BookingPassenger.builder()
                .booking(booking).passengerName(p.getName()).age(p.getAge())
                .gender(p.getGender()).idType(p.getIdType()).idNumber(p.getIdNumber())
                .berthPreference(p.getBerthPreference()).seniorCitizen(p.isSeniorCitizen())
                .status(isWaitlist ? PassengerStatus.WL : PassengerStatus.CNF)
                .waitlistNumber(isWaitlist ? wlNum++ : null).fare(pFare)
                .concessionType(p.getConcessionType()).build();
            bookingPassengers.add(bp);
        }
        booking.setPassengers(bookingPassengers);
        bookingRepository.save(booking);

        // Update inventory
        if (!isWaitlist) {
            inventoryRepository.decrementAvailableSeats(inventory.getId(), passengers);
        } else {
            for (int i = 0; i < passengers; i++) inventoryRepository.incrementWaitlist(inventory.getId());
        }

        notificationService.sendBookingConfirmation(booking);
        return mapToResponse(booking);
    }

    @Transactional
    public BookingResponse cancelBooking(CancellationRequest req, UUID userId) {
        Booking booking = bookingRepository.findById(req.getBookingId())
            .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", req.getBookingId()));
        if (!booking.getUser().getId().equals(userId))
            throw new BusinessException("Not authorized to cancel this booking", "UNAUTHORIZED");
        if (booking.getBookingStatus() == BookingStatus.CANCELLED)
            throw new BusinessException("Booking already cancelled", "ALREADY_CANCELLED");

        long hoursBeforeDeparture = ChronoUnit.HOURS.between(
            Instant.now(),
            booking.getSchedule().getJourneyDate().atTime(0,0).toInstant(ZoneOffset.UTC)
        );

        BigDecimal refund = BigDecimal.ZERO;
        if (booking.getPaymentStatus() == PaymentStatus.SUCCESS) {
            refund = fareCalculator.calculateRefundAmount(booking.getTotalAmount(), hoursBeforeDeparture);
        }

        booking.setBookingStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(req.getReason());
        booking.setCancelledAt(Instant.now());
        booking.setRefundAmount(refund);
        booking.setRefundStatus(refund.compareTo(BigDecimal.ZERO) > 0 ? "PENDING" : "NOT_APPLICABLE");
        bookingRepository.save(booking);

        // Release seats back
        inventoryRepository.findAndLock(
            booking.getSchedule().getId(), booking.getClassType(),
            booking.getFromStation().getId(), booking.getToStation().getId()
        ).ifPresent(inv -> inventoryRepository.incrementAvailableSeats(inv.getId(), booking.getTotalPassengers()));

        notificationService.sendCancellationConfirmation(booking);
        return mapToResponse(booking);
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingByPnr(String pnr) {
        return bookingRepository.findByPnr(pnr.toUpperCase())
            .map(this::mapToResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Booking", "PNR", pnr));
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> getUserBookings(UUID userId, Pageable pageable) {
        return bookingRepository.findByUserIdOrderByBookedAtDesc(userId, pageable)
            .map(this::mapToResponse);
    }

    private void validateBookingWindow(BookingType type, LocalDate journeyDate) {
        LocalDate today = LocalDate.now();
        if (type == BookingType.TATKAL && !journeyDate.equals(today.plusDays(1)))
            throw new BusinessException("Tatkal booking is only available for next day travel", "INVALID_TATKAL_DATE");
        if (type == BookingType.PREMIUM_TATKAL && !journeyDate.equals(today.plusDays(1)))
            throw new BusinessException("Premium Tatkal booking is only available for next day travel", "INVALID_PREMIUM_TATKAL_DATE");
    }

    private BookingResponse mapToResponse(Booking b) {
        List<BookingResponse.PassengerResponse> pList = b.getPassengers() == null ? List.of() :
            b.getPassengers().stream().map(p -> BookingResponse.PassengerResponse.builder()
                .id(p.getId()).name(p.getPassengerName()).age(p.getAge()).gender(p.getGender())
                .seatNumber(p.getSeatNumber()).coachNumber(p.getCoachNumber()).berthType(p.getBerthType())
                .status(p.getStatus().name()).waitlistNumber(p.getWaitlistNumber()).fare(p.getFare()).build()
            ).toList();
        return BookingResponse.builder()
            .id(b.getId()).pnr(b.getPnr())
            .trainNumber(b.getSchedule().getTrain().getTrainNumber())
            .trainName(b.getSchedule().getTrain().getTrainName())
            .fromStation(b.getFromStation().getStationName())
            .toStation(b.getToStation().getStationName())
            .journeyDate(b.getSchedule().getJourneyDate().toString())
            .classType(b.getClassType().name()).bookingType(b.getBookingType().name())
            .bookingStatus(b.getBookingStatus().name()).paymentStatus(b.getPaymentStatus().name())
            .totalAmount(b.getTotalAmount()).refundAmount(b.getRefundAmount())
            .passengers(pList).bookedAt(b.getBookedAt()).build();
    }
}
