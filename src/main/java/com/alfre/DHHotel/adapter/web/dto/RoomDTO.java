package com.alfre.DHHotel.adapter.web.dto;

import com.alfre.DHHotel.domain.model.RoomStatus;
import com.alfre.DHHotel.domain.model.RoomType;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * Data Transfer Object (DTO) for room details.
 * Encapsulates room-related information such as number, type, price, and status.
 *
 * <p>This class utilizes Lombok annotations to reduce boilerplate code:</p>
 * <ul>
 *   <li>{@code @Builder} - Provides a builder pattern for creating instances.</li>
 * </ul>
 *
 * @author Alfredo Sobrados González
 */
@Builder
public class RoomDTO {
    /**
     * The unique room number.
     */
    public int room_number;

    /**
     * The type of the room (e.g., SINGLE, DOUBLE, SUITE).
     */
    public RoomType type;

    /**
     * The price per night for booking the room.
     */
    public BigDecimal price_per_night;

    /**
     * The current status of the room (e.g., AVAILABLE, OCCUPIED, MAINTENANCE).
     */
    public RoomStatus status;
}
