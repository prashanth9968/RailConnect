package com.railconnect.repository;

import com.railconnect.entity.Station;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface StationRepository extends JpaRepository<Station, Long> {
    Optional<Station> findByStationCode(String stationCode);

    @Query("""
    SELECT s FROM Station s
    WHERE LOWER(s.stationName) LIKE LOWER(CONCAT('%', :query, '%'))
       OR LOWER(s.stationCode) LIKE LOWER(CONCAT('%', :query, '%'))
       OR LOWER(s.city) LIKE LOWER(CONCAT('%', :query, '%'))
    ORDER BY s.stationName
    """)
    List<Station> searchStations(String query);
}
