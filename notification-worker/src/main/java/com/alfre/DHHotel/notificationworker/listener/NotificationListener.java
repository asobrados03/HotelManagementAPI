package com.alfre.DHHotel.notificationworker.listener;

import com.alfre.DHHotel.notificationworker.config.RabbitMQConfig;
import com.alfre.DHHotel.notificationworker.event.BookingCreatedEvent;
import com.alfre.DHHotel.notificationworker.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationListener {

    private final EmailService emailService;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATIONS_QUEUE)
    public void onBookingCreated(BookingCreatedEvent event, Message message) {
        long attempt = resolveAttempt(message);
        log.info("Procesando notificación de reserva {} para {}. Intento {}",
                event.bookingId(), event.guestEmail(), attempt);

        try {
            emailService.sendBookingConfirmation(event);
            log.info("Email de confirmación enviado para reserva {}", event.bookingId());
        } catch (Exception ex) {
            log.error("Falló el envío de email para reserva {} en intento {}",
                    event.bookingId(), attempt, ex);
            throw new AmqpRejectAndDontRequeueException(
                    "Falló el envío de email para reserva " + event.bookingId(),
                    ex
            );
        }
    }

    private long resolveAttempt(Message message) {
        Object xDeath = message.getMessageProperties().getHeaders().get("x-death");
        if (!(xDeath instanceof List<?> deaths) || deaths.isEmpty()) {
            return 1;
        }

        long previousDeaths = deaths.stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(death -> death.get("count"))
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .mapToLong(Number::longValue)
                .sum();

        return previousDeaths + 1;
    }
}
