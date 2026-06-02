package com.railconnect.websocket;

import com.railconnect.dto.response.TrainLiveStatusResponse;
import com.railconnect.service.TrainTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
public class TrainTrackingWebSocketController {

    private final TrainTrackingService trackingService;

    /**
     * Client subscribes to: /app/track/{trainNumber}
     * Receives live updates on: /topic/train/{trainNumber}
     */
    @SubscribeMapping("/track/{trainNumber}")
    public TrainLiveStatusResponse subscribeToTrain(@DestinationVariable String trainNumber) {
        return trackingService.getLiveStatus(trainNumber, LocalDate.now());
    }

    @MessageMapping("/track/{trainNumber}/request")
    @SendTo("/topic/train/{trainNumber}")
    public TrainLiveStatusResponse requestLiveUpdate(@DestinationVariable String trainNumber) {
        return trackingService.getLiveStatus(trainNumber, LocalDate.now());
    }
}
