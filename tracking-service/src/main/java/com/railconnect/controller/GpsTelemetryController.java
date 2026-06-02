package com.railconnect.controller;

import com.railconnect.dto.request.GpsTelemetryRequest;
import com.railconnect.dto.response.ApiResponse;
import com.railconnect.dto.response.TrainLiveStatusResponse;
import com.railconnect.service.TrainTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tracking")
@RequiredArgsConstructor
@Tag(name = "GPS Telemetry Ingestion", description = "Endpoints for ingesting live train GPS data")
public class GpsTelemetryController {

    private final TrainTrackingService trackingService;

    @PostMapping("/telemetry")
    @Operation(summary = "Ingest GPS telemetry coordinates, speed, and heading from trains")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<TrainLiveStatusResponse>> ingestTelemetry(
            @Valid @RequestBody GpsTelemetryRequest request) {
        return ResponseEntity.ok(ApiResponse.success(trackingService.ingestTelemetry(request)));
    }
}
