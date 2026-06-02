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
    private final TrainTrackingService trackingService;

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

    @GetMapping("/{trainNumber}/live-status")
    @Operation(summary = "Get live train running status with GPS coordinates")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<TrainLiveStatusResponse>> getLiveStatus(
            @PathVariable String trainNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate queryDate = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(ApiResponse.success(trackingService.getLiveStatus(trainNumber, queryDate)));
    }

    @GetMapping("/{trainNumber}/maps-config")
    @Operation(summary = "Get Google Maps configuration for train tracking UI")
    public ResponseEntity<ApiResponse<String>> getMapsConfig(@PathVariable String trainNumber) {
        return ResponseEntity.ok(ApiResponse.success(trackingService.getGoogleMapsTrackingUrl(trainNumber)));
    }

    @GetMapping("/stations/search")
    @Operation(summary = "Search stations by name or code")
    public ResponseEntity<ApiResponse<?>> searchStations(@RequestParam String query) {
        return ResponseEntity.ok(ApiResponse.success(trainService.searchStations(query)));
    }
}
