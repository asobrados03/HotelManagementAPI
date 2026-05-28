package com.alfre.DHHotel.notificationworker.service;

import com.alfre.DHHotel.notificationworker.event.BookingCreatedEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.from}")
    private String fromAddress;

    public void sendBookingConfirmation(BookingCreatedEvent event) {
        Context context = new Context();
        context.setVariable("bookingId", event.bookingId());
        context.setVariable("guestName", event.guestName());
        context.setVariable("roomNumber", event.roomNumber());
        context.setVariable("checkIn", event.checkIn());
        context.setVariable("checkOut", event.checkOut());

        String html = templateEngine.process("booking-confirmation", context);
        String subject = "Confirmación de reserva #" + event.bookingId() + " - DH Hotel";

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );
            helper.setFrom(fromAddress);
            helper.setTo(event.guestEmail());
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException ex) {
            throw new IllegalStateException("No se pudo construir el email de confirmación", ex);
        }
    }
}
