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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentUseCaseTest {
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ReservationRepository reservationRepository;

    private PaymentUseCase paymentUseCase;

    @BeforeEach
    public void setup() {
        paymentUseCase = new PaymentUseCase(paymentRepository, reservationRepository);
    }

    @Test
    public void getAllPayments_success() {
        // Arrange: Preparamos una lista de pagos
        List<Payment> paymentList = new ArrayList<>();
        Payment payment = new Payment();
        // Configuramos las propiedades de payment si es necesario
        paymentList.add(payment);
        when(paymentRepository.getAllPayments()).thenReturn(paymentList);

        // Act: Ejecutamos el método
        List<Payment> result = paymentUseCase.getAllPayments();

        // Assert: Verificamos que el resultado sea el esperado y se llamó al método del repository
        assertEquals(paymentList, result);
        verify(paymentRepository).getAllPayments();
    }

    @Test
    public void getPaymentById_success() {
        // Arrange: Creamos un cliente y simulamos su retorno para el id dado
        Payment payment = new Payment();
        when(paymentRepository.getPaymentById(10L)).thenReturn(Optional.of(payment));

        // Act
        Optional<Payment> result = paymentUseCase.getPaymentById(10L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(payment, result.get());
        verify(paymentRepository).getPaymentById(10L);
    }

    @Test
    public void getPaymentsByReservationId_success() {
        // Arrange: Creamos un cliente y simulamos su retorno para el id dado
        List<Payment> paymentList = new ArrayList<>();
        when(paymentRepository.getPaymentsByReservationId(10L)).thenReturn(paymentList);

        // Act
        List<Payment> result = paymentUseCase.getPaymentsByReservationId(10L);

        // Assert
        assertEquals(paymentList, result);
        verify(paymentRepository).getPaymentsByReservationId(10L);
    }

    @Test
    public void updatePayment_success() {
        // Arrange
        long id = 1L;

        // Pago existente con referencia a la reserva
        Payment payment = new Payment();
        payment.reservation_id = 100L;
        payment.amount = new BigDecimal("20.00"); // valor previo

        // Datos de actualización: se actualiza el importe, fecha y método
        Payment updatedPayment = new Payment();
        updatedPayment.amount = new BigDecimal("50.00");
        updatedPayment.payment_date = new Date();
        updatedPayment.method = MethodPayment.CARD;

        // Simulamos que se encuentra el pago en el repositorio
        when(paymentRepository.getPaymentById(id)).thenReturn(Optional.of(payment));

        // Simulamos la reserva asociada al pago
        Reservation reservation = new Reservation();
        reservation.id = 100L;
        reservation.total_price = new BigDecimal("100.00");
        when(reservationRepository.getReservationById(payment.reservation_id))
                .thenReturn(Optional.of(reservation));

        // Simulamos que se actualiza el pago en el repositorio (retornando 1 fila afectada)
        when(paymentRepository.updatePayment(payment, id)).thenReturn(1);

        // Simulamos que, tras actualizar el importe, el total pagado es igual al precio total de la reserva
        when(paymentRepository.getTotalPaid(reservation.id))
                .thenReturn(new BigDecimal("100.00"));

        // Act
        int rowsAffected = paymentUseCase.updatePayment(updatedPayment, id);

        // Assert and verify
        assertEquals(1, rowsAffected);
        assertEquals(ReservationStatus.CONFIRMED, reservation.status);

        // Verificamos que se llamaron los métodos esperados
        verify(paymentRepository).getPaymentById(id);
        verify(reservationRepository).getReservationById(payment.reservation_id);
        verify(paymentRepository).updatePayment(payment, id);
        verify(paymentRepository).getTotalPaid(reservation.id);
        verify(reservationRepository).updateReservation(reservation);
    }

    @Test
    public void updatePayment_paymentNotFound_throwsException() {
        // Arrange
        long id = 1L;

        Payment updatedPayment = new Payment();

        // Simulamos que no se encuentra el pago
        when(paymentRepository.getPaymentById(id)).thenReturn(Optional.empty());

        // Act
        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                paymentUseCase.updatePayment(updatedPayment, id)
        );

        // Assert and verify
        assertEquals("El pago no existe", ex.getMessage());
        verify(paymentRepository).getPaymentById(id);
    }

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

        // Act
        Exception ex = assertThrows(RuntimeException.class, () ->
                paymentUseCase.updatePayment(updatedPayment, id)
        );

        // Assert and verify
        assertEquals("Reserva asociada al pago no encontrada", ex.getMessage());
        verify(paymentRepository).getPaymentById(id);
        verify(reservationRepository).getReservationById(payment.reservation_id);
    }

    @Test
    public void updatePayment_noFieldToUpdate_throwsException() {
        // Arrange
        long id = 1L;

        Payment payment = new Payment();
        payment.reservation_id = 100L;
        Payment updatedPayment = new Payment(); // Todos los campos null

        when(paymentRepository.getPaymentById(id)).thenReturn(Optional.of(payment));

        Reservation reservation = new Reservation();
        reservation.id = 100L;
        reservation.total_price = new BigDecimal("100.00");

        when(reservationRepository.getReservationById(payment.reservation_id)).thenReturn(Optional.of(reservation));

        // Act
        Exception ex = assertThrows(RuntimeException.class, () ->
                paymentUseCase.updatePayment(updatedPayment, id)
        );

        // Assert and verify
        assertEquals("Debes indicar algún campo para actualizar", ex.getMessage());
        verify(paymentRepository).getPaymentById(id);
        verify(reservationRepository).getReservationById(payment.reservation_id);
    }

    @Test
    public void updatePayment_amountNull_updatesOtherFieldsOnly() {
        // Arrange
        long id = 1L;

        Payment payment = new Payment();
        payment.reservation_id = 100L;
        payment.amount = new BigDecimal("20.00");

        Payment updatedPayment = new Payment();
        updatedPayment.amount = null; // No se actualiza importe
        updatedPayment.payment_date = new Date();
        updatedPayment.method = MethodPayment.CARD;

        when(paymentRepository.getPaymentById(id)).thenReturn(Optional.of(payment));

        Reservation reservation = new Reservation();
        reservation.id = 100L;
        reservation.total_price = new BigDecimal("100.00");

        when(reservationRepository.getReservationById(payment.reservation_id)).thenReturn(Optional.of(reservation));

        // En este caso, se actualiza solo el pago (sin recalcular reserva)
        when(paymentRepository.updatePayment(payment, id)).thenReturn(1);

        // Act
        int result = paymentUseCase.updatePayment(updatedPayment, id);

        // Assert and verify
        assertEquals(1, result);

        // Se deben actualizar payment_date y method
        assertEquals(updatedPayment.payment_date, payment.payment_date);
        assertEquals(updatedPayment.method, payment.method);

        verify(paymentRepository).updatePayment(payment, id);
        verify(reservationRepository, never()).updateReservation(any());
    }

    @Test
    public void updatePayment_amountNotNull_paidTotalLessThanTotalPrice_setsPending() {
        // Arrange
        long id = 1L;

        Payment payment = new Payment();
        payment.reservation_id = 100L;
        payment.amount = new BigDecimal("20.00");

        Payment updatedPayment = new Payment();
        updatedPayment.amount = new BigDecimal("50.00"); // Se actualiza el importe
        updatedPayment.payment_date = new Date();
        updatedPayment.method = MethodPayment.CASH;

        when(paymentRepository.getPaymentById(id)).thenReturn(Optional.of(payment));

        Reservation reservation = new Reservation();
        reservation.id = 100L;
        reservation.total_price = new BigDecimal("100.00");

        when(reservationRepository.getReservationById(payment.reservation_id)).thenReturn(Optional.of(reservation));
        when(paymentRepository.updatePayment(payment, id)).thenReturn(1);

        // Total pagado menor que total_price
        when(paymentRepository.getTotalPaid(reservation.id)).thenReturn(new BigDecimal("70.00"));

        // Act
        int rowsAffected = paymentUseCase.updatePayment(updatedPayment, id);

        // Assert and verify
        assertEquals(1, rowsAffected);
        assertEquals(ReservationStatus.PENDING, reservation.status);

        verify(paymentRepository).updatePayment(payment, id);
        verify(paymentRepository).getTotalPaid(reservation.id);
        verify(reservationRepository).updateReservation(reservation);
    }

    @Test
    public void updatePayment_amountNotNull_paidTotalEqualsTotalPrice_setsConfirmed() {
        // Arrange
        long id = 1L;

        Payment payment = new Payment();
        payment.reservation_id = 100L;
        payment.amount = new BigDecimal("20.00");

        Payment updatedPayment = new Payment();
        updatedPayment.amount = new BigDecimal("50.00");
        updatedPayment.payment_date = new Date();
        updatedPayment.method = MethodPayment.TRANSFER;

        when(paymentRepository.getPaymentById(id)).thenReturn(Optional.of(payment));

        Reservation reservation = new Reservation();
        reservation.id = 100L;
        reservation.total_price = new BigDecimal("100.00");

        when(reservationRepository.getReservationById(payment.reservation_id)).thenReturn(Optional.of(reservation));
        when(paymentRepository.updatePayment(payment, id)).thenReturn(1);

        // Total pagado igual al precio total
        when(paymentRepository.getTotalPaid(reservation.id)).thenReturn(new BigDecimal("100.00"));

        // Act
        int rowsAffected = paymentUseCase.updatePayment(updatedPayment, id);

        // Assert and verify
        assertEquals(1, rowsAffected);
        assertEquals(ReservationStatus.CONFIRMED, reservation.status);

        verify(paymentRepository).updatePayment(payment, id);
        verify(paymentRepository).getTotalPaid(reservation.id);
        verify(reservationRepository).updateReservation(reservation);
    }

    @Test
    public void updatePayment_amountNotNull_paidTotalGreaterThanTotalPrice_throwsException() {
        // Arrange
        long id = 1L;

        Payment payment = new Payment();
        payment.reservation_id = 100L;
        payment.amount = new BigDecimal("20.00");

        Payment updatedPayment = new Payment();
        updatedPayment.amount = new BigDecimal("50.00");
        updatedPayment.payment_date = new Date();
        updatedPayment.method = MethodPayment.CASH;

        when(paymentRepository.getPaymentById(id)).thenReturn(Optional.of(payment));

        Reservation reservation = new Reservation();
        reservation.id = 100L;
        reservation.total_price = new BigDecimal("100.00");

        when(reservationRepository.getReservationById(payment.reservation_id)).thenReturn(Optional.of(reservation));
        when(paymentRepository.updatePayment(payment, id)).thenReturn(1);

        // Total pagado mayor que el precio total
        when(paymentRepository.getTotalPaid(reservation.id)).thenReturn(new BigDecimal("150.00"));

        // Act
        Exception ex = assertThrows(RuntimeException.class, () ->
                paymentUseCase.updatePayment(updatedPayment, id)
        );

        // Assert and verify
        assertEquals("El importe del pago excede el precio total de la reserva asociada", ex.getMessage());

        verify(paymentRepository).updatePayment(payment, id);
        verify(paymentRepository).getTotalPaid(reservation.id);
        verify(reservationRepository, never()).updateReservation(any());
    }

    @Test
    public void deletePayment_paymentNotFound_throwsException() {
        // Arrange
        long id = 1L;
        when(paymentRepository.getPaymentById(id)).thenReturn(Optional.empty());

        // Act
        Exception ex = assertThrows(RuntimeException.class, () ->
                paymentUseCase.deletePayment(id)
        );

        // Assert and verify
        assertEquals("Pago no encontrado", ex.getMessage());
        verify(paymentRepository).getPaymentById(id);
    }

    @Test
    public void deletePayment_reservationNotFound_throwsException() {
        // Arrange
        long id = 1L;
        Payment payment = new Payment();
        payment.reservation_id = 100L;
        when(paymentRepository.getPaymentById(id)).thenReturn(Optional.of(payment));
        when(reservationRepository.getReservationById(payment.reservation_id)).thenReturn(Optional.empty());

        // Act
        Exception ex = assertThrows(RuntimeException.class, () ->
                paymentUseCase.deletePayment(id)
        );

        // Assert and verify
        assertEquals("Reserva asociada al pago no encontrada", ex.getMessage());
        verify(paymentRepository).getPaymentById(id);
        verify(reservationRepository).getReservationById(payment.reservation_id);
    }

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
        // Simulamos que el total pagado es menor al precio total (por ejemplo, 80.00)
        when(paymentRepository.getTotalPaid(reservation.id)).thenReturn(new BigDecimal("80.00"));

        // Act
        int result = paymentUseCase.deletePayment(id);

        // Assert and verify
        assertEquals(rowsAffected, result);
        // Como paidTotal (80.00) < total_price (100.00), se actualiza el estado a PENDING
        assertEquals(ReservationStatus.PENDING, reservation.status);
        verify(paymentRepository).getPaymentById(id);
        verify(reservationRepository).getReservationById(payment.reservation_id);
        verify(paymentRepository).deletePayment(id);
        verify(paymentRepository).getTotalPaid(reservation.id);
        verify(reservationRepository).updateReservation(reservation);
    }

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
        when(reservationRepository.getReservationById(payment.reservation_id))
                .thenReturn(Optional.of(reservation));

        int rowsAffected = 1;
        when(paymentRepository.deletePayment(id)).thenReturn(rowsAffected);
        // Simulamos que el total pagado es igual al precio total (100.00)
        when(paymentRepository.getTotalPaid(reservation.id)).thenReturn(new BigDecimal("100.00"));

        // Act
        int result = paymentUseCase.deletePayment(id);

        // Assert and verify
        assertEquals(rowsAffected, result);
        // Como paidTotal (100.00) no es menor que total_price (100.00), no se actualiza la reserva
        // Se puede verificar que updateReservation no se invoque:
        verify(paymentRepository).getPaymentById(id);
        verify(reservationRepository).getReservationById(payment.reservation_id);
        verify(paymentRepository).deletePayment(id);
        verify(paymentRepository).getTotalPaid(reservation.id);
        verify(reservationRepository, never()).updateReservation(any());
    }
}