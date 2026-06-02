package com.railconnect.entity;

import com.railconnect.enums.PaymentMethod;
import com.railconnect.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status = PaymentStatus.PENDING;

    private BigDecimal amount;
    private String currency = "INR";

    // Razorpay fields
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;

    // UPI fields
    private String upiTransactionId;
    private String upiVpa;

    private String failureReason;
    private BigDecimal refundedAmount;
    private String refundId;
    private LocalDateTime refundedAt;

    @CreationTimestamp private LocalDateTime createdAt;
    @UpdateTimestamp  private LocalDateTime updatedAt;
}
