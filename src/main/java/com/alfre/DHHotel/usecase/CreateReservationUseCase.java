package com.alfre.DHHotel.usecase;

import com.alfre.DHHotel.domain.model.Reservation;
import com.alfre.DHHotel.domain.model.User;
import org.springframework.stereotype.Service;

@Service
public class CreateReservationUseCase {
    private final ReservationUseCase reservationUseCase;

    public CreateReservationUseCase(ReservationUseCase reservationUseCase) {
        this.reservationUseCase = reservationUseCase;
    }

    public long execute(Reservation reservation, User user) {
        return reservationUseCase.createReservation(reservation, user);
    }
}
