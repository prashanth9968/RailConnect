package com.railconnect.entity;
import com.railconnect.enums.ClassType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
@Entity @Table(name = "seat_inventory")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class SeatInventory extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false) private TrainSchedule schedule;
    @Enumerated(EnumType.STRING) @Column(name = "class_type") private ClassType classType;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_station_id") private Station fromStation;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_station_id") private Station toStation;
    @Column(name = "total_seats") private int totalSeats;
    @Column(name = "available_seats") private int availableSeats;
    @Builder.Default @Column(name = "waitlisted_count") private int waitlistedCount = 0;
    @Builder.Default @Column(name = "tatkal_available") private int tatkalAvailable = 0;
    @Builder.Default @Column(name = "premium_tatkal_available") private int premiumTatkalAvailable = 0;
    private BigDecimal fare;
    @Column(name = "tatkal_fare") private BigDecimal tatkalFare;
    @Column(name = "premium_tatkal_fare") private BigDecimal premiumTatkalFare;
}
