package com.railconnect.controller;

import com.railconnect.dto.response.ApiResponse;
import com.railconnect.dto.response.TrainLiveStatusResponse;
import com.railconnect.service.TrainTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/trains")
@RequiredArgsConstructor
@Tag(name = "Train Tracking", description = "Train live status and map configurations")
public class TrainTrackingController {

    private final TrainTrackingService trackingService;

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
}
