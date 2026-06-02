package com.railconnect.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalTime;

@Data @Builder
public class StationStatusInfo {
    private String stationCode;
    private String stationName;
    private LocalTime scheduledArrival;
    private LocalTime scheduledDeparture;
    private LocalTime actualArrival;
    private LocalTime actualDeparture;
    private String status; // DEPARTED, AT_STATION, UPCOMING
    private int delayMinutes;
}
