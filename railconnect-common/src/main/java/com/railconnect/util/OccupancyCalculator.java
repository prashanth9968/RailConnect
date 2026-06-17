package com.railconnect.util;

import java.time.DayOfWeek;
import java.time.LocalDate;

public class OccupancyCalculator {

    public static double calculateOccupancy(String trainType, LocalDate date) {
        double baseOccupancy = 0.7; // default
        if (trainType != null) {
            switch (trainType.toUpperCase()) {
                case "RAJDHANI":
                    baseOccupancy = 0.85 + Math.random() * 0.10; // 85% to 95%
                    break;
                case "VANDE_BHARAT":
                    baseOccupancy = 0.70 + Math.random() * 0.20; // 70% to 90%
                    break;
                case "SHATABDI":
                    baseOccupancy = 0.65 + Math.random() * 0.20; // 65% to 85%
                    break;
                case "DURONTO":
                    baseOccupancy = 0.75 + Math.random() * 0.15; // 75% to 90%
                    break;
                case "EXPRESS":
                case "SUPERFAST":
                default:
                    baseOccupancy = 0.60 + Math.random() * 0.20; // 60% to 80%
                    break;
            }
        }

        // Day of week adjustment: weekends Fri, Sat, Sun are more busy
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.FRIDAY || dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            baseOccupancy = Math.min(0.98, baseOccupancy + 0.08);
        }

        // Season adjustment: holiday months (May, June, October, November, December)
        int month = date.getMonthValue();
        if (month == 5 || month == 6 || month == 10 || month == 11 || month == 12) {
            baseOccupancy = Math.min(0.98, baseOccupancy + 0.05);
        }

        return baseOccupancy;
    }
}
