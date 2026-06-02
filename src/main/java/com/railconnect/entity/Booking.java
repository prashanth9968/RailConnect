package com.railconnect.entity;

import com.railconnect.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "bookings", indexes = {
    @Index(name = "idx_booking_user", columnList = "user_id"),
    @Index(name = "idx_booking_pnr", columnList = "pnrNumber"),
    @Index(name = "idx_booking_train_date", columnList = "train_id, journeyDate")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Booking {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 10)
    private String pnrNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "train_id", nullable = false)
    private Train train;

    private LocalDate journeyDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "source_station_id")
    private Station sourceStation;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "destination_station_id")
    private Station destinationStation;

    @Enumerated(EnumType.STRING)
    private SeatClass seatClass;

    @Enumerated(EnumType.STRING)
    private QuotaType quotaType = QuotaType.GENERAL;

    @Enumerated(EnumType.STRING)
    private BookingStatus status = BookingStatus.PENDING;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Passenger> passengers;

    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL)
    private Payment payment;

    @OneToOne(mappedBy = "booking", cascade = CascadeType.ALL)
    private Cancellation cancellation;

    private BigDecimal baseFare;
    private BigDecimal tatkalCharge;
    private BigDecimal serviceTax;
    private BigDecimal totalAmount;
    private BigDecimal refundAmount;

    private String boardingStationCode;
    private boolean smsAlert = true;
    private boolean emailAlert = true;

    @CreationTimestamp private LocalDateTime bookedAt;
    @UpdateTimestamp  private LocalDateTime updatedAt;
}
