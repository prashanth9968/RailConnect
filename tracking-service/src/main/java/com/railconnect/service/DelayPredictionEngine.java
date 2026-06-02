package com.railconnect.service;

import com.railconnect.entity.TrainRoute;
import com.railconnect.entity.TrainSchedule;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DelayPredictionEngine {

    /**
     * Calculates predicted delays (in minutes) for all upcoming stations.
     */
    public Map<String, Integer> predictDelays(
            List<TrainRoute> routes,
            TrainSchedule schedule,
            double distanceToNextStationKm) {
        
        Map<String, Integer> predictions = new HashMap<>();
        if (routes == null || routes.isEmpty() || schedule == null) {
            return predictions;
        }

        int currentDelay = schedule.getDelayMinutes();
        int currentSpeed = schedule.getSpeedKmph() > 0 ? schedule.getSpeedKmph() : 60;

        String nextStationCode = schedule.getNextStationCode();
        if (nextStationCode == null) {
            return predictions;
        }

        int nextStationIndex = -1;
        for (int i = 0; i < routes.size(); i++) {
            if (routes.get(i).getStation().getStationCode().equals(nextStationCode)) {
                nextStationIndex = i;
                break;
            }
        }

        if (nextStationIndex == -1) {
            return predictions;
        }

        // 1. Predict delay for the immediate next station
        TrainRoute nextRoute = routes.get(nextStationIndex);
        TrainRoute currentRoute = nextStationIndex > 0 ? routes.get(nextStationIndex - 1) : null;
        
        int scheduledTravelTimeMinutes = 30; // fallback
        if (currentRoute != null && nextRoute.getArrivalTime() != null && currentRoute.getDepartureTime() != null) {
            scheduledTravelTimeMinutes = (int) Duration.between(
                    currentRoute.getDepartureTime(),
                    nextRoute.getArrivalTime()
            ).toMinutes();
            if (scheduledTravelTimeMinutes <= 0) {
                scheduledTravelTimeMinutes = 30;
            }
        }

        int estimatedTravelTimeMinutes = (int) Math.round((distanceToNextStationKm / currentSpeed) * 60.0);
        int difference = estimatedTravelTimeMinutes - scheduledTravelTimeMinutes;
        int nextPredictedDelay = Math.max(0, currentDelay + difference);
        predictions.put(nextStationCode, nextPredictedDelay);

        // 2. Predict delays for subsequent stations
        int lastPredictedDelay = nextPredictedDelay;
        for (int i = nextStationIndex + 1; i < routes.size(); i++) {
            TrainRoute r = routes.get(i);
            // Propagate delay with minor adjustments based on station halt
            int haltAdj = r.getHaltMinutes() > 5 ? (r.getHaltMinutes() - 5) : 0;
            int predictedDelay = Math.max(0, lastPredictedDelay + haltAdj);
            predictions.put(r.getStation().getStationCode(), predictedDelay);
            lastPredictedDelay = predictedDelay;
        }

        return predictions;
    }
}
