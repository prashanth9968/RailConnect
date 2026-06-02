package com.railconnect.service;

import com.railconnect.dto.request.GpsTelemetryRequest;
import com.railconnect.dto.response.*;
import com.railconnect.entity.*;
import com.railconnect.enums.TrainStatus;
import com.railconnect.exception.RailConnectException;
import com.railconnect.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrainTrackingService {

    private final TrainScheduleRepository scheduleRepository;
    private final TrainRepository trainRepository;
    private final TrainRouteRepository routeRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RestTemplate restTemplate;
    private final EtaPredictionEngine etaPredictionEngine;

    @Value("${google.maps.api-key:demo-key}")
    private String googleMapsApiKey;

    @Transactional(readOnly = true)
    public TrainLiveStatusResponse getLiveStatus(String trainNumber, LocalDate date) {
        Train train = trainRepository.findByTrainNumber(trainNumber)
            .orElseThrow(() -> new RailConnectException("Train not found", HttpStatus.NOT_FOUND));

        TrainSchedule schedule = scheduleRepository.findByTrainIdAndJourneyDate(train.getId(), date)
            .orElseGet(() -> createDefaultSchedule(train, date));

        List<TrainRoute> routes = routeRepository.findByTrainIdOrderByStopNumber(train.getId());
        double distToNext = calculateDistanceToNextStation(schedule, routes);
        List<StationStatusInfo> routeStatus = etaPredictionEngine.predictEtas(routes, schedule, distToNext);

        return TrainLiveStatusResponse.builder()
            .trainNumber(train.getTrainNumber())
            .trainName(train.getTrainName())
            .journeyDate(date)
            .status(schedule.getStatus())
            .delayMinutes(schedule.getDelayMinutes())
            .currentLatitude(schedule.getCurrentLatitude())
            .currentLongitude(schedule.getCurrentLongitude())
            .currentStation(schedule.getCurrentStationCode())
            .nextStation(schedule.getNextStationCode())
            .speedKmph(schedule.getSpeedKmph())
            .lastUpdated(schedule.getLastUpdated())
            .routeStatus(routeStatus)
            .build();
    }

    /**
     * Ingests real-time GPS telemetry from the train, updates the schedule,
     * performs ETA and delay predictions, and broadcasts updates via WebSocket.
     */
    @Transactional
    public TrainLiveStatusResponse ingestTelemetry(GpsTelemetryRequest telemetryRequest) {
        Train train = trainRepository.findByTrainNumber(telemetryRequest.getTrainNumber())
            .orElseThrow(() -> new RailConnectException("Train not found", HttpStatus.NOT_FOUND));

        LocalDate today = LocalDate.now();
        TrainSchedule schedule = scheduleRepository.findByTrainIdAndJourneyDate(train.getId(), today)
            .orElseGet(() -> {
                TrainSchedule newSchedule = createDefaultSchedule(train, today);
                return scheduleRepository.save(newSchedule);
            });

        // 1. Update coordinates and speed
        schedule.setCurrentLatitude(telemetryRequest.getLatitude());
        schedule.setCurrentLongitude(telemetryRequest.getLongitude());
        if (telemetryRequest.getSpeedKmph() != null) {
            schedule.setSpeedKmph(telemetryRequest.getSpeedKmph());
        }

        // 2. Fetch routes and calculate closest station to update current/next stations
        List<TrainRoute> routes = routeRepository.findByTrainIdOrderByStopNumber(train.getId());
        updateStationsFromGps(schedule, routes);

        // 3. Dynamically update delay based on remaining distance to next station
        updateDelayFromGps(schedule, routes);

        // Save updated schedule
        scheduleRepository.save(schedule);

        // 4. Predict ETAs and delays for upcoming stops
        double distToNext = calculateDistanceToNextStation(schedule, routes);
        List<StationStatusInfo> routeStatus = etaPredictionEngine.predictEtas(routes, schedule, distToNext);

        TrainLiveStatusResponse statusResponse = TrainLiveStatusResponse.builder()
            .trainNumber(train.getTrainNumber())
            .trainName(train.getTrainName())
            .journeyDate(today)
            .status(schedule.getStatus())
            .delayMinutes(schedule.getDelayMinutes())
            .currentLatitude(schedule.getCurrentLatitude())
            .currentLongitude(schedule.getCurrentLongitude())
            .currentStation(schedule.getCurrentStationCode())
            .nextStation(schedule.getNextStationCode())
            .speedKmph(schedule.getSpeedKmph())
            .lastUpdated(schedule.getLastUpdated())
            .routeStatus(routeStatus)
            .build();

        // 5. Broadcast to subscribers
        messagingTemplate.convertAndSend("/topic/train/" + train.getTrainNumber(), statusResponse);

        return statusResponse;
    }

    /**
     * Simulates real-time GPS updates by generating simulated GpsTelemetryRequests.
     */
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    @Transactional
    public void broadcastLiveUpdates() {
        List<TrainSchedule> runningToday = scheduleRepository.findAllByDate(LocalDate.now());
        runningToday.forEach(schedule -> {
            // Simulate coordinates slightly offset from previous values
            double currentLat = schedule.getCurrentLatitude() == 0 ? 17.3850 : schedule.getCurrentLatitude();
            double currentLon = schedule.getCurrentLongitude() == 0 ? 78.4867 : schedule.getCurrentLongitude();
            
            double nextLat = currentLat + (Math.random() * 0.01 - 0.005);
            double nextLon = currentLon + (Math.random() * 0.01 - 0.005);
            int simulatedSpeed = (int) (60 + Math.random() * 50);

            GpsTelemetryRequest request = GpsTelemetryRequest.builder()
                .trainNumber(schedule.getTrain().getTrainNumber())
                .latitude(nextLat)
                .longitude(nextLon)
                .speedKmph(simulatedSpeed)
                .build();

            try {
                ingestTelemetry(request);
            } catch (Exception e) {
                log.error("Failed to ingest simulated telemetry for train {}", schedule.getTrain().getTrainNumber(), e);
            }
        });
    }

    /**
     * Returns a Google Maps directions URL for embedding in the frontend.
     */
    public String getGoogleMapsTrackingUrl(String trainNumber) {
        return "https://maps.googleapis.com/maps/api/js?key=" + googleMapsApiKey
            + "&callback=initMap&libraries=geometry";
    }

    private void updateStationsFromGps(TrainSchedule schedule, List<TrainRoute> routes) {
        if (routes.isEmpty()) return;

        TrainRoute closestRoute = null;
        double minDistance = Double.MAX_VALUE;
        int closestIdx = -1;
        
        for (int i = 0; i < routes.size(); i++) {
            TrainRoute r = routes.get(i);
            double dist = calculateDistance(
                    schedule.getCurrentLatitude(),
                    schedule.getCurrentLongitude(),
                    r.getStation().getLatitude(),
                    r.getStation().getLongitude()
            );
            if (dist < minDistance) {
                minDistance = dist;
                closestRoute = r;
                closestIdx = i;
            }
        }

        if (closestRoute != null) {
            if (minDistance < 1.0) {
                // Train is at the station
                schedule.setCurrentStationCode(closestRoute.getStation().getStationCode());
                if (closestIdx < routes.size() - 1) {
                    schedule.setNextStationCode(routes.get(closestIdx + 1).getStation().getStationCode());
                } else {
                    schedule.setNextStationCode(null);
                }
            } else {
                // Train is in transit. Calculate if approaching or departed closest station.
                LocalTime nowTime = LocalTime.now();
                LocalTime scheduledArrival = closestRoute.getArrivalTime();
                
                if (closestIdx == 0) {
                    schedule.setCurrentStationCode(closestRoute.getStation().getStationCode());
                    schedule.setNextStationCode(routes.get(1).getStation().getStationCode());
                } else if (closestIdx == routes.size() - 1) {
                    schedule.setCurrentStationCode(routes.get(closestIdx - 1).getStation().getStationCode());
                    schedule.setNextStationCode(closestRoute.getStation().getStationCode());
                } else {
                    if (scheduledArrival != null && nowTime.isBefore(scheduledArrival.plusMinutes(schedule.getDelayMinutes()))) {
                        // Approaching closest station
                        schedule.setCurrentStationCode(routes.get(closestIdx - 1).getStation().getStationCode());
                        schedule.setNextStationCode(closestRoute.getStation().getStationCode());
                    } else {
                        // Departed closest station
                        schedule.setCurrentStationCode(closestRoute.getStation().getStationCode());
                        schedule.setNextStationCode(routes.get(closestIdx + 1).getStation().getStationCode());
                    }
                }
            }
        }
    }

    private void updateDelayFromGps(TrainSchedule schedule, List<TrainRoute> routes) {
        if (schedule.getNextStationCode() == null) return;

        Optional<TrainRoute> nextRouteOpt = routes.stream()
                .filter(r -> r.getStation().getStationCode().equals(schedule.getNextStationCode()))
                .findFirst();

        if (nextRouteOpt.isPresent()) {
            TrainRoute nextRoute = nextRouteOpt.get();
            LocalTime scheduledArrival = nextRoute.getArrivalTime();
            if (scheduledArrival != null) {
                double dist = calculateDistance(
                        schedule.getCurrentLatitude(),
                        schedule.getCurrentLongitude(),
                        nextRoute.getStation().getLatitude(),
                        nextRoute.getStation().getLongitude()
                );
                int speed = schedule.getSpeedKmph() > 0 ? schedule.getSpeedKmph() : 60;
                int etaMin = (int) Math.round((dist / speed) * 60.0);
                LocalTime estimatedArrival = LocalTime.now().plusMinutes(etaMin);
                long diff = Duration.between(scheduledArrival, estimatedArrival).toMinutes();
                schedule.setDelayMinutes(Math.max(0, (int) diff));
            }
        }
    }

    private double calculateDistanceToNextStation(TrainSchedule schedule, List<TrainRoute> routes) {
        if (schedule.getNextStationCode() == null) {
            return 0.0;
        }
        Optional<TrainRoute> nextRoute = routes.stream()
                .filter(r -> r.getStation().getStationCode().equals(schedule.getNextStationCode()))
                .findFirst();
        if (nextRoute.isPresent()) {
            Station nextStation = nextRoute.get().getStation();
            return calculateDistance(
                    schedule.getCurrentLatitude(),
                    schedule.getCurrentLongitude(),
                    nextStation.getLatitude(),
                    nextStation.getLongitude()
            );
        }
        return 0.0;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private TrainSchedule createDefaultSchedule(Train train, LocalDate date) {
        return TrainSchedule.builder()
            .train(train)
            .journeyDate(date)
            .status(TrainStatus.ON_TIME)
            .build();
    }
}

