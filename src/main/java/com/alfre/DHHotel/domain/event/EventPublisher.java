package com.alfre.DHHotel.domain.event;

import com.alfre.DHHotel.config.RabbitMQConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventPublisher {
    private final RabbitTemplate rabbitTemplate;

    public EventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishBookingCreated(BookingCreatedEvent event) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.HOTEL_EXCHANGE,
                RabbitMQConfig.BOOKING_CREATED_ROUTING_KEY,
                event
        );
    }
}
