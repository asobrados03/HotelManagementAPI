package com.alfre.DHHotel.domain.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity class representing a payment.
 * This class maps to the payments table in the database and contains essential attributes.
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
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Payment {
    /**
     * The unique identifier for the payment.
     */
    public long id;

    /**
     * The ID of the reservation associated with this payment.
     */
    public long reservation_id;

    /**
     * The total amount of the payment.
     */
    public BigDecimal amount;

    /**
     * The date when the payment was made.
     */
    public LocalDate payment_date;

    /**
     * The method used for the payment (CARD, CASH, TRANSFER).
     */
    public MethodPayment method;
}