package com.railconnect.service;

import com.railconnect.config.KafkaConfig;
import com.railconnect.entity.Booking;
import com.railconnect.entity.Passenger;
import com.railconnect.enums.BookingStatus;
import com.railconnect.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class AutoPromotionService {

    private final BookingRepository bookingRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Promotes RAC and Waiting List passengers when a seat is freed up.
     */
    @Transactional
    public boolean promotePassengers(Long trainId, LocalDate journeyDate, String coachNumber, String seatNumber) {
        log.info("Starting auto-promotion for trainId={}, date={}, freed seat={}-{}", trainId, journeyDate, coachNumber, seatNumber);

        // 1. Find all RAC bookings for the train and date
        List<Booking> racBookings = bookingRepository.findByTrainIdAndJourneyDateAndStatus(trainId, journeyDate, BookingStatus.RAC);
        
        // 2. Find all Waitlisted bookings for the train and date
        List<Booking> wlBookings = bookingRepository.findByTrainIdAndJourneyDateAndStatus(trainId, journeyDate, BookingStatus.WAITLISTED);

        // Sort RAC passengers by waitlistNumber (acting as RAC queue index here)
        List<Passenger> racPassengers = new ArrayList<>();
        for (Booking b : racBookings) {
            for (Passenger p : b.getPassengers()) {
                if ("RAC".equalsIgnoreCase(p.getBookingStatus()) || (p.getBookingStatus() != null && p.getBookingStatus().startsWith("RAC"))) {
                    racPassengers.add(p);
                }
            }
        }
        racPassengers.sort(Comparator.comparingInt(Passenger::getWaitlistNumber));

        // Sort WL passengers
        List<Passenger> wlPassengers = new ArrayList<>();
        for (Booking b : wlBookings) {
            for (Passenger p : b.getPassengers()) {
                if (p.getBookingStatus() != null && p.getBookingStatus().startsWith("WL")) {
                    wlPassengers.add(p);
                }
            }
        }
        wlPassengers.sort(Comparator.comparingInt(Passenger::getWaitlistNumber));

        // Case A: There are RAC passengers. Promote the first RAC passenger to CONFIRMED.
        if (!racPassengers.isEmpty()) {
            Passenger promotedRac = racPassengers.get(0);
            Booking bookingToPromote = promotedRac.getBooking();
            
            // Assign seat
            promotedRac.setCoachNumber(coachNumber);
            promotedRac.setSeatNumber(seatNumber);
            promotedRac.setBookingStatus("CONFIRMED");
            promotedRac.setWaitlistNumber(0);
            
            // If all passengers in this booking are confirmed, update status
            boolean allConfirmed = bookingToPromote.getPassengers().stream()
                    .allMatch(p -> "CONFIRMED".equalsIgnoreCase(p.getBookingStatus()));
            if (allConfirmed) {
                bookingToPromote.setStatus(BookingStatus.CONFIRMED);
            } else {
                bookingToPromote.setStatus(BookingStatus.PARTIALLY_CONFIRMED);
            }
            bookingRepository.save(bookingToPromote);
            
            log.info("Promoted RAC passenger {} to CONFIRMED. Assigned seat {}-{}", promotedRac.getName(), coachNumber, seatNumber);
            
            // Trigger Kafka ticket confirmed event
            publishTicketConfirmedEvent(bookingToPromote.getId(), "PROMOTED");

            // Now, we promote the first WL passenger to take the vacant RAC slot.
            if (!wlPassengers.isEmpty()) {
                Passenger promotedWl = wlPassengers.get(0);
                Booking wlBookingToPromote = promotedWl.getBooking();
                
                promotedWl.setBookingStatus("RAC");
                promotedWl.setWaitlistNumber(racPassengers.size()); // Put at the end of RAC queue
                
                wlBookingToPromote.setStatus(BookingStatus.RAC);
                bookingRepository.save(wlBookingToPromote);
                
                log.info("Promoted WL passenger {} to RAC", promotedWl.getName());
                publishTicketConfirmedEvent(wlBookingToPromote.getId(), "RAC_UPGRADE");

                // Shift remaining WL passengers up
                shiftWaitlistQueue(wlPassengers, 1);
            }

            // Shift remaining RAC queue up
            shiftRacQueue(racPassengers, 1);
            return true;

        } else if (!wlPassengers.isEmpty()) {
            // Case B: No RAC passengers, but there are WL passengers. Promote the first WL directly to CONFIRMED.
            Passenger promotedWl = wlPassengers.get(0);
            Booking bookingToPromote = promotedWl.getBooking();
            
            promotedWl.setCoachNumber(coachNumber);
            promotedWl.setSeatNumber(seatNumber);
            promotedWl.setBookingStatus("CONFIRMED");
            promotedWl.setWaitlistNumber(0);
            
            bookingToPromote.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(bookingToPromote);
            
            log.info("Promoted WL passenger {} directly to CONFIRMED. Assigned seat {}-{}", promotedWl.getName(), coachNumber, seatNumber);
            publishTicketConfirmedEvent(bookingToPromote.getId(), "PROMOTED");

            // Shift remaining WL passengers up
            shiftWaitlistQueue(wlPassengers, 1);
            return true;
        }
        return false;
    }

    private void shiftWaitlistQueue(List<Passenger> wlPassengers, int startIdx) {
        for (int i = startIdx; i < wlPassengers.size(); i++) {
            Passenger p = wlPassengers.get(i);
            int newWlNum = i;
            p.setBookingStatus("WL/" + newWlNum);
            p.setWaitlistNumber(newWlNum);
            bookingRepository.save(p.getBooking());
        }
    }

    private void shiftRacQueue(List<Passenger> racPassengers, int startIdx) {
        for (int i = startIdx; i < racPassengers.size(); i++) {
            Passenger p = racPassengers.get(i);
            int newRacNum = i;
            p.setBookingStatus("RAC/" + newRacNum);
            p.setWaitlistNumber(newRacNum);
            bookingRepository.save(p.getBooking());
        }
    }

    private void publishTicketConfirmedEvent(UUID bookingId, String status) {
        try {
            kafkaTemplate.send(KafkaConfig.TICKET_CONFIRMED_TOPIC, Map.of(
                "bookingId", bookingId.toString(),
                "status", status
            ));
        } catch (Exception e) {
            log.error("Failed to publish promotion event to Kafka: {}", e.getMessage());
        }
    }
}
