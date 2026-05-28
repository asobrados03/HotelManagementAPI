package com.alfre.DHHotel.notificationworker.event;

import java.time.LocalDate;

public record BookingCreatedEvent(
        Long bookingId,
        String guestEmail,
        String guestName,
        String roomNumber,
        LocalDate checkIn,
        LocalDate checkOut
) { }
