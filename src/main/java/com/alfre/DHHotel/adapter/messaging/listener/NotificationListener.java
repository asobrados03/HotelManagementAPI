package com.alfre.DHHotel.adapter.messaging.listener;

import com.alfre.DHHotel.domain.event.ReservationCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationListener {
    private static final Logger logger = LoggerFactory.getLogger(NotificationListener.class);

    @RabbitListener(queues = "reservation.created.queue")
    public void onReservationCreated(ReservationCreatedEvent event) {
        logger.info("Notificación procesada para reserva {} del cliente {}", event.reservationId(), event.clientId());
    }
}
