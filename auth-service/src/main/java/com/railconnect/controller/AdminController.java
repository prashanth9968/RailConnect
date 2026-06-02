package com.railconnect.controller;

import com.railconnect.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Users", description = "Admin-only operations for user management")
public class AdminController {

    private final UserRepository userRepository;

    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard stats for users")
    public ResponseEntity<?> getDashboard() {
        long totalUsers = userRepository.count();
        return ResponseEntity.ok(Map.of(
            "totalUsers", totalUsers
        ));
    }

    @GetMapping("/users")
    @Operation(summary = "All users paginated")
    public ResponseEntity<?> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(userRepository.findAll(PageRequest.of(page, size)));
    }
}
