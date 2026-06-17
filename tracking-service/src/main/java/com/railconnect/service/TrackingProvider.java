package com.railconnect.service;

import com.railconnect.dto.request.GpsTelemetryRequest;
import com.railconnect.entity.Train;
import java.time.LocalDate;
import java.util.Optional;

public interface TrackingProvider {
    Optional<GpsTelemetryRequest> getTelemetry(Train train, LocalDate journeyDate);
}
