package com.railconnect.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "trains", indexes = {
    @Index(name = "idx_train_number", columnList = "trainNumber")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Train {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String trainNumber;

    @Column(nullable = false)
    private String trainName;

    private String trainType; // EXPRESS, SUPERFAST, RAJDHANI, SHATABDI, VANDE_BHARAT

    @OneToMany(mappedBy = "train", cascade = CascadeType.ALL)
    private List<TrainRoute> routes;

    @OneToMany(mappedBy = "train", cascade = CascadeType.ALL)
    private List<TrainCoach> coaches;

    @OneToMany(mappedBy = "train", cascade = CascadeType.ALL)
    private List<TrainSchedule> schedules;

    // Days of operation (bitmask: Mon=1,Tue=2,...,Sun=64)
    private int runningDays;
    private boolean active = true;

    @CreationTimestamp private LocalDateTime createdAt;
}
