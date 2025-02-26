package com.alfre.DHHotel.domain.model;

/**
 * Enum representing the available payment methods.
 * Defines the different ways a client can make a payment.
 *
 * <p>Possible values:</p>
 * <ul>
 *   <li>{@code CARD} - Payment made using a credit or debit card.</li>
 *   <li>{@code CASH} - Payment made in cash.</li>
 *   <li>{@code TRANSFER} - Payment made via bank transfer.</li>
 * </ul>
 *
 * This enum can be used to enforce valid payment methods in transactions.
 *
 * @author Alfredo Sobrados González
 */
public enum MethodPayment {
    /**
     * Payment made using a credit or debit card.
     */
    CARD,

    /**
     * Payment made in cash.
     */
    CASH,

    /**
     * Payment made via bank transfer.
     */
    TRANSFER
}
