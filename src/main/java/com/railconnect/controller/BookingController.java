package com.railconnect.controller;

import com.railconnect.dto.request.*;
import com.railconnect.dto.response.*;
import com.railconnect.security.CustomUserDetails;
import com.railconnect.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Bookings", description = "Ticket booking, PNR status, and cancellation")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @Operation(summary = "Initiate a new booking")
    public ResponseEntity<ApiResponse<BookingResponse>> initiateBooking(
            @Valid @RequestBody BookingRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        BookingResponse response = bookingService.initiateBooking(request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Booking initiated. Please complete payment.", response));
    }

    @GetMapping("/pnr/{pnr}")
    @Operation(summary = "Check PNR status")
    public ResponseEntity<ApiResponse<PnrStatusResponse>> getPnrStatus(@PathVariable String pnr) {
        return ResponseEntity.ok(ApiResponse.success(bookingService.getPnrStatus(pnr)));
    }

    @GetMapping("/my-bookings")
    @Operation(summary = "Get all bookings for the logged-in user")
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> getMyBookings(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(bookingService.getUserBookings(user.getId(), pageable)));
    }

    @PostMapping("/cancel")
    @Operation(summary = "Cancel a booking")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @RequestBody CancellationRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled. Refund initiated.",
            bookingService.cancelBooking(request, user.getId())));
    }
}
