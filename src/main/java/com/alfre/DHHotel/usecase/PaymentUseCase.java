package com.alfre.DHHotel.usecase;

import com.alfre.DHHotel.domain.model.Payment;
import com.alfre.DHHotel.domain.model.Reservation;
import com.alfre.DHHotel.domain.model.ReservationStatus;
import com.alfre.DHHotel.domain.repository.PaymentRepository;
import com.alfre.DHHotel.domain.repository.ReservationRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Service class that handles business logic for payment-related operations.
 * It interacts with the {@link PaymentRepository} and {@link ReservationRepository}
 * to manage payments and update reservation statuses accordingly.
 * <p>
 * This class provides methods for retrieving, updating, and deleting payments
 * while ensuring proper validation and transaction consistency.
 * </p>
 *
 * @author Alfredo Sobrados González
 */
@Service
public class PaymentUseCase {
    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;

    /**
     * Constructs a new PaymentUseCase with the given repositories.
     *
     * @param paymentRepository the repository for managing payments
     * @param reservationRepository the repository for managing reservations
     */
    public PaymentUseCase(PaymentRepository paymentRepository, ReservationRepository reservationRepository) {
        this.paymentRepository = paymentRepository;
        this.reservationRepository = reservationRepository;
    }

    /**
     * Retrieves a list of all payments.
     *
     * @return a {@link List} of {@link Payment} objects.
     */
    public List<Payment> getAllPayments() {
        return paymentRepository.getAllPayments();
    }

    /**
     * Retrieves a payment by its ID.
     *
     * @param id the ID of the payment.
     * @return an {@link Optional} containing the {@link Payment}, if found.
     */
    public Optional<Payment> getPaymentById(long id) {
        return paymentRepository.getPaymentById(id);
    }

    /**
     * Retrieves all payments associated with a specific reservation.
     *
     * @param reservationId the ID of the reservation.
     * @return a {@link List} of {@link Payment} objects associated with the reservation.
     */
    public List<Payment> getPaymentsByReservationId(long reservationId) {
        return paymentRepository.getPaymentsByReservationId(reservationId);
    }

    /**
     * Updates a payment record.
     * <p>
     * This method validates and updates the payment details. It also recalculates
     * the status of the associated reservation based on the total amount paid.
     * </p>
     *
     * @param updatedPayment the updated payment details.
     * @param id the ID of the payment to update.
     * @return the number of rows affected in the database.
     * @throws IllegalArgumentException if the payment does not exist.
     * @throws RuntimeException if the total paid amount exceeds the reservation price.
     */
    public int updatePayment(Payment updatedPayment, long id) {
        Payment payment = paymentRepository.getPaymentById(id)
                .orElseThrow(() -> new IllegalArgumentException("El pago no existe"));

        Reservation reservation = reservationRepository.getReservationById(payment.reservation_id)
                .orElseThrow(() -> new RuntimeException("Reserva asociada al pago no encontrada"));

        // Validate that at least one field is being updated
        if (updatedPayment.amount == null && updatedPayment.payment_date == null && updatedPayment.method == null) {
            throw new RuntimeException("Debes indicar algún campo para actualizar");
        }

        // Update payment fields if provided
        if (updatedPayment.amount != null) {
            payment.setAmount(updatedPayment.amount);
        }
        if (updatedPayment.payment_date != null) {
            payment.setPayment_date(updatedPayment.payment_date);
        }
        if (updatedPayment.method != null) {
            payment.setMethod(updatedPayment.method);
        }

        // Update payment and reservation if necessary
        int rowsAffected = paymentRepository.updatePayment(payment, id);

        if (updatedPayment.amount != null) {
            updateReservationStatus(reservation);
        }

        return rowsAffected;
    }

    /**
     * Deletes a payment and updates the status of the associated reservation.
     *
     * @param id the ID of the payment to delete.
     * @return the number of rows affected in the database.
     * @throws RuntimeException if the payment or associated reservation is not found.
     */
    public int deletePayment(Long id) {
        Payment payment = paymentRepository.getPaymentById(id)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado"));

        Reservation reservation = reservationRepository.getReservationById(payment.reservation_id)
                .orElseThrow(() -> new RuntimeException("Reserva asociada al pago no encontrada"));

        int rowsAffected = paymentRepository.deletePayment(id);

        // Recalculate reservation status after payment deletion
        updateReservationStatus(reservation);

        return rowsAffected;
    }

    /**
     * Updates the status of a reservation based on the total amount paid.
     *
     * @param reservation the reservation whose status needs to be updated.
     * @throws RuntimeException if the total paid amount exceeds the reservation price.
     */
    private void updateReservationStatus(Reservation reservation) {
        BigDecimal paidTotal = paymentRepository.getTotalPaid(reservation.id);

        if (paidTotal.compareTo(reservation.total_price) < 0) {
            reservation.setStatus(ReservationStatus.PENDING);
        } else if (paidTotal.compareTo(reservation.total_price) == 0) {
            reservation.setStatus(ReservationStatus.CONFIRMED);
        } else {
            throw new RuntimeException("El importe del pago excede el precio total de la reserva asociada");
        }

        reservationRepository.updateReservation(reservation);
    }
}
