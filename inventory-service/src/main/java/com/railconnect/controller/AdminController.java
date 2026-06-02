package com.railconnect.controller;

import com.railconnect.entity.Train;
import com.railconnect.repository.TrainRepository;
import com.railconnect.service.SeatSeederService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Trains", description = "Admin-only operations for train and seat inventory management")
public class AdminController {

    private final TrainRepository trainRepository;
    private final SeatSeederService seatSeederService;

    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard stats for trains")
    public ResponseEntity<?> getDashboard() {
        long totalTrains = trainRepository.count();
        return ResponseEntity.ok(Map.of(
            "totalTrains", totalTrains
        ));
    }

    @PostMapping("/trains")
    @Operation(summary = "Add a new train")
    public ResponseEntity<?> addTrain(@RequestBody Train train) {
        return ResponseEntity.ok(trainRepository.save(train));
    }

    @PostMapping("/seed-seats")
    @Operation(summary = "Manually trigger seat seeding for next 120 days")
    public ResponseEntity<?> seedSeats() {
        seatSeederService.seedSeatsForFutureDates();
        return ResponseEntity.ok(Map.of("message", "Seat seeding triggered successfully"));
    }

    @DeleteMapping("/trains/{id}")
    @Operation(summary = "Deactivate a train")
    public ResponseEntity<?> deactivateTrain(@PathVariable Long id) {
        trainRepository.findById(id).ifPresent(t -> { t.setActive(false); trainRepository.save(t); });
        return ResponseEntity.ok(Map.of("message", "Train deactivated"));
    }
}
