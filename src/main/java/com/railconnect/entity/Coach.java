package com.railconnect.entity;

import com.railconnect.enums.ClassType;
import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "coaches")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Coach extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_id", nullable = false)
    private Train train;

    @Column(nullable = false, length = 5)
    private String coachNumber; // e.g. S1, A1, B2

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClassType classType;

    @Column(nullable = false)
    private Integer totalSeats;

    @OneToMany(mappedBy = "coach", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Seat> seats = new ArrayList<>();
}
