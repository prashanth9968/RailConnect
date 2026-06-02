package com.railconnect.dto.response;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
@Data @Builder
public class TrainLocationResponse {
    private String trainNumber;
    private String trainName;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String currentStation;
    private String nextStation;
    private String status;
    private int delayMinutes;
    private BigDecimal speedKmh;
    private Instant lastUpdated;
    private String estimatedArrival;
    private double progressPercent;
}
