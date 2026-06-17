package com.railconnect.repository;

import com.railconnect.entity.Train;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TrainRepository extends JpaRepository<Train, Long> {
    Optional<Train> findByTrainNumber(String trainNumber);

    @Query(value = """
        SELECT DISTINCT t.* FROM trains t
        JOIN train_routes r1 ON r1.train_id = t.id
        JOIN stations s1 ON s1.id = r1.station_id
        JOIN train_routes r2 ON r2.train_id = t.id
        JOIN stations s2 ON s2.id = r2.station_id
        WHERE s1.station_code = :fromCode
        AND s2.station_code = :toCode
        AND r1.stop_number < r2.stop_number
        AND t.active = true
        AND (t.running_days & :dayBit) > 0
    """, nativeQuery = true)
    List<Train> findTrainsBetweenStations(String fromCode, String toCode, int dayBit);
}
