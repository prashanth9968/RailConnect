package com.railconnect.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Data @Builder
public class TrainSearchResponse {
    private Long trainId;
    private String trainNumber;
    private String trainName;
    private String trainType;
    private String sourceStationCode;
    private String sourceStationName;
    private LocalTime departureTime;
    private String destinationStationCode;
    private String destinationStationName;
    private LocalTime arrivalTime;
    private String duration;
    private int dayDifference;
    private Map<String, AvailabilityInfo> classAvailability;
}
