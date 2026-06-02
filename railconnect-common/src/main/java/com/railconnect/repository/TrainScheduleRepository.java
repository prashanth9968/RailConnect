package com.railconnect.repository;

import com.railconnect.entity.TrainSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TrainScheduleRepository extends JpaRepository<TrainSchedule, Long> {
    Optional<TrainSchedule> findByTrainIdAndJourneyDate(Long trainId, LocalDate date);

    @Query("SELECT ts FROM TrainSchedule ts WHERE ts.journeyDate = :date")
    List<TrainSchedule> findAllByDate(LocalDate date);
}
