package com.alfre.DHHotel.domain.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Room {
    public long id;
    public int room_number;
    public RoomType type;
    public BigDecimal price_per_night;
    public RoomStatus status;
}