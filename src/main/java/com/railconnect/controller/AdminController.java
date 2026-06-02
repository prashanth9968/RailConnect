package com.railconnect.controller;

import com.railconnect.entity.*;
import com.railconnect.repository.*;
import com.railconnect.service.SeatSeederService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin", description = "Admin-only operations: trains, users, analytics")
public class AdminController {

    private final TrainRepository trainRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final SeatSeederService seatSeederService;

    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard stats")
    public ResponseEntity<?> getDashboard() {
        long totalTrains    = trainRepository.count();
        long totalUsers     = userRepository.count();
        long totalBookings  = bookingRepository.count();
        return ResponseEntity.ok(Map.of(
            "totalTrains", totalTrains,
            "totalUsers", totalUsers,
            "totalBookings", totalBookings
        ));
    }

    @GetMapping("/bookings")
    @Operation(summary = "All bookings paginated")
    public ResponseEntity<?> getAllBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(bookingRepository.findAll(PageRequest.of(page, size)));
    }

    @GetMapping("/users")
    @Operation(summary = "All users paginated")
    public ResponseEntity<?> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(userRepository.findAll(PageRequest.of(page, size)));
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
