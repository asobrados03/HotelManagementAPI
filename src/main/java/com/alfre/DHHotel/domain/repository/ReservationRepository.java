package com.alfre.DHHotel.domain.repository;

import com.alfre.DHHotel.domain.model.Reservation;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository {
    List<Reservation> getAllReservations();
    Optional<Reservation> getReservationById(long id);
    List<Reservation> getReservationsByClientId(Long clientId);
    long createReservation(Reservation newReservation);
    int updateReservation(Reservation updatedReservation);
    boolean isRoomAvailable(Long roomId, Date startDate, Date endDate);
    void deleteAll();
}
