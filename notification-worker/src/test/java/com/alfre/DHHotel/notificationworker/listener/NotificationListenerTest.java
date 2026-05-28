package com.alfre.DHHotel.notificationworker.listener;

import com.alfre.DHHotel.notificationworker.event.BookingCreatedEvent;
import com.alfre.DHHotel.notificationworker.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * This class contains the attributes and methods for realize the tests
 * of the notification listener.
 *
 * @author Alfredo Sobrados González
 */
@SpringBootTest(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "spring.rabbitmq.listener.direct.auto-startup=false",
        "spring.rabbitmq.username=guest",
        "spring.rabbitmq.password=guest",
        "spring.mail.host=localhost",
        "spring.mail.port=25",
        "app.mail.from=test@dhhotel.com"
})
@ExtendWith(MockitoExtension.class)
public class NotificationListenerTest {

    @Autowired
    private NotificationListener notificationListener;

    @MockitoBean
    private EmailService emailService;

    /**
     * Tests that a valid booking message sends one confirmation email.
     */
    @Test
    void testOnBookingCreated_ValidMessage_SendsBookingConfirmation() {
        // Arrange
        BookingCreatedEvent event = bookingCreatedEvent();
        Message message = new Message(new byte[0], new MessageProperties());

        // Act
        notificationListener.onBookingCreated(event, message);

        // Assert
        verify(emailService, times(1)).sendBookingConfirmation(event);
    }

    /**
     * Tests that email delivery failures reject the message without requeueing.
     */
    @Test
    void testOnBookingCreated_EmailServiceThrows_RejectsWithoutRequeue() {
        // Arrange
        BookingCreatedEvent event = bookingCreatedEvent();
        Message message = new Message(new byte[0], new MessageProperties());
        doThrow(new RuntimeException("SMTP failure")).when(emailService).sendBookingConfirmation(event);

        // Act & Assert
        assertThrows(AmqpRejectAndDontRequeueException.class,
                () -> notificationListener.onBookingCreated(event, message));
    }

    private BookingCreatedEvent bookingCreatedEvent() {
        return new BookingCreatedEvent(
                1L,
                "booking-client@test.com",
                "Booking Client",
                "301",
                LocalDate.now().plusDays(3),
                LocalDate.now().plusDays(5)
        );
    }
}
