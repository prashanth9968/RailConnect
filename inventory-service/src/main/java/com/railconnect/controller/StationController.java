package com.railconnect.controller;

import com.railconnect.dto.response.ApiResponse;
import com.railconnect.entity.Station;
import com.railconnect.repository.StationRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/stations")
@RequiredArgsConstructor
@Tag(name = "Stations", description = "Station search and details")
public class StationController {

    private final StationRepository stationRepository;

    @GetMapping("/search")
    @Cacheable(value = "stationSearch", key = "#query")
    public ResponseEntity<ApiResponse<List<Station>>> searchStations(@RequestParam String query) {
        List<Station> stations = stationRepository.searchStations(query);
        return ResponseEntity.ok(ApiResponse.success("Stations found", stations));
    }

    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<Station>> getStation(@PathVariable String code) {
        return stationRepository.findByStationCode(code)
            .map(s -> ResponseEntity.ok(ApiResponse.success("Station found", s)))
            .orElse(ResponseEntity.notFound().build());
    }
}
