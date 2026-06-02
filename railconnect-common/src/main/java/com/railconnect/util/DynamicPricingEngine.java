package com.railconnect.util;

import com.railconnect.enums.SeatClass;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class DynamicPricingEngine {

    /**
     * Calculates the dynamic price multiplier based on occupancy rate.
     * For Premium Tatkal, fare increases dynamically:
     * - Base multiplier is 1.30x (up to 20% occupancy).
     * - 20% to 50% occupancy: 1.40x.
     * - 50% to 80% occupancy: 1.60x.
     * - Above 80% occupancy: 2.00x.
     */
    public BigDecimal calculateDynamicMultiplier(int totalSeats, int bookedSeats) {
        if (totalSeats <= 0) return BigDecimal.ONE;
        double occupancyRate = (double) bookedSeats / totalSeats;
        
        double multiplierValue = 1.30;
        if (occupancyRate >= 0.8) {
            multiplierValue = 2.00;
        } else if (occupancyRate >= 0.5) {
            multiplierValue = 1.60;
        } else if (occupancyRate >= 0.2) {
            multiplierValue = 1.40;
        }
        
        return BigDecimal.valueOf(multiplierValue).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Integrates base fare and dynamically scaled premium tatkal surcharges.
     */
    public BigDecimal calculatePremiumTatkalFare(BigDecimal baseFare, BigDecimal tatkalCharge, BigDecimal dynamicMultiplier) {
        BigDecimal premiumCharge = tatkalCharge.multiply(dynamicMultiplier);
        return baseFare.add(premiumCharge).setScale(2, RoundingMode.HALF_UP);
    }
}
