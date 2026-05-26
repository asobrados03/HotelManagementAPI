package com.alfre.DHHotel.config;

import com.alfre.DHHotel.adapter.messaging.publisher.RabbitMQEventPublisher;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    @Bean
    public Queue reservationCreatedQueue() {
        return new Queue("reservation.created.queue", true);
    }

    @Bean
    public TopicExchange reservationExchange() {
        return new TopicExchange(RabbitMQEventPublisher.RESERVATION_EXCHANGE);
    }

    @Bean
    public Binding reservationCreatedBinding(Queue reservationCreatedQueue, TopicExchange reservationExchange) {
        return BindingBuilder.bind(reservationCreatedQueue)
                .to(reservationExchange)
                .with(RabbitMQEventPublisher.RESERVATION_CREATED_ROUTING_KEY);
    }
}
