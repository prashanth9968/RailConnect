package com.railconnect.entity;

import com.railconnect.enums.ClassType;
import com.railconnect.enums.QuotaType;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "train_availability", indexes = {
    @Index(name = "idx_avail_train_date_class", columnList = "train_id,journey_date,classType,quotaType", unique = true)
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrainAvailability extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "train_id", nullable = false)
    private Train train;

    @Column(name = "journey_date", nullable = false)
    private LocalDate journeyDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClassType classType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuotaType quotaType;

    @Column(nullable = false)
    private Integer totalSeats;

    @Column(nullable = false)
    private Integer availableSeats;

    @Column(nullable = false)
    private Integer waitingListCount = 0;

    @Column(nullable = false)
    private Integer racCount = 0;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal baseFare;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal tatkalFare = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean tatkalOpen = false;

    @Column(nullable = false)
    private boolean bookingOpen = true;
}
