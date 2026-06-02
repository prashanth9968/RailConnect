package com.railconnect.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data @Builder
public class AvailabilityInfo {
    private int availableSeats;
    private int racSeats;
    private int waitlistCount;
    private int tatkalAvailable;
    private int premiumTatkalAvailable;
    private BigDecimal baseFare;
    private BigDecimal tatkalFare;
    private BigDecimal premiumTatkalFare;
    private String status; // AVAILABLE, RAC, WL, NOT_AVAILABLE
}
