package com.alfre.DHHotel.domain.model;

/**
 * Enum representing the possible statuses of a room.
 * This is used to track the availability and condition of a room in the system.
 *
 * <p>Possible values:</p>
 * <ul>
 *   <li>{@code AVAILABLE} - The room is available for booking.</li>
 *   <li>{@code OCCUPIED} - The room is currently occupied by a guest.</li>
 *   <li>{@code MAINTENANCE} - The room is under maintenance and cannot be booked.</li>
 * </ul>
 *
 * This enum helps maintain consistency in room status management.
 *
 * @author Alfredo Sobrados González
 */
public enum RoomStatus {
    /**
     * The room is available for booking.
     */
    AVAILABLE,

    /**
     * The room is currently occupied by a guest.
     */
    OCCUPIED,

    /**
     * The room is under maintenance and cannot be booked.
     */
    MAINTENANCE
}
