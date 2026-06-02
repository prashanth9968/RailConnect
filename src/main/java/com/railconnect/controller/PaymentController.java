package com.railconnect.controller;

import com.railconnect.dto.request.*;
import com.railconnect.dto.response.*;
import com.railconnect.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Payments", description = "Payment gateway integration - GPay, PhonePe, Cards via Razorpay")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/initiate")
    @Operation(summary = "Create Razorpay order and get UPI intent URLs for GPay/PhonePe")
    public ResponseEntity<ApiResponse<PaymentInitResponse>> initiatePayment(
            @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Payment order created", paymentService.initiatePayment(request)));
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify payment signature from Razorpay callback")
    public ResponseEntity<ApiResponse<BookingResponse>> verifyPayment(
            @Valid @RequestBody PaymentVerifyRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Payment verified successfully", paymentService.verifyPayment(request)));
    }

    @PostMapping("/webhook")
    @Operation(summary = "Razorpay webhook endpoint (payment.failed, refund.created events)")
    public ResponseEntity<Void> webhook(@RequestBody String payload,
                                        @RequestHeader("X-Razorpay-Signature") String signature) {
        // Handle Razorpay webhook events (payment.failed, refund events)
        // Signature verification would be done here in production
        return ResponseEntity.ok().build();
    }
}
