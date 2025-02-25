package com.alfre.DHHotel.domain.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Setter
public class Reservation {
    public long id;
    public Long client_id;
    public Long room_id;
    public BigDecimal total_price;
    public Date start_date;
    public Date end_date;
    public ReservationStatus status;
}