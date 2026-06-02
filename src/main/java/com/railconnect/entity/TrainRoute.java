package com.railconnect.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;

@Entity
@Table(name = "train_routes", indexes = {
    @Index(name = "idx_route_train", columnList = "train_id"),
    @Index(name = "idx_route_station", columnList = "station_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrainRoute {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_id", nullable = false)
    private Train train;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    private int stopNumber;          // 1 = source, last = destination
    private LocalTime arrivalTime;
    private LocalTime departureTime;
    private int distanceFromSource;  // km
    private int dayNumber;           // 1 = day of departure
    private int haltMinutes;
}
