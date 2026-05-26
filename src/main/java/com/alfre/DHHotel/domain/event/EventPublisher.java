package com.alfre.DHHotel.domain.event;

public interface EventPublisher {
    void publishReservationCreated(ReservationCreatedEvent event);
}
