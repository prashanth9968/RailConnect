package com.railconnect.util;

import com.railconnect.enums.QuotaType;
import com.railconnect.enums.SeatClass;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;

@Component
public class FareCalculator {

    // Base fare per km per class (INR)
    private static final java.util.Map<SeatClass, BigDecimal> BASE_FARE_PER_KM = java.util.Map.of(
        SeatClass.GN,  new BigDecimal("0.35"),
        SeatClass.SL,  new BigDecimal("0.60"),
        SeatClass.S3,  new BigDecimal("1.20"),
        SeatClass.S2,  new BigDecimal("1.85"),
        SeatClass.S1,  new BigDecimal("3.10"),
        SeatClass.CC,  new BigDecimal("0.95"),
        SeatClass.EC,  new BigDecimal("2.20")
    );

    // Tatkal charge per km
    private static final java.util.Map<SeatClass, BigDecimal> TATKAL_PER_KM = java.util.Map.of(
        SeatClass.SL,  new BigDecimal("0.10"),
        SeatClass.S3,  new BigDecimal("0.30"),
        SeatClass.S2,  new BigDecimal("0.50"),
        SeatClass.S1,  new BigDecimal("0.80"),
        SeatClass.CC,  new BigDecimal("0.20"),
        SeatClass.EC,  new BigDecimal("0.60")
    );

    public BigDecimal calculateBaseFare(SeatClass seatClass, int distanceKm, int passengerCount) {
        BigDecimal perKm = BASE_FARE_PER_KM.getOrDefault(seatClass, new BigDecimal("0.60"));
        BigDecimal fare = perKm.multiply(BigDecimal.valueOf(distanceKm));
        // Minimum fare
        BigDecimal minFare = new BigDecimal("30");
        fare = fare.max(minFare);
        return fare.multiply(BigDecimal.valueOf(passengerCount)).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateTatkalCharge(SeatClass seatClass, int distanceKm, int passengerCount) {
        BigDecimal perKm = TATKAL_PER_KM.getOrDefault(seatClass, new BigDecimal("0.10"));
        BigDecimal charge = perKm.multiply(BigDecimal.valueOf(Math.min(distanceKm, 500)));
        BigDecimal minCharge = getMinTatkalCharge(seatClass);
        BigDecimal maxCharge = getMaxTatkalCharge(seatClass);
        charge = charge.max(minCharge).min(maxCharge);
        return charge.multiply(BigDecimal.valueOf(passengerCount)).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculatePremiumTatkalCharge(SeatClass seatClass, int distanceKm, int passengerCount) {
        // Premium tatkal = 1.3x tatkal charge
        return calculateTatkalCharge(seatClass, distanceKm, passengerCount)
            .multiply(new BigDecimal("1.30")).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateServiceTax(BigDecimal baseFare) {
        return baseFare.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateCancellationCharge(BigDecimal totalFare, LocalDate journeyDate, LocalDate cancellationDate, QuotaType quotaType) {
        long daysBeforeJourney = cancellationDate.until(journeyDate).getDays();

        if (quotaType == QuotaType.TATKAL || quotaType == QuotaType.PREMIUM_TATKAL) {
            return totalFare; // No refund for tatkal
        }

        if (daysBeforeJourney > 2) {
            return totalFare.multiply(new BigDecimal("0.25")); // 25% charge
        } else if (daysBeforeJourney > 0) {
            return totalFare.multiply(new BigDecimal("0.50")); // 50% charge
        } else {
            return totalFare; // No refund within 4 hours of departure
        }
    }

    private BigDecimal getMinTatkalCharge(SeatClass sc) {
        return switch (sc) {
            case SL -> new BigDecimal("100");
            case S3 -> new BigDecimal("300");
            case S2 -> new BigDecimal("400");
            case S1 -> new BigDecimal("500");
            case CC -> new BigDecimal("125");
            default -> new BigDecimal("100");
        };
    }

    private BigDecimal getMaxTatkalCharge(SeatClass sc) {
        return switch (sc) {
            case SL -> new BigDecimal("200");
            case S3 -> new BigDecimal("500");
            case S2 -> new BigDecimal("600");
            case S1 -> new BigDecimal("900");
            case CC -> new BigDecimal("225");
            default -> new BigDecimal("200");
        };
    }

    public boolean isTatkalBookingAllowed(LocalDate journeyDate) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        // Tatkal opens at 10:00 AM one day before journey
        return journeyDate.equals(today.plusDays(1)) && now.isAfter(LocalTime.of(10, 0));
    }

    public boolean isPremiumTatkalBookingAllowed(LocalDate journeyDate) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        // Premium tatkal opens at 11:00 AM one day before
        return journeyDate.equals(today.plusDays(1)) && now.isAfter(LocalTime.of(11, 0));
    }
}
