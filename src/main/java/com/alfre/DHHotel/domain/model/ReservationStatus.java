package com.alfre.DHHotel.domain.model;

/**
 * Enum representing the possible statuses of a reservation.
 * This is used to track the current state of a reservation in the system.
 *
 * <p>Possible values:</p>
 * <ul>
 *   <li>{@code PENDING} - The reservation has been created but not yet confirmed.</li>
 *   <li>{@code CONFIRMED} - The reservation has been confirmed and is active.</li>
 *   <li>{@code CANCELED} - The reservation has been canceled.</li>
 * </ul>
 *
 * This enum helps maintain consistency and readability in reservation status management.
 *
 * @author Alfredo Sobrados González
 */
public enum ReservationStatus {
    /**
     * The reservation has been created but not yet confirmed.
     */
    PENDING,

    /**
     * The reservation has been confirmed and is active.
     */
    CONFIRMED,

    /**
     * The reservation has been canceled.
     */
    CANCELED
}
