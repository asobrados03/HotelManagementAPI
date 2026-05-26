package com.alfre.DHHotel.adapter.messaging.publisher;

import com.alfre.DHHotel.domain.event.EventPublisher;
import com.alfre.DHHotel.domain.event.ReservationCreatedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class RabbitMQEventPublisher implements EventPublisher {
    public static final String RESERVATION_EXCHANGE = "reservation.exchange";
    public static final String RESERVATION_CREATED_ROUTING_KEY = "reservation.created";

    private final RabbitTemplate rabbitTemplate;

    public RabbitMQEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publishReservationCreated(ReservationCreatedEvent event) {
        rabbitTemplate.convertAndSend(RESERVATION_EXCHANGE, RESERVATION_CREATED_ROUTING_KEY, event);
    }
}
