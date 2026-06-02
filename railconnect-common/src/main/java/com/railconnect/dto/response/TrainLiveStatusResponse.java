package com.railconnect.dto.response;

import com.railconnect.enums.TrainStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder
public class TrainLiveStatusResponse {
    private String trainNumber;
    private String trainName;
    private LocalDate journeyDate;
    private TrainStatus status;
    private int delayMinutes;
    private double currentLatitude;
    private double currentLongitude;
    private String currentStation;
    private String nextStation;
    private int speedKmph;
    private LocalDateTime lastUpdated;
    private List<StationStatusInfo> routeStatus;
}
