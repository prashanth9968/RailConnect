package com.railconnect.entity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
@Entity @Table(name = "train_location_history")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TrainLocationHistory extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false) private TrainSchedule schedule;
    @Column(nullable = false) private BigDecimal latitude;
    @Column(nullable = false) private BigDecimal longitude;
    @Column(name = "speed_kmh") private BigDecimal speedKmh;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id") private Station station;
    private String status;
    @Builder.Default @Column(name = "recorded_at") private Instant recordedAt = Instant.now();
}
