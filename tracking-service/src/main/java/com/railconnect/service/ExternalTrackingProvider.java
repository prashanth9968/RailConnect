package com.railconnect.service;

import com.railconnect.dto.request.GpsTelemetryRequest;
import com.railconnect.entity.Train;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExternalTrackingProvider implements TrackingProvider {

    private final SimulatedTrackingProvider fallbackProvider;

    @Override
    public Optional<GpsTelemetryRequest> getTelemetry(Train train, LocalDate journeyDate) {
        log.info("Fetching live telemetry from external tracking provider for train: {}", train.getTrainNumber());
        return fallbackProvider.getTelemetry(train, journeyDate);
    }
}
