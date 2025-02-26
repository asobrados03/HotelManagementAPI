package com.alfre.DHHotel.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * Entity class representing a room.
 * This class maps to the rooms table in the database and contains essential attributes.
 *
 * <p>It uses Lombok annotations to reduce boilerplate code:</p>
 * <ul>
 *   <li>{@code @Data} - Generates getters, setters, {@code toString()}, {@code equals()}, and {@code hashCode()} methods.</li>
 *   <li>{@code @AllArgsConstructor} - Generates a constructor with all fields.</li>
 *   <li>{@code @NoArgsConstructor} - Generates a no-argument constructor.</li>
 * </ul>
 *
 * @author Alfredo Sobrados González
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Room {
    /**
     * The unique identifier for the room.
     */
    public long id;

    /**
     * The number assigned to the room.
     */
    public int room_number;

    /**
     * The type of the room (e.g., SINGLE, DOUBLE, SUITE).
     */
    public RoomType type;

    /**
     * The price per night for booking this room.
     */
    public BigDecimal price_per_night;

    /**
     * The current status of the room (e.g., AVAILABLE, OCCUPIED, MAINTENANCE).
     */
    public RoomStatus status;
}