package com.alfre.DHHotel.domain.repository;

import com.alfre.DHHotel.domain.model.Reservation;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing reservation data.
 * Defines CRUD operations for reservations in the system.
 *
 * <p>This interface should be implemented by a class that interacts with the database.</p>
 *
 * @author Alfredo
 */
public interface ReservationRepository {

    /**
     * Retrieves a list of all reservations in the system.
     *
     * @return a list of {@link Reservation} objects.
     */
    List<Reservation> getAllReservations();

    /**
     * Retrieves a reservation based on its unique ID.
     *
     * @param id the unique identifier of the reservation.
     * @return an {@code Optional} containing the reservation if found, otherwise empty.
     */
    Optional<Reservation> getReservationById(long id);

    /**
     * Retrieves a list of reservations associated with a specific client.
     *
     * @param clientId the unique identifier of the client.
     * @return a list of {@link Reservation} objects associated with the client.
     */
    List<Reservation> getReservationsByClientId(Long clientId);

    /**
     * Creates a new reservation in the system.
     *
     * @param newReservation the {@link Reservation} object to be added.
     * @return the generated unique identifier of the newly created reservation.
     */
    long createReservation(Reservation newReservation);

    /**
     * Updates the details of an existing reservation.
     *
     * @param updatedReservation the reservation object containing updated information.
     * @return the number of rows affected in the database.
     */
    int updateReservation(Reservation updatedReservation);

    /**
     * Checks if a room is available for booking within a specific date range.
     *
     * @param roomId the unique identifier of the room.
     * @param startDate the start date of the reservation.
     * @param endDate the end date of the reservation.
     * @return {@code true} if the room is available, {@code false} otherwise.
     */
    boolean isRoomAvailable(Long roomId, LocalDate startDate, LocalDate endDate);

    /**
     * Deletes all reservation records from the system.
     * <p><b>Warning:</b> This action is irreversible.</p>
     */
    void deleteAll();
}
