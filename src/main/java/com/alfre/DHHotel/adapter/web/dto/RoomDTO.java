package com.alfre.DHHotel.adapter.web.dto;

import com.alfre.DHHotel.domain.model.RoomStatus;
import com.alfre.DHHotel.domain.model.RoomType;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public class RoomDTO {
    public int room_number;
    public RoomType type;
    public BigDecimal price_per_night;
    public RoomStatus status;
}
