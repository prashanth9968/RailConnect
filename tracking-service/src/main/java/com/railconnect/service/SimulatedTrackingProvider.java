package com.railconnect.service;

import com.railconnect.dto.request.GpsTelemetryRequest;
import com.railconnect.entity.Train;
import com.railconnect.entity.TrainRoute;
import com.railconnect.repository.TrainRouteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class SimulatedTrackingProvider implements TrackingProvider {

    private final TrainRouteRepository routeRepository;

    @Override
    public Optional<GpsTelemetryRequest> getTelemetry(Train train, LocalDate journeyDate) {
        List<TrainRoute> routes = routeRepository.findByTrainIdOrderByStopNumber(train.getId());
        if (routes.isEmpty()) {
            return Optional.empty();
        }

        LocalDateTime now = LocalDateTime.now();

        // 1. Calculate absolute arrival and departure times for each stop
        LocalDateTime[] arrivalTimes = new LocalDateTime[routes.size()];
        LocalDateTime[] departureTimes = new LocalDateTime[routes.size()];

        for (int i = 0; i < routes.size(); i++) {
            TrainRoute r = routes.get(i);
            LocalDate stopDate = journeyDate.plusDays(r.getDayNumber() - 1);
            arrivalTimes[i] = r.getArrivalTime() != null ? stopDate.atTime(r.getArrivalTime()) : null;
            departureTimes[i] = r.getDepartureTime() != null ? stopDate.atTime(r.getDepartureTime()) : null;
        }

        // Before first departure: Stationary at source
        LocalDateTime firstDep = departureTimes[0];
        if (firstDep != null && now.isBefore(firstDep)) {
            TrainRoute source = routes.get(0);
            return Optional.of(GpsTelemetryRequest.builder()
                    .trainNumber(train.getTrainNumber())
                    .latitude(source.getStation().getLatitude())
                    .longitude(source.getStation().getLongitude())
                    .speedKmph(0)
                    .build());
        }

        // After last arrival: Stationary at destination
        LocalDateTime lastArr = arrivalTimes[routes.size() - 1];
        if (lastArr != null && now.isAfter(lastArr)) {
            TrainRoute dest = routes.get(routes.size() - 1);
            return Optional.of(GpsTelemetryRequest.builder()
                    .trainNumber(train.getTrainNumber())
                    .latitude(dest.getStation().getLatitude())
                    .longitude(dest.getStation().getLongitude())
                    .speedKmph(0)
                    .build());
        }

        // Check halts vs transits
        for (int i = 0; i < routes.size(); i++) {
            LocalDateTime arr = arrivalTimes[i];
            LocalDateTime dep = departureTimes[i];

            // Halting at station i
            if (arr != null && dep != null && !now.isBefore(arr) && !now.isAfter(dep)) {
                TrainRoute stop = routes.get(i);
                return Optional.of(GpsTelemetryRequest.builder()
                        .trainNumber(train.getTrainNumber())
                        .latitude(stop.getStation().getLatitude())
                        .longitude(stop.getStation().getLongitude())
                        .speedKmph(0)
                        .build());
            }

            // In transit between i and i+1
            if (i < routes.size() - 1) {
                LocalDateTime currentDep = departureTimes[i];
                LocalDateTime nextArr = arrivalTimes[i + 1];

                if (currentDep != null && nextArr != null && !now.isBefore(currentDep) && !now.isAfter(nextArr)) {
                    TrainRoute currentStop = routes.get(i);
                    TrainRoute nextStop = routes.get(i + 1);

                    double latStart = currentStop.getStation().getLatitude();
                    double lonStart = currentStop.getStation().getLongitude();
                    double latEnd = nextStop.getStation().getLatitude();
                    double lonEnd = nextStop.getStation().getLongitude();

                    long totalSec = Duration.between(currentDep, nextArr).toSeconds();
                    long elapsedSec = Duration.between(currentDep, now).toSeconds();

                    double fraction = totalSec > 0 ? (double) elapsedSec / totalSec : 0.0;
                    double lat = latStart + fraction * (latEnd - latStart);
                    double lon = lonStart + fraction * (lonEnd - lonStart);

                    int speed = 80 + (int)(Math.random() * 20); // 80-100 km/h

                    return Optional.of(GpsTelemetryRequest.builder()
                            .trainNumber(train.getTrainNumber())
                            .latitude(lat)
                            .longitude(lon)
                            .speedKmph(speed)
                            .build());
                }
            }
        }

        // Fallback to source
        TrainRoute fallback = routes.get(0);
        return Optional.of(GpsTelemetryRequest.builder()
                .trainNumber(train.getTrainNumber())
                .latitude(fallback.getStation().getLatitude())
                .longitude(fallback.getStation().getLongitude())
                .speedKmph(0)
                .build());
    }
}
