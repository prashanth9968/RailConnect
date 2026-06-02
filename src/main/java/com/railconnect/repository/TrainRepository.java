package com.railconnect.repository;

import com.railconnect.entity.Train;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TrainRepository extends JpaRepository<Train, Long> {
    Optional<Train> findByTrainNumber(String trainNumber);

    @Query("""
        SELECT DISTINCT t FROM Train t
        JOIN t.routes r1 ON r1.station.stationCode = :fromCode
        JOIN t.routes r2 ON r2.station.stationCode = :toCode
        WHERE r1.stopNumber < r2.stopNumber
        AND t.active = true
        AND (t.runningDays & :dayBit) > 0
    """)
    List<Train> findTrainsBetweenStations(String fromCode, String toCode, int dayBit);
}
