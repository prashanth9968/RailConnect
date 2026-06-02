package com.railconnect.entity;

import com.railconnect.enums.SeatClass;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "train_coaches")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrainCoach {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_id", nullable = false)
    private Train train;

    private String coachNumber; // S1, S2, B1, A1, etc.

    @Enumerated(EnumType.STRING)
    private SeatClass seatClass;

    private int totalSeats;
    private int tatkalQuota;
    private int premiumTatkalQuota;
    private int ladiesQuota;
    private int seniorCitizenQuota;
    private int maxRacSeats;
    private int maxWaitlistSeats;

    @OneToMany(mappedBy = "coach", cascade = CascadeType.ALL)
    private List<Seat> seats;
}
