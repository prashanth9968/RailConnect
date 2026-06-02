package com.railconnect.service.impl;

import com.railconnect.dto.response.TrainLocationResponse;
import com.railconnect.entity.Train;
import com.railconnect.exception.RailConnectException;
import com.railconnect.repository.TrainRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrainTrackingServiceImpl {

    private final TrainRepository trainRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final WebClient.Builder webClientBuilder;

    @Value("${app.google.maps.api-key:YOUR_GOOGLE_MAPS_API_KEY}")
    private String googleMapsApiKey;

    /**
     * Get live location of a train by number.
     * Fetches current lat/lng from DB (updated by scheduled job or operator input).
     */
    public TrainLocationResponse getTrainLocation(String trainNumber) {
        Train train = trainRepository.findByTrainNumber(trainNumber)
            .orElseThrow(() -> RailConnectException.notFound("Train not found: " + trainNumber));

        String mapsUrl = buildGoogleMapsUrl(train.getCurrentLatitude(), train.getCurrentLongitude(), trainNumber);

        return TrainLocationResponse.builder()
            .trainNumber(train.getTrainNumber())
            .trainName(train.getTrainName())
            .latitude(train.getCurrentLatitude())
            .longitude(train.getCurrentLongitude())
            .currentStation(train.getCurrentStationCode())
            .status(train.getStatus())
            .delayMinutes(train.getDelayMinutes())
            .lastUpdated(LocalDateTime.now())
            .googleMapsUrl(mapsUrl)
            .build();
    }

    /**
     * Scheduled job to broadcast train locations to WebSocket subscribers.
     * In production: pull from railway API/GPS devices.
     */
    @Scheduled(fixedDelay = 30000) // every 30 seconds
    public void broadcastTrainLocations() {
        List<Train> activeTrains = trainRepository.findAll().stream()
            .filter(t -> t.getCurrentLatitude() != null)
            .toList();

        for (Train train : activeTrains) {
            TrainLocationResponse location = TrainLocationResponse.builder()
                .trainNumber(train.getTrainNumber())
                .trainName(train.getTrainName())
                .latitude(train.getCurrentLatitude())
                .longitude(train.getCurrentLongitude())
                .currentStation(train.getCurrentStationCode())
                .status(train.getStatus())
                .delayMinutes(train.getDelayMinutes())
                .lastUpdated(LocalDateTime.now())
                .googleMapsUrl(buildGoogleMapsUrl(train.getCurrentLatitude(), train.getCurrentLongitude(), train.getTrainNumber()))
                .build();

            // Broadcast to train-specific topic
            messagingTemplate.convertAndSend("/topic/train/" + train.getTrainNumber(), location);
        }
    }

    /**
     * Update train location (called by train operators or GPS integration).
     */
    public void updateTrainLocation(String trainNumber, double lat, double lng,
                                     String stationCode, int delayMinutes) {
        Train train = trainRepository.findByTrainNumber(trainNumber)
            .orElseThrow(() -> RailConnectException.notFound("Train not found"));

        train.setCurrentLatitude(lat);
        train.setCurrentLongitude(lng);
        train.setCurrentStationCode(stationCode);
        train.setDelayMinutes(delayMinutes);
        trainRepository.save(train);

        log.info("Updated location for train {} - Lat:{}, Lng:{}, Station:{}", trainNumber, lat, lng, stationCode);
    }

    /**
     * Get running status of train between two stations.
     */
    public String getRunningStatus(String trainNumber, String fromStation, String toStation) {
        Train train = trainRepository.findByTrainNumber(trainNumber)
            .orElseThrow(() -> RailConnectException.notFound("Train not found"));

        return String.format("Train %s (%s) is currently at %s. Status: %s. Delay: %d minutes.",
            train.getTrainNumber(), train.getTrainName(),
            train.getCurrentStationCode() != null ? train.getCurrentStationCode() : "Unknown",
            train.getStatus().name(),
            train.getDelayMinutes());
    }

    private String buildGoogleMapsUrl(Double lat, Double lng, String trainNumber) {
        if (lat == null || lng == null) return null;
        return String.format("https://maps.googleapis.com/maps/api/staticmap?center=%f,%f&zoom=10&size=400x400&markers=color:red%%7Clabel:T%%7C%f,%f&key=%s",
            lat, lng, lat, lng, googleMapsApiKey);
    }
}
