package com.alfre.DHHotel.domain.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Payment {
    public long id;
    public long reservation_id;
    public BigDecimal amount;
    public Date payment_date;
    public MethodPayment method;
}