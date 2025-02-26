package com.alfre.DHHotel.domain.model;

/**
 * Enum representing the different types of rooms available for booking.
 *
 * <p>Possible values:</p>
 * <ul>
 *   <li>{@code SINGLE} - A single room with one bed.</li>
 *   <li>{@code DOUBLE} - A double room with two beds or a larger bed.</li>
 *   <li>{@code SUITE} - A luxury suite with additional amenities.</li>
 * </ul>
 *
 * This enum helps maintain consistency in room classification.
 *
 * @author Alfredo Sobrados González
 */
public enum RoomType {
    /**
     * A single room with one bed.
     */
    SINGLE,

    /**
     * A double room with two beds or a larger bed.
     */
    DOUBLE,

    /**
     * A luxury suite with additional amenities.
     */
    SUITE
}
