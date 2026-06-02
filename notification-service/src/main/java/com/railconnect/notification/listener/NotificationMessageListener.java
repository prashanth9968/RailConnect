package com.railconnect.notification.listener;

import com.railconnect.config.RabbitMQConfig;
import com.railconnect.entity.Booking;
import com.railconnect.repository.BookingRepository;
import com.railconnect.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
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

    @RabbitListener(queues = RabbitMQConfig.BOOKING_CONFIRMED_QUEUE)
    public void handleBookingConfirmed(Map<String, String> message) {
        log.info("Received booking confirmation message: {}", message);
        try {
            String bookingIdStr = message.get("bookingId");
            if (bookingIdStr == null) return;
            UUID bookingId = UUID.fromString(bookingIdStr);
            Booking booking = bookingRepository.findById(bookingId).orElse(null);
            if (booking != null) {
                notificationService.sendBookingConfirmation(booking);
            } else {
                log.warn("Booking not found for ID: {}", bookingId);
            }
        } catch (Exception e) {
            log.error("Failed to handle booking confirmation: {}", e.getMessage());
        }
    }

    @RabbitListener(queues = RabbitMQConfig.BOOKING_CANCELLED_QUEUE)
    public void handleBookingCancelled(Map<String, String> message) {
        log.info("Received booking cancellation message: {}", message);
        try {
            String bookingIdStr = message.get("bookingId");
            String refundAmountStr = message.get("refundAmount");
            if (bookingIdStr == null) return;
            UUID bookingId = UUID.fromString(bookingIdStr);
            Booking booking = bookingRepository.findById(bookingId).orElse(null);
            if (booking != null) {
                BigDecimal refundAmount = refundAmountStr != null ? new BigDecimal(refundAmountStr) : BigDecimal.ZERO;
                notificationService.sendCancellationConfirmation(booking, refundAmount);
            } else {
                log.warn("Booking not found for ID: {}", bookingId);
            }
        } catch (Exception e) {
            log.error("Failed to handle booking cancellation: {}", e.getMessage());
        }
    }

    @RabbitListener(queues = RabbitMQConfig.WL_PROMOTION_QUEUE)
    public void handleWaitlistPromotion(Map<String, String> message) {
        log.info("Received waitlist promotion message: {}", message);
        try {
            String bookingIdStr = message.get("bookingId");
            if (bookingIdStr == null) return;
            UUID bookingId = UUID.fromString(bookingIdStr);
            Booking booking = bookingRepository.findById(bookingId).orElse(null);
            if (booking != null) {
                notificationService.sendWaitlistConfirmation(booking);
            } else {
                log.warn("Booking not found for ID: {}", bookingId);
            }
        } catch (Exception e) {
            log.error("Failed to handle waitlist promotion: {}", e.getMessage());
        }
    }
}
