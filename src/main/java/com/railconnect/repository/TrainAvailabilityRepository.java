package com.railconnect.repository;

import com.railconnect.entity.TrainAvailability;
import com.railconnect.enums.ClassType;
import com.railconnect.enums.QuotaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrainAvailabilityRepository extends JpaRepository<TrainAvailability, UUID> {

    Optional<TrainAvailability> findByTrainIdAndJourneyDateAndClassTypeAndQuotaType(
        UUID trainId, LocalDate journeyDate, ClassType classType, QuotaType quotaType);

    List<TrainAvailability> findByTrainIdAndJourneyDate(UUID trainId, LocalDate journeyDate);

    @Modifying
    @Query("UPDATE TrainAvailability a SET a.availableSeats = a.availableSeats - :count WHERE a.train.id = :trainId AND a.journeyDate = :date AND a.classType = :classType AND a.quotaType = :quotaType AND a.availableSeats >= :count")
    int decrementAvailability(@Param("trainId") UUID trainId, @Param("date") LocalDate date,
                               @Param("classType") ClassType classType, @Param("quotaType") QuotaType quotaType,
                               @Param("count") int count);
}
