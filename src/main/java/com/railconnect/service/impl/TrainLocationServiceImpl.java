package com.railconnect.service.impl;
import com.railconnect.dto.response.TrainLocationResponse;
import com.railconnect.entity.*;
import com.railconnect.repository.*;
import com.railconnect.websocket.TrainLocationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
@Service @Slf4j @RequiredArgsConstructor
public class TrainLocationServiceImpl {
    private final TrainScheduleRepository scheduleRepository;
    private final TrainLocationHistoryRepository locationHistoryRepository;
    private final SimpMessagingTemplate messagingTemplate;
    @Value("${google.maps.api-key}") private String mapsApiKey;

    @Transactional(readOnly = true)
    public TrainLocationResponse getTrainLocation(UUID scheduleId) {
        TrainSchedule schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new com.railconnect.exception.ResourceNotFoundException("Schedule", "id", scheduleId));
        TrainLocationHistory latest = locationHistoryRepository
            .findFirstByScheduleIdOrderByRecordedAtDesc(scheduleId).orElse(null);

        Train train = schedule.getTrain();
        return TrainLocationResponse.builder()
            .trainNumber(train.getTrainNumber())
            .trainName(train.getTrainName())
            .latitude(latest != null ? latest.getLatitude() : schedule.getCurrentLatitude())
            .longitude(latest != null ? latest.getLongitude() : schedule.getCurrentLongitude())
            .currentStation(schedule.getCurrentStation() != null ? schedule.getCurrentStation().getStationName() : "En Route")
            .status(schedule.getStatus().name())
            .delayMinutes(schedule.getDelayMinutes())
            .speedKmh(latest != null ? latest.getSpeedKmh() : BigDecimal.ZERO)
            .lastUpdated(latest != null ? latest.getRecordedAt() : schedule.getLastLocationUpdate())
            .progressPercent(calculateProgress(schedule))
            .build();
    }

    private double calculateProgress(TrainSchedule schedule) {
        LocalDate today = LocalDate.now();
        if (schedule.getJourneyDate().isAfter(today)) return 0.0;
        if (schedule.getJourneyDate().isBefore(today)) return 100.0;
        // Simplified - real implementation would use route timestamps
        LocalTime now = LocalTime.now();
        return Math.min(100, Math.max(0, (now.toSecondOfDay() / 86400.0) * 100));
    }

    @Transactional
    public void updateTrainLocation(UUID scheduleId, BigDecimal lat, BigDecimal lng, BigDecimal speed) {
        scheduleRepository.findById(scheduleId).ifPresent(schedule -> {
            schedule.setCurrentLatitude(lat);
            schedule.setCurrentLongitude(lng);
            schedule.setLastLocationUpdate(Instant.now());
            scheduleRepository.save(schedule);

            TrainLocationHistory history = TrainLocationHistory.builder()
                .schedule(schedule).latitude(lat).longitude(lng).speedKmh(speed).build();
            locationHistoryRepository.save(history);

            // Broadcast via WebSocket
            TrainLocationResponse response = getTrainLocation(scheduleId);
            messagingTemplate.convertAndSend("/topic/train/" + scheduleId, response);
        });
    }

    @Scheduled(fixedRate = 30000) // Every 30 seconds - simulate location updates for demo
    @Transactional
    public void simulateLocationUpdates() {
        LocalDate today = LocalDate.now();
        List<TrainSchedule> activeSchedules = scheduleRepository.findByJourneyDate(today);
        for (TrainSchedule schedule : activeSchedules) {
            if (schedule.getCurrentLatitude() != null) {
                // Simulate small movement
                BigDecimal newLat = schedule.getCurrentLatitude().add(new BigDecimal("0.001"));
                BigDecimal newLng = schedule.getCurrentLongitude().add(new BigDecimal("0.001"));
                updateTrainLocation(schedule.getId(), newLat, newLng, new BigDecimal("80"));
            }
        }
    }
}
