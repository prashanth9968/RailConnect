package com.railconnect.entity;

import com.railconnect.enums.SeatStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "seats", indexes = {
    @Index(name = "idx_seat_coach", columnList = "coach_id"),
    @Index(name = "idx_seat_date", columnList = "journeyDate")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Seat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coach_id", nullable = false)
    private TrainCoach coach;

    private String seatNumber;
    private String berthType; // LOWER, MIDDLE, UPPER, SIDE_LOWER, SIDE_UPPER, WINDOW, AISLE

    @Enumerated(EnumType.STRING)
    private SeatStatus status = SeatStatus.AVAILABLE;

    private LocalDate journeyDate;

    // Optimistic locking to prevent double-booking
    @Version
    private Long version;
}
