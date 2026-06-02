package com.railconnect.service;

import com.railconnect.dto.response.*;
import com.railconnect.entity.*;
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

import java.time.LocalDate;
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

    @Value("${google.maps.api-key:demo-key}")
    private String googleMapsApiKey;

    @Transactional(readOnly = true)
    public TrainLiveStatusResponse getLiveStatus(String trainNumber, LocalDate date) {
        Train train = trainRepository.findByTrainNumber(trainNumber)
            .orElseThrow(() -> new RailConnectException("Train not found", HttpStatus.NOT_FOUND));

        TrainSchedule schedule = scheduleRepository.findByTrainIdAndJourneyDate(train.getId(), date)
            .orElseGet(() -> createDefaultSchedule(train, date));

        List<TrainRoute> routes = routeRepository.findByTrainIdOrderByStopNumber(train.getId());
        List<StationStatusInfo> routeStatus = routes.stream()
            .map(route -> buildStationStatus(route, schedule))
            .toList();

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
     * Simulates real-time GPS updates (in production, this would consume
     * a real-time feed from NTES or a GPS device API).
     * Broadcasts updates via WebSocket to subscribed clients.
     */
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    @Transactional
    public void broadcastLiveUpdates() {
        List<TrainSchedule> runningToday = scheduleRepository.findAllByDate(LocalDate.now());
        runningToday.forEach(schedule -> {
            // Simulate small GPS movement
            simulateGpsMovement(schedule);
            scheduleRepository.save(schedule);

            // Broadcast via WebSocket
            TrainLiveStatusResponse status = buildLiveStatusFromSchedule(schedule);
            messagingTemplate.convertAndSend(
                "/topic/train/" + schedule.getTrain().getTrainNumber(), status);
        });
    }

    /**
     * Returns a Google Maps directions URL for embedding in the frontend.
     */
    public String getGoogleMapsTrackingUrl(String trainNumber) {
        return "https://maps.googleapis.com/maps/api/js?key=" + googleMapsApiKey
            + "&callback=initMap&libraries=geometry";
    }

    private void simulateGpsMovement(TrainSchedule schedule) {
        // Small random movement to simulate train in motion
        if (schedule.getCurrentLatitude() == 0) {
            schedule.setCurrentLatitude(17.3850); // Hyderabad default
            schedule.setCurrentLongitude(78.4867);
        }
        schedule.setCurrentLatitude(schedule.getCurrentLatitude() + (Math.random() * 0.01 - 0.005));
        schedule.setCurrentLongitude(schedule.getCurrentLongitude() + (Math.random() * 0.01 - 0.005));
        schedule.setSpeedKmph((int)(60 + Math.random() * 50));
    }

    private StationStatusInfo buildStationStatus(TrainRoute route, TrainSchedule schedule) {
        String status = "UPCOMING";
        if (route.getStation().getStationCode().equals(schedule.getCurrentStationCode())) {
            status = "AT_STATION";
        } else if (route.getStopNumber() < getCurrentStopNumber(schedule)) {
            status = "DEPARTED";
        }
        return StationStatusInfo.builder()
            .stationCode(route.getStation().getStationCode())
            .stationName(route.getStation().getStationName())
            .scheduledArrival(route.getArrivalTime())
            .scheduledDeparture(route.getDepartureTime())
            .delayMinutes(schedule.getDelayMinutes())
            .status(status)
            .build();
    }

    private int getCurrentStopNumber(TrainSchedule schedule) {
        if (schedule.getCurrentStationCode() == null) return 0;
        return routeRepository.findByTrainIdOrderByStopNumber(schedule.getTrain().getId())
            .stream().filter(r -> r.getStation().getStationCode().equals(schedule.getCurrentStationCode()))
            .mapToInt(TrainRoute::getStopNumber).findFirst().orElse(0);
    }

    private TrainSchedule createDefaultSchedule(Train train, LocalDate date) {
        return TrainSchedule.builder()
            .train(train)
            .journeyDate(date)
            .build();
    }

    private TrainLiveStatusResponse buildLiveStatusFromSchedule(TrainSchedule s) {
        return TrainLiveStatusResponse.builder()
            .trainNumber(s.getTrain().getTrainNumber())
            .trainName(s.getTrain().getTrainName())
            .journeyDate(s.getJourneyDate())
            .status(s.getStatus())
            .delayMinutes(s.getDelayMinutes())
            .currentLatitude(s.getCurrentLatitude())
            .currentLongitude(s.getCurrentLongitude())
            .currentStation(s.getCurrentStationCode())
            .nextStation(s.getNextStationCode())
            .speedKmph(s.getSpeedKmph())
            .lastUpdated(s.getLastUpdated())
            .build();
    }
}
