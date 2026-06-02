package com.railconnect.entity;

import com.railconnect.enums.TrainStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "train_schedules", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"train_id", "journeyDate"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrainSchedule {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_id", nullable = false)
    private Train train;

    private LocalDate journeyDate;

    @Enumerated(EnumType.STRING)
    private TrainStatus status = TrainStatus.ON_TIME;

    private int delayMinutes = 0;

    // Live GPS
    private double currentLatitude;
    private double currentLongitude;
    private String currentStationCode;
    private String nextStationCode;
    private int speedKmph;

    @UpdateTimestamp private LocalDateTime lastUpdated;
}
