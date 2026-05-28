package com.alfre.DHHotel.domain.event;

import java.time.LocalDate;

public record ReservationCreatedEvent(
        long reservationId,
        long clientId,
        long roomId,
        LocalDate startDate,
        LocalDate endDate,
        String status
) { }
