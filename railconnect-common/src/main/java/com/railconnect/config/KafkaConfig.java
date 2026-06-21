package com.railconnect.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaConfig {

    public static final String BOOKING_CREATED_TOPIC = "booking-created";
    public static final String PAYMENT_COMPLETED_TOPIC = "payment-completed";
    public static final String SEAT_LOCKED_TOPIC = "seat-locked";
    public static final String TICKET_CONFIRMED_TOPIC = "ticket-confirmed";

    @Bean
    public NewTopic bookingCreatedTopic() {
        return TopicBuilder.name(BOOKING_CREATED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentCompletedTopic() {
        return TopicBuilder.name(PAYMENT_COMPLETED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic seatLockedTopic() {
        return TopicBuilder.name(SEAT_LOCKED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic ticketConfirmedTopic() {
        return TopicBuilder.name(TICKET_CONFIRMED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
