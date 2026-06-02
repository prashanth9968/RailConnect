package com.railconnect.notification.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.railconnect.config.KafkaConfig;
import com.railconnect.entity.Booking;
import com.railconnect.repository.BookingRepository;
import com.railconnect.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationMessageListener {

    private final NotificationService notificationService;
    private final BookingRepository bookingRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = KafkaConfig.TICKET_CONFIRMED_TOPIC, groupId = "notification-group")
    public void handleTicketConfirmed(String payload) {
        log.info("Received ticket confirmed Kafka payload: {}", payload);
        try {
            Map<String, String> message = objectMapper.readValue(payload, new TypeReference<Map<String, String>>() {});
            String bookingIdStr = message.get("bookingId");
            String status = message.get("status");
            if (bookingIdStr == null) return;
            UUID bookingId = UUID.fromString(bookingIdStr);
            Booking booking = bookingRepository.findById(bookingId).orElse(null);
            if (booking == null) {
                log.warn("Booking not found for ID: {}", bookingId);
                return;
            }

            if ("CANCELLED".equals(status)) {
                String refundAmountStr = message.get("refundAmount");
                BigDecimal refundAmount = refundAmountStr != null ? new BigDecimal(refundAmountStr) : BigDecimal.ZERO;
                notificationService.sendCancellationConfirmation(booking, refundAmount);
            } else if ("PROMOTED".equals(status)) {
                notificationService.sendWaitlistConfirmation(booking);
            } else {
                // Default CONFIRMED
                notificationService.sendBookingConfirmation(booking);
            }
        } catch (Exception e) {
            log.error("Failed to handle ticket confirmed event: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = KafkaConfig.PAYMENT_COMPLETED_TOPIC, groupId = "notification-group")
    public void handlePaymentCompleted(String payload) {
        log.info("Received payment completed Kafka payload: {}", payload);
    }

    @KafkaListener(topics = KafkaConfig.BOOKING_CREATED_TOPIC, groupId = "notification-group")
    public void handleBookingCreated(String payload) {
        log.info("Received booking created Kafka payload: {}", payload);
    }

    @KafkaListener(topics = KafkaConfig.SEAT_LOCKED_TOPIC, groupId = "notification-group")
    public void handleSeatLocked(String payload) {
        log.info("Received seat locked Kafka payload: {}", payload);
    }
}
