package com.railconnect.repository;

import com.railconnect.entity.TrainRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TrainRouteRepository extends JpaRepository<TrainRoute, Long> {
    List<TrainRoute> findByTrainIdOrderByStopNumber(Long trainId);

    @Query("SELECT tr FROM TrainRoute tr WHERE tr.train.id = :trainId AND tr.station.stationCode IN :stationCodes ORDER BY tr.stopNumber")
    List<TrainRoute> findRouteSegments(Long trainId, List<String> stationCodes);
}
