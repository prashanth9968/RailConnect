package com.railconnect.entity;

import com.railconnect.enums.CancellationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cancellations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Cancellation {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Enumerated(EnumType.STRING)
    private CancellationStatus status = CancellationStatus.REQUESTED;

    private String reason;
    private BigDecimal cancellationCharge;
    private BigDecimal refundAmount;
    private String cancelledPassengerIds; // JSON array for partial cancellation

    @CreationTimestamp private LocalDateTime requestedAt;
    private LocalDateTime processedAt;
}
