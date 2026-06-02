package com.railconnect.repository;

import com.railconnect.entity.TrainFare;
import com.railconnect.enums.CoachClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TrainFareRepository extends JpaRepository<TrainFare, Long> {
    @Query("SELECT f FROM TrainFare f WHERE f.train.id = :trainId AND f.fromStop <= :fromStop AND f.toStop >= :toStop AND f.coachClass = :coachClass")
    Optional<TrainFare> findFare(Long trainId, Integer fromStop, Integer toStop, CoachClass coachClass);
}
