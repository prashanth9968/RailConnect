package com.railconnect.service;

import com.railconnect.dto.response.StationStatusInfo;
import com.railconnect.entity.TrainRoute;
import com.railconnect.entity.TrainSchedule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EtaPredictionEngine {

    private final DelayPredictionEngine delayPredictionEngine;

    /**
     * Calculates the estimated times of arrival (ETAs) and departures for each stop in the route.
     */
    public List<StationStatusInfo> predictEtas(
            List<TrainRoute> routes,
            TrainSchedule schedule,
            double distanceToNextStationKm) {

        Map<String, Integer> predictedDelays = delayPredictionEngine.predictDelays(routes, schedule, distanceToNextStationKm);
        List<StationStatusInfo> routeStatusList = new ArrayList<>();
        
        int currentStopNumber = getCurrentStopNumber(routes, schedule);

        for (TrainRoute route : routes) {
            String stationCode = route.getStation().getStationCode();
            String status = "UPCOMING";

            if (stationCode.equals(schedule.getCurrentStationCode())) {
                status = "AT_STATION";
            } else if (route.getStopNumber() < currentStopNumber) {
                status = "DEPARTED";
            }

            // For departed stations, delay is historical/current schedule delay.
            // For upcoming stations, delay is predicted by the DelayPredictionEngine.
            int delay = route.getStopNumber() < currentStopNumber 
                    ? schedule.getDelayMinutes() 
                    : predictedDelays.getOrDefault(stationCode, schedule.getDelayMinutes());

            LocalTime scheduledArrival = route.getArrivalTime();
            LocalTime scheduledDeparture = route.getDepartureTime();
            
            LocalTime actualArrival = scheduledArrival != null ? scheduledArrival.plusMinutes(delay) : null;
            LocalTime actualDeparture = scheduledDeparture != null ? scheduledDeparture.plusMinutes(delay) : null;

            routeStatusList.add(StationStatusInfo.builder()
                    .stationCode(stationCode)
                    .stationName(route.getStation().getStationName())
                    .scheduledArrival(scheduledArrival)
                    .scheduledDeparture(scheduledDeparture)
                    .actualArrival(actualArrival)
                    .actualDeparture(actualDeparture)
                    .delayMinutes(delay)
                    .status(status)
                    .latitude(route.getStation().getLatitude())
                    .longitude(route.getStation().getLongitude())
                    .build());

        }

        return routeStatusList;
    }

    private int getCurrentStopNumber(List<TrainRoute> routes, TrainSchedule schedule) {
        if (schedule.getCurrentStationCode() == null) return 0;
        return routes.stream()
                .filter(r -> r.getStation().getStationCode().equals(schedule.getCurrentStationCode()))
                .mapToInt(TrainRoute::getStopNumber)
                .findFirst()
                .orElse(0);
    }
}
