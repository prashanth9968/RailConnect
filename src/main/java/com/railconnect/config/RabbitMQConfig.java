package com.railconnect.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchange
    public static final String RAILCONNECT_EXCHANGE = "railconnect.exchange";
    // Queues
    public static final String BOOKING_CONFIRMED_QUEUE = "booking.confirmed";
    public static final String BOOKING_CANCELLED_QUEUE = "booking.cancelled";
    public static final String PAYMENT_SUCCESS_QUEUE = "payment.success";
    public static final String PAYMENT_FAILED_QUEUE = "payment.failed";
    public static final String WL_PROMOTION_QUEUE = "waitinglist.promotion";
    public static final String NOTIFICATION_EMAIL_QUEUE = "notification.email";
    public static final String NOTIFICATION_SMS_QUEUE = "notification.sms";
    // Routing keys
    public static final String BOOKING_CONFIRMED_KEY = "booking.confirmed";
    public static final String BOOKING_CANCELLED_KEY = "booking.cancelled";
    public static final String PAYMENT_SUCCESS_KEY = "payment.success";
    public static final String WL_PROMOTION_KEY = "waitinglist.promotion";

    @Bean
    public TopicExchange railconnectExchange() {
        return new TopicExchange(RAILCONNECT_EXCHANGE, true, false);
    }

    @Bean public Queue bookingConfirmedQueue() { return QueueBuilder.durable(BOOKING_CONFIRMED_QUEUE).build(); }
    @Bean public Queue bookingCancelledQueue() { return QueueBuilder.durable(BOOKING_CANCELLED_QUEUE).build(); }
    @Bean public Queue paymentSuccessQueue() { return QueueBuilder.durable(PAYMENT_SUCCESS_QUEUE).build(); }
    @Bean public Queue paymentFailedQueue() { return QueueBuilder.durable(PAYMENT_FAILED_QUEUE).build(); }
    @Bean public Queue wlPromotionQueue() { return QueueBuilder.durable(WL_PROMOTION_QUEUE).build(); }
    @Bean public Queue notificationEmailQueue() { return QueueBuilder.durable(NOTIFICATION_EMAIL_QUEUE).build(); }
    @Bean public Queue notificationSmsQueue() { return QueueBuilder.durable(NOTIFICATION_SMS_QUEUE).build(); }

    @Bean public Binding bookingConfirmedBinding() { return BindingBuilder.bind(bookingConfirmedQueue()).to(railconnectExchange()).with(BOOKING_CONFIRMED_KEY); }
    @Bean public Binding paymentSuccessBinding() { return BindingBuilder.bind(paymentSuccessQueue()).to(railconnectExchange()).with(PAYMENT_SUCCESS_KEY); }
    @Bean public Binding wlPromotionBinding() { return BindingBuilder.bind(wlPromotionQueue()).to(railconnectExchange()).with(WL_PROMOTION_KEY); }

    @Bean public Jackson2JsonMessageConverter messageConverter() { return new Jackson2JsonMessageConverter(); }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory factory) {
        RabbitTemplate template = new RabbitTemplate(factory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
