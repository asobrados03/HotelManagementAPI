package com.alfre.DHHotel.usecase;

import com.alfre.DHHotel.domain.model.*;
import com.alfre.DHHotel.domain.repository.PaymentRepository;
import com.alfre.DHHotel.domain.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * This class contains the attributes and methods for realize the unit tests
 * of the payments operations business logic.
 *
 * @author Alfredo Sobrados González
 */
@ExtendWith(MockitoExtension.class)
public class PaymentUseCaseTest {
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ReservationRepository reservationRepository;

    private PaymentUseCase paymentUseCase;

    /**
     * Initializes the PaymentUseCase instance with the required repositories before each test.
     */
    @BeforeEach
    public void setup() {
        paymentUseCase = new PaymentUseCase(paymentRepository, reservationRepository);
    }

    /**
     * Tests that getAllPayments() returns the expected list of payments.
     */
    @Test
    public void getAllPayments_success() {
        // Arrange: Prepare a list of payments.
        List<Payment> paymentList = new ArrayList<>();
        Payment payment = new Payment();
        // Optionally configure payment properties if needed.
        paymentList.add(payment);
        when(paymentRepository.getAllPayments()).thenReturn(paymentList);

        // Act: Execute getAllPayments().
        List<Payment> result = paymentUseCase.getAllPayments();

        // Assert: Verify that the result matches the expected list.
        assertEquals(paymentList, result);
        verify(paymentRepository).getAllPayments();
    }

    /**
     * Tests that getPaymentById(long) returns the expected payment when found.
     */
    @Test
    public void getPaymentById_success() {
        // Arrange: Prepare a payment with a specific id.
        Payment payment = new Payment();
        when(paymentRepository.getPaymentById(10L)).thenReturn(Optional.of(payment));

        // Act: Execute getPaymentById().
        Optional<Payment> result = paymentUseCase.getPaymentById(10L);

        // Assert: Verify that the result is present and matches the expected payment.
        assertTrue(result.isPresent());
        assertEquals(payment, result.get());
        verify(paymentRepository).getPaymentById(10L);
    }

    /**
     * Tests that getPaymentsByReservationId(long) returns the expected list of payments.
     */
    @Test
    public void getPaymentsByReservationId_success() {
        // Arrange: Prepare a list of payments for a reservation.
        List<Payment> paymentList = new ArrayList<>();
        when(paymentRepository.getPaymentsByReservationId(10L)).thenReturn(paymentList);

        // Act: Execute getPaymentsByReservationId().
        List<Payment> result = paymentUseCase.getPaymentsByReservationId(10L);

        // Assert: Verify that the returned list matches the expected list.
        assertEquals(paymentList, result);
        verify(paymentRepository).getPaymentsByReservationId(10L);
    }

    /**
     * Tests that updatePayment() successfully updates a payment and sets the reservation status to CONFIRMED
     * when the total paid equals the reservation's total price.
     */
    @Test
    public void updatePayment_success() {
        // Arrange
        long id = 1L;
        // Existing payment with reservation reference.
        Payment payment = new Payment();
        payment.reservation_id = 100L;
        payment.amount = new BigDecimal("20.00"); // previous amount

        // Updated data: new amount, payment date, and method.
        Payment updatedPayment = new Payment();
        updatedPayment.amount = new BigDecimal("50.00");
        updatedPayment.payment_date = LocalDate.now();
        updatedPayment.method = MethodPayment.CARD;

        // Stub: payment exists.
        when(paymentRepository.getPaymentById(id)).thenReturn(Optional.of(payment));

        // Stub: associated reservation exists.
        Reservation reservation = new Reservation();
        reservation.id = 100L;
        reservation.total_price = new BigDecimal("100.00");
        when(reservationRepository.getReservationById(payment.reservation_id))
                .thenReturn(Optional.of(reservation));

        // Stub: updatePayment returns 1 row updated.
        when(paymentRepository.updatePayment(payment, id)).thenReturn(1);

        // Stub: total paid equals reservation total.
        when(paymentRepository.getTotalPaid(reservation.id))
                .thenReturn(new BigDecimal("100.00"));

        // Act: Execute updatePayment.
        int rowsAffected = paymentUseCase.updatePayment(updatedPayment, id);

        // Assert: Verify that one row is affected and the reservation status is updated to CONFIRMED.
        assertEquals(1, rowsAffected);
        assertEquals(ReservationStatus.CONFIRMED, reservation.status);

        // Verify expected method calls.
        verify(paymentRepository).getPaymentById(id);
        verify(reservationRepository).getReservationById(payment.reservation_id);
        verify(paymentRepository).updatePayment(payment, id);
        verify(paymentRepository).getTotalPaid(reservation.id);
        verify(reservationRepository).updateReservation(reservation);
    }

    /**
     * Tests that updatePayment() throws an IllegalArgumentException when the payment is not found.
     */
    @Test
    public void updatePayment_paymentNotFound_throwsException() {
        // Arrange
        long id = 1L;
        Payment updatedPayment = new Payment();
        // Stub: payment not found.
        when(paymentRepository.getPaymentById(id)).thenReturn(Optional.empty());

        // Act & Assert: Verify that an IllegalArgumentException is thrown.
        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                paymentUseCase.updatePayment(updatedPayment, id)
        );
        assertEquals("El pago no existe", ex.getMessage());
        verify(paymentRepository).getPaymentById(id);
    }

    /**
     * Tests that updatePayment() throws a RuntimeException when the associated reservation is not found.
     */
    @Test
    public void updatePayment_reservationNotFound_throwsException() {
        // Arrange
        long id = 1L;
        Payment payment = new Payment();
        payment.reservation_id = 100L;
        Payment updatedPayment = new Payment();
        updatedPayment.amount = new BigDecimal("50.00");

        when(paymentRepository.getPaymentById(id)).thenReturn(Optional.of(payment));
        when(reservationRepository.getReservationById(payment.reservation_id)).thenReturn(Optional.empty());

        // Act & Assert: Verify that a RuntimeException is thrown with the expected message.
        Exception ex = assertThrows(RuntimeException.class, () ->
                paymentUseCase.updatePayment(updatedPayment, id)
        );
        assertEquals("Reserva asociada al pago no encontrada", ex.getMessage());
        verify(paymentRepository).getPaymentById(id);
        verify(reservationRepository).getReservationById(payment.reservation_id);
    }

    /**
     * Tests that updatePayment() throws a RuntimeException when no fields are provided for update.
     */
    @Test
    public void updatePayment_noFieldToUpdate_throwsException() {
        // Arrange
        long id = 1L;
        Payment payment = new Payment();
        payment.reservation_id = 100L;
        Payment updatedPayment = new Payment(); // All fields null

        when(paymentRepository.getPaymentById(id)).thenReturn(Optional.of(payment));

        Reservation reservation = new Reservation();
        reservation.id = 100L;
        reservation.total_price = new BigDecimal("100.00");
        when(reservationRepository.getReservationById(payment.reservation_id)).thenReturn(Optional.of(reservation));

        // Act & Assert: Expect RuntimeException for missing update fields.
        Exception ex = assertThrows(RuntimeException.class, () ->
                paymentUseCase.updatePayment(updatedPayment, id)
        );
        assertEquals("Debes indicar algún campo para actualizar", ex.getMessage());
        verify(paymentRepository).getPaymentById(id);
        verify(reservationRepository).getReservationById(payment.reservation_id);
    }

    /**
     * Tests that updatePayment() updates only non-null fields when the amount is null,
     * and does not update the reservation.
     */
    @Test
    public void updatePayment_amountNull_updatesOtherFieldsOnly() {
        // Arrange
        long id = 1L;
        Payment payment = new Payment();
        payment.reservation_id = 100L;
        payment.amount = new BigDecimal("20.00");

        Payment updatedPayment = new Payment();
        updatedPayment.amount = null; // Amount not updated
        updatedPayment.payment_date = LocalDate.now();
        updatedPayment.method = MethodPayment.CARD;

        when(paymentRepository.getPaymentById(id)).thenReturn(Optional.of(payment));

        Reservation reservation = new Reservation();
        reservation.id = 100L;
        reservation.total_price = new BigDecimal("100.00");
        when(reservationRepository.getReservationById(payment.reservation_id)).thenReturn(Optional.of(reservation));

        // Stub: updatePayment returns success.
        when(paymentRepository.updatePayment(payment, id)).thenReturn(1);

        // Act
        int result = paymentUseCase.updatePayment(updatedPayment, id);

        // Assert: Verify that the update was successful and that payment_date and method were updated.
        assertEquals(1, result);
        assertEquals(updatedPayment.payment_date, payment.payment_date);
        assertEquals(updatedPayment.method, payment.method);

        verify(paymentRepository).updatePayment(payment, id);
        // Reservation should not be updated.
        verify(reservationRepository, never()).updateReservation(any());
    }

    /**
     * Tests that when updating a payment with a new amount and the total paid is less than the reservation total,
     * the reservation status is set to PENDING.
     */
    @Test
    public void updatePayment_amountNotNull_paidTotalLessThanTotalPrice_setsPending() {
        // Arrange
        long id = 1L;
        Payment payment = new Payment();
        payment.reservation_id = 100L;
        payment.amount = new BigDecimal("20.00");

        Payment updatedPayment = new Payment();
        updatedPayment.amount = new BigDecimal("50.00");
        updatedPayment.payment_date = LocalDate.now();
        updatedPayment.method = MethodPayment.CASH;

        when(paymentRepository.getPaymentById(id)).thenReturn(Optional.of(payment));

        Reservation reservation = new Reservation();
        reservation.id = 100L;
        reservation.total_price = new BigDecimal("100.00");
        when(reservationRepository.getReservationById(payment.reservation_id)).thenReturn(Optional.of(reservation));
        when(paymentRepository.updatePayment(payment, id)).thenReturn(1);

        // Total paid is less than total_price.
        when(paymentRepository.getTotalPaid(reservation.id)).thenReturn(new BigDecimal("70.00"));

        // Act
        int rowsAffected = paymentUseCase.updatePayment(updatedPayment, id);

        // Assert: Verify that the update succeeded and the reservation status remains PENDING.
        assertEquals(1, rowsAffected);
        assertEquals(ReservationStatus.PENDING, reservation.status);

        verify(paymentRepository).updatePayment(payment, id);
        verify(paymentRepository).getTotalPaid(reservation.id);
        verify(reservationRepository).updateReservation(reservation);
    }

    /**
     * Tests that when updating a payment with a new amount and the total paid equals the reservation total,
     * the reservation status is set to CONFIRMED.
     */
    @Test
    public void updatePayment_amountNotNull_paidTotalEqualsTotalPrice_setsConfirmed() {
        // Arrange
        long id = 1L;
        Payment payment = new Payment();
        payment.reservation_id = 100L;
        payment.amount = new BigDecimal("20.00");

        Payment updatedPayment = new Payment();
        updatedPayment.amount = new BigDecimal("50.00");
        updatedPayment.payment_date = LocalDate.now();
        updatedPayment.method = MethodPayment.TRANSFER;

        when(paymentRepository.getPaymentById(id)).thenReturn(Optional.of(payment));

        Reservation reservation = new Reservation();
        reservation.id = 100L;
        reservation.total_price = new BigDecimal("100.00");
        when(reservationRepository.getReservationById(payment.reservation_id)).thenReturn(Optional.of(reservation));
        when(paymentRepository.updatePayment(payment, id)).thenReturn(1);

        // Total paid equals total_price.
        when(paymentRepository.getTotalPaid(reservation.id)).thenReturn(new BigDecimal("100.00"));

        // Act
        int rowsAffected = paymentUseCase.updatePayment(updatedPayment, id);

        // Assert: Verify that the update succeeded and the reservation status is CONFIRMED.
        assertEquals(1, rowsAffected);
        assertEquals(ReservationStatus.CONFIRMED, reservation.status);

        verify(paymentRepository).updatePayment(payment, id);
        verify(paymentRepository).getTotalPaid(reservation.id);
        verify(reservationRepository).updateReservation(reservation);
    }

    /**
     * Tests that when updating a payment causes the total paid to exceed the reservation total,
     * a RuntimeException is thrown.
     */
    @Test
    public void updatePayment_amountNotNull_paidTotalGreaterThanTotalPrice_throwsException() {
        // Arrange
        long id = 1L;
        Payment payment = new Payment();
        payment.reservation_id = 100L;
        payment.amount = new BigDecimal("20.00");

        Payment updatedPayment = new Payment();
        updatedPayment.amount = new BigDecimal("50.00");
        updatedPayment.payment_date = LocalDate.now();
        updatedPayment.method = MethodPayment.CASH;

        when(paymentRepository.getPaymentById(id)).thenReturn(Optional.of(payment));

        Reservation reservation = new Reservation();
        reservation.id = 100L;
        reservation.total_price = new BigDecimal("100.00");
        when(reservationRepository.getReservationById(payment.reservation_id)).thenReturn(Optional.of(reservation));
        when(paymentRepository.updatePayment(payment, id)).thenReturn(1);

        // Total paid is greater than total_price.
        when(paymentRepository.getTotalPaid(reservation.id)).thenReturn(new BigDecimal("150.00"));

        // Act
        Exception ex = assertThrows(RuntimeException.class, () ->
                paymentUseCase.updatePayment(updatedPayment, id)
        );

        // Assert: Verify that the correct exception is thrown.
        assertEquals("El importe del pago excede el precio total de la reserva asociada", ex.getMessage());

        verify(paymentRepository).updatePayment(payment, id);
        verify(paymentRepository).getTotalPaid(reservation.id);
        verify(reservationRepository, never()).updateReservation(any());
    }

    /**
     * Tests that deletePayment() throws a RuntimeException when the payment is not found.
     */
    @Test
    public void deletePayment_paymentNotFound_throwsException() {
        // Arrange
        long id = 1L;
        when(paymentRepository.getPaymentById(id)).thenReturn(Optional.empty());

        // Act & Assert: Verify that a RuntimeException is thrown with the message "Pago no encontrado".
        Exception ex = assertThrows(RuntimeException.class, () ->
                paymentUseCase.deletePayment(id)
        );
        assertEquals("Pago no encontrado", ex.getMessage());
        verify(paymentRepository).getPaymentById(id);
    }

    /**
     * Tests that deletePayment() throws a RuntimeException when the associated reservation is not found.
     */
    @Test
    public void deletePayment_reservationNotFound_throwsException() {
        // Arrange
        long id = 1L;
        Payment payment = new Payment();
        payment.reservation_id = 100L;
        when(paymentRepository.getPaymentById(id)).thenReturn(Optional.of(payment));
        when(reservationRepository.getReservationById(payment.reservation_id)).thenReturn(Optional.empty());

        // Act & Assert: Verify that a RuntimeException is thrown with the correct message.
        Exception ex = assertThrows(RuntimeException.class, () ->
                paymentUseCase.deletePayment(id)
        );
        assertEquals("Reserva asociada al pago no encontrada", ex.getMessage());
        verify(paymentRepository).getPaymentById(id);
        verify(reservationRepository).getReservationById(payment.reservation_id);
    }

    /**
     * Tests that deletePayment() updates the reservation when the total paid is less than the reservation's total price.
     * <p>
     * After deleting the payment, if the total paid is less than the reservation's total price, the reservation status should be set to PENDING.
     * </p>
     */
    @Test
    public void deletePayment_paidTotalLessThanTotalPrice_updatesReservation() {
        // Arrange
        long id = 1L;
        Payment payment = new Payment();
        payment.reservation_id = 100L;
        when(paymentRepository.getPaymentById(id)).thenReturn(Optional.of(payment));

        Reservation reservation = new Reservation();
        reservation.id = 100L;
        reservation.total_price = new BigDecimal("100.00");
        when(reservationRepository.getReservationById(payment.reservation_id))
                .thenReturn(Optional.of(reservation));

        int rowsAffected = 1;
        when(paymentRepository.deletePayment(id)).thenReturn(rowsAffected);
        // Simulate total paid less than total_price (e.g., 80.00)
        when(paymentRepository.getTotalPaid(reservation.id)).thenReturn(new BigDecimal("80.00"));

        // Act
        int result = paymentUseCase.deletePayment(id);

        // Assert: Verify the result and that the reservation status is updated to PENDING.
        assertEquals(rowsAffected, result);
        assertEquals(ReservationStatus.PENDING, reservation.status);
        verify(paymentRepository).getPaymentById(id);
        verify(reservationRepository).getReservationById(payment.reservation_id);
        verify(paymentRepository).deletePayment(id);
        verify(paymentRepository).getTotalPaid(reservation.id);
        verify(reservationRepository).updateReservation(reservation);
    }

    /**
     * Tests that deletePayment() does not update the reservation when the total paid is greater than or equal to the reservation's total price.
     * <p>
     * After deletion, if the total paid is not less than the reservation total, the reservation update should not be invoked.
     * </p>
     */
    @Test
    public void deletePayment_paidTotalGreaterOrEqualTotalPrice_doesNotUpdateReservation() {
        // Arrange
        long id = 1L;
        Payment payment = new Payment();
        payment.reservation_id = 100L;
        when(paymentRepository.getPaymentById(id)).thenReturn(Optional.of(payment));

        Reservation reservation = new Reservation();
        reservation.id = 100L;
        reservation.total_price = new BigDecimal("100.00");
        reservation.setStatus(ReservationStatus.CONFIRMED);
        when(reservationRepository.getReservationById(payment.reservation_id))
                .thenReturn(Optional.of(reservation));

        int rowsAffected = 1;
        when(paymentRepository.deletePayment(id)).thenReturn(rowsAffected);
        // Simulate total paid equal to total_price (100.00)
        when(paymentRepository.getTotalPaid(reservation.id)).thenReturn(new BigDecimal("100.00"));

        // Act
        int result = paymentUseCase.deletePayment(id);

        // Assert: Verify that the result is as expected and that updateReservation is not invoked.
        assertEquals(rowsAffected, result);
        verify(paymentRepository).getPaymentById(id);
        verify(reservationRepository).getReservationById(payment.reservation_id);
        verify(paymentRepository).deletePayment(id);
        verify(paymentRepository).getTotalPaid(reservation.id);
        verify(reservationRepository, never()).updateReservation(any());
    }
}