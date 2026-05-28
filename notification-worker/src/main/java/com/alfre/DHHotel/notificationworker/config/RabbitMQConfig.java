package com.alfre.DHHotel.notificationworker.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String HOTEL_EXCHANGE = "hotel.exchange";
    public static final String NOTIFICATIONS_QUEUE = "hotel.notifications";
    public static final String RETRY_QUEUE_1 = "retry.1";
    public static final String RETRY_QUEUE_2 = "retry.2";
    public static final String RETRY_QUEUE_3 = "retry.3";
    public static final String NOTIFICATIONS_DLQ = "hotel.notifications.dlq";
    public static final String BOOKING_CREATED_ROUTING_KEY = "booking.created";

    @Bean
    public TopicExchange hotelExchange() {
        return new TopicExchange(HOTEL_EXCHANGE);
    }

    @Bean
    public Queue hotelNotificationsQueue() {
        return QueueBuilder.durable(NOTIFICATIONS_QUEUE)
                .deadLetterExchange(HOTEL_EXCHANGE)
                .deadLetterRoutingKey(RETRY_QUEUE_1)
                .build();
    }

    @Bean
    public Queue retryQueue1() {
        return retryQueue(RETRY_QUEUE_1, 5_000, RETRY_QUEUE_2);
    }

    @Bean
    public Queue retryQueue2() {
        return retryQueue(RETRY_QUEUE_2, 25_000, RETRY_QUEUE_3);
    }

    @Bean
    public Queue retryQueue3() {
        return retryQueue(RETRY_QUEUE_3, 125_000, NOTIFICATIONS_DLQ);
    }

    @Bean
    public Queue hotelNotificationsDlq() {
        return QueueBuilder.durable(NOTIFICATIONS_DLQ).build();
    }

    @Bean
    public Binding bookingCreatedBinding(Queue hotelNotificationsQueue, TopicExchange hotelExchange) {
        return BindingBuilder.bind(hotelNotificationsQueue)
                .to(hotelExchange)
                .with(BOOKING_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding retryQueue1Binding(Queue retryQueue1, TopicExchange hotelExchange) {
        return BindingBuilder.bind(retryQueue1)
                .to(hotelExchange)
                .with(RETRY_QUEUE_1);
    }

    @Bean
    public Binding retryQueue2Binding(Queue retryQueue2, TopicExchange hotelExchange) {
        return BindingBuilder.bind(retryQueue2)
                .to(hotelExchange)
                .with(RETRY_QUEUE_2);
    }

    @Bean
    public Binding retryQueue3Binding(Queue retryQueue3, TopicExchange hotelExchange) {
        return BindingBuilder.bind(retryQueue3)
                .to(hotelExchange)
                .with(RETRY_QUEUE_3);
    }

    @Bean
    public Binding hotelNotificationsDlqBinding(Queue hotelNotificationsDlq, TopicExchange hotelExchange) {
        return BindingBuilder.bind(hotelNotificationsDlq)
                .to(hotelExchange)
                .with(NOTIFICATIONS_DLQ);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    private Queue retryQueue(String queueName, int ttl, String deadLetterRoutingKey) {
        return QueueBuilder.durable(queueName)
                .ttl(ttl)
                .deadLetterExchange(HOTEL_EXCHANGE)
                .deadLetterRoutingKey(deadLetterRoutingKey)
                .build();
    }
}
