package com.railconnect.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stations", indexes = {
    @Index(name = "idx_station_code", columnList = "stationCode")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Station {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 10)
    private String stationCode;

    @Column(nullable = false)
    private String stationName;

    private String city;
    private String state;
    private String zone;
    private double latitude;
    private double longitude;
    private boolean active = true;
}
