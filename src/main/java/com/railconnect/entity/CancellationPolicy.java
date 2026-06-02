package com.railconnect.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "cancellation_policies")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CancellationPolicy extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer hoursBeforeDeparture; // cancellation > X hours before

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal refundPercentage; // % of fare refunded

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal flatDeduction; // flat deduction amount

    @Column(nullable = false)
    private boolean active = true;
}
