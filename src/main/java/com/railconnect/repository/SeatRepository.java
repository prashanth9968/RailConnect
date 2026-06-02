package com.railconnect.repository;

import com.railconnect.entity.Seat;
import com.railconnect.enums.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.coach.train.id = :trainId AND s.journeyDate = :date AND s.coach.seatClass = :seatClass AND s.status = 'AVAILABLE' ORDER BY s.id")
    List<Seat> findAvailableSeatsWithLock(Long trainId, LocalDate date, String seatClass);

    @Query("SELECT COUNT(s) FROM Seat s WHERE s.coach.train.id = :trainId AND s.journeyDate = :date AND s.coach.seatClass = :seatClass AND s.status = :status")
    int countByTrainDateClassStatus(Long trainId, LocalDate date, String seatClass, SeatStatus status);

    @Query("SELECT s FROM Seat s WHERE s.coach.id = :coachId AND s.journeyDate = :date")
    List<Seat> findByCoachAndDate(Long coachId, LocalDate date);
}
