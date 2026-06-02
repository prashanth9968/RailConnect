package com.railconnect.controller;

import com.railconnect.dto.request.TrainSearchRequest;
import com.railconnect.dto.response.*;
import com.railconnect.enums.SeatClass;
import com.railconnect.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/trains")
@RequiredArgsConstructor
@Tag(name = "Trains", description = "Train search, availability, and live tracking")
public class TrainController {

    private final TrainService trainService;

    @GetMapping("/search")
    @Operation(summary = "Search trains between stations")
    public ResponseEntity<ApiResponse<List<TrainSearchResponse>>> searchTrains(
            @Valid TrainSearchRequest request) {
        return ResponseEntity.ok(ApiResponse.success(trainService.searchTrains(request)));
    }

    @GetMapping("/{trainId}/availability")
    @Operation(summary = "Get seat availability for a train on a date")
    public ResponseEntity<ApiResponse<Map<String, AvailabilityInfo>>> getAvailability(
            @PathVariable Long trainId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) SeatClass seatClass) {
        return ResponseEntity.ok(ApiResponse.success(trainService.getAvailability(trainId, date, seatClass)));
    }

    @GetMapping("/stations/search")
    @Operation(summary = "Search stations by name or code")
    public ResponseEntity<ApiResponse<?>> searchStations(@RequestParam String query) {
        return ResponseEntity.ok(ApiResponse.success(trainService.searchStations(query)));
    }
}
