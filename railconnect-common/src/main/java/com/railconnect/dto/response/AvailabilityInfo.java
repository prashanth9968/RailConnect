package com.railconnect.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
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
