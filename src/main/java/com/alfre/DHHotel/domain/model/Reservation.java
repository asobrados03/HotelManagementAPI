package com.alfre.DHHotel.domain.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity class representing a reservation.
 * This class maps to the reservations table in the database and contains essential attributes.
 *
 * <p>It uses Lombok annotations to reduce boilerplate code:</p>
 * <ul>
 *   <li>{@code @Setter} - Generates setter methods for all fields.</li>
 *   <li>{@code @AllArgsConstructor} - Generates a constructor with all fields.</li>
 *   <li>{@code @NoArgsConstructor} - Generates a no-argument constructor.</li>
 * </ul>
 *
 * @author Alfredo Sobrados González
 */
@AllArgsConstructor
@NoArgsConstructor
@Setter
public class Reservation {
    /**
     * The unique identifier for the reservation.
     */
    public long id;

    /**
     * The ID of the client associated with this reservation.
     */
    public Long client_id;

    /**
     * The ID of the room assigned to this reservation.
     */
    public Long room_id;

    /**
     * The total price of the reservation.
     */
    public BigDecimal total_price;

    /**
     * The start date of the reservation.
     */
    public LocalDate start_date;

    /**
     * The end date of the reservation.
     */
    public LocalDate end_date;

    /**
     * The current status of the reservation (e.g., CONFIRMED, PENDING, CANCELED).
     */
    public ReservationStatus status;
}