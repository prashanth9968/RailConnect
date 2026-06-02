package com.railconnect.entity;
import com.railconnect.enums.ClassType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
@Entity @Table(name = "train_classes")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TrainClass extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_id", nullable = false) private Train train;
    @Enumerated(EnumType.STRING) @Column(name = "class_type", nullable = false) private ClassType classType;
    @Column(name = "total_seats", nullable = false) private int totalSeats;
    @Column(name = "coach_count", nullable = false) private int coachCount;
    @Column(name = "base_fare_per_km", nullable = false) private BigDecimal baseFarePerKm;
    @Builder.Default @Column(name = "tatkal_charge_flat") private BigDecimal tatkalChargeFlat = BigDecimal.ZERO;
    @Builder.Default @Column(name = "premium_tatkal_charge_flat") private BigDecimal premiumTatkalChargeFlat = BigDecimal.ZERO;
}
