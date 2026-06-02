package com.railconnect.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
@Entity @Table(name = "seat_locks")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SeatLock extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id") private TrainSchedule schedule;
    @Column(name = "class_type") private String classType;
    @Column(name = "seat_number") private String seatNumber;
    @Column(name = "coach_number") private String coachNumber;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") private User user;
    @Column(name = "locked_at") @Builder.Default private Instant lockedAt = Instant.now();
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "lock_token", unique = true) private String lockToken;
}
