package com.railconnect.entity;

import com.railconnect.enums.CoachClass;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "train_fares",
    uniqueConstraints = @UniqueConstraint(columnNames = {"train_id","from_stop","to_stop","coach_class"})
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrainFare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_id", nullable = false)
    private Train train;

    @Column(name = "from_stop", nullable = false)
    private Integer fromStop;

    @Column(name = "to_stop", nullable = false)
    private Integer toStop;

    @Enumerated(EnumType.STRING)
    @Column(name = "coach_class", nullable = false)
    private CoachClass coachClass;

    @Column(name = "base_fare", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseFare;

    @Column(name = "reservation_charge", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal reservationCharge = BigDecimal.ZERO;
}
