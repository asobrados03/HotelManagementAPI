package com.alfre.DHHotel.domain.repository;

import com.alfre.DHHotel.domain.model.Payment;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing payment data.
 * Defines CRUD operations for payments in the system.
 *
 * <p>This interface should be implemented by a class that interacts with the database.</p>
 *
 * @author Alfredo
 */
public interface PaymentRepository {

    /**
     * Retrieves a list of all payments in the system.
     *
     * @return a list of {@link Payment} objects.
     */
    List<Payment> getAllPayments();

    /**
     * Retrieves a payment based on its unique ID.
     *
     * @param id the unique identifier of the payment.
     * @return an {@code Optional} containing the payment if found, otherwise empty.
     */
    Optional<Payment> getPaymentById(long id);

    /**
     * Retrieves a list of payments associated with a specific reservation.
     *
     * @param reservationId the unique identifier of the reservation.
     * @return a list of {@link Payment} objects associated with the reservation.
     */
    List<Payment> getPaymentsByReservationId(long reservationId);

    /**
     * Creates a new payment in the system.
     *
     * @param payment the {@link Payment} object to be added.
     * @return the generated unique identifier of the newly created payment.
     */
    long createPayment(Payment payment);

    /**
     * Updates the details of an existing payment.
     *
     * @param payment the payment object containing updated information.
     * @param id the unique identifier of the payment to be updated.
     * @return the number of rows affected in the database.
     */
    int updatePayment(Payment payment, long id);

    /**
     * Deletes a payment from the system based on its unique ID.
     *
     * @param id the unique identifier of the payment to be deleted.
     * @return the number of rows affected in the database.
     */
    int deletePayment(long id);

    /**
     * Retrieves the total amount paid for a specific reservation.
     *
     * @param reservationId the unique identifier of the reservation.
     * @return the total amount paid as a {@link BigDecimal}.
     */
    BigDecimal getTotalPaid(long reservationId);

    /**
     * Retrieves a list of payments associated with a specific client.
     *
     * @param clientId the unique identifier of the client.
     * @return a list of {@link Payment} objects associated with the client.
     */
    List<Payment> getPaymentsByClient(long clientId);

    /**
     * Deletes all payment records from the system.
     * <p><b>Warning:</b> This action is irreversible.</p>
     */
    void deleteAll();
}
