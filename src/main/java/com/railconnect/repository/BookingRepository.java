package com.railconnect.repository;

import com.railconnect.entity.Booking;
import com.railconnect.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {
    Optional<Booking> findByPnrNumber(String pnrNumber);

    Page<Booking> findByUserIdOrderByBookedAtDesc(UUID userId, Pageable pageable);

    List<Booking> findByTrainIdAndJourneyDateAndStatus(Long trainId, LocalDate date, BookingStatus status);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.train.id = :trainId AND b.journeyDate = :date AND b.seatClass = :seatClass AND b.status IN ('CONFIRMED','TATKAL_CONFIRMED','PREMIUM_TATKAL_CONFIRMED')")
    int countConfirmedBookings(Long trainId, LocalDate date, String seatClass);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.train.id = :trainId AND b.journeyDate = :date AND b.seatClass = :seatClass AND b.status = 'WAITLISTED'")
    int countWaitlistedBookings(Long trainId, LocalDate date, String seatClass);
}
