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

@Service
public class PaymentUseCase {
    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;

    public PaymentUseCase(PaymentRepository paymentRepository, ReservationRepository reservationRepository) {
        this.paymentRepository = paymentRepository;
        this.reservationRepository = reservationRepository;
    }

    public List<Payment> getAllPayments() {
        return paymentRepository.getAllPayments();
    }

    public Optional<Payment> getPaymentById(long id) {
        return paymentRepository.getPaymentById(id);
    }

    public List<Payment> getPaymentsByReservationId(long reservationId) {
        return paymentRepository.getPaymentsByReservationId(reservationId);
    }

    public int updatePayment(Payment updatedPayment, long id) {
        Payment payment = paymentRepository.getPaymentById(id)
                .orElseThrow(() -> new IllegalArgumentException("El pago no existe"));

        Reservation reservation = reservationRepository.getReservationById(payment.reservation_id)
                .orElseThrow(() -> new RuntimeException("Reserva asociada al pago no encontrada"));

        // Validar que estamos actualizando algún valor del pago
        if(updatedPayment.amount == null && updatedPayment.payment_date == null && updatedPayment.method == null) {
            throw new RuntimeException("Debes indicar algún campo para actualizar");
        }

        // Validar que se ha indicado un nuevo importe del pago
        if(updatedPayment.amount != null) {
            payment.setAmount(updatedPayment.amount);
        }

        // Validar que se ha indicado una fecha de pago
        if(updatedPayment.payment_date != null) {
            payment.setPayment_date(updatedPayment.payment_date);
        }

        // Validar que se ha indicado un método de pago
        if(updatedPayment.method != null) {
            payment.setMethod(updatedPayment.method);
        }

        // Actualizamos el pago y su reserva asociada en función de la entrada recibida
        if(updatedPayment.amount == null){
            return paymentRepository.updatePayment(payment, id);
        } else {
            int rowsAffected = paymentRepository.updatePayment(payment, id);

            // Recalcular estado de la reserva
            BigDecimal paidTotal = paymentRepository.getTotalPaid(reservation.id);

            if (paidTotal.compareTo(reservation.total_price) < 0) {
                reservation.setStatus(ReservationStatus.PENDING);
                reservationRepository.updateReservation(reservation);
            } else if (paidTotal.compareTo(reservation.total_price) == 0) {
                reservation.setStatus(ReservationStatus.CONFIRMED);
                reservationRepository.updateReservation(reservation);
            } else {
                throw new RuntimeException("El importe del pago excede el precio total de la reserva asociada");
            }

            return rowsAffected;
        }
    }

    public int deletePayment(Long id) {
        Payment payment = paymentRepository.getPaymentById(id)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado"));

        Reservation reservation = reservationRepository.getReservationById(payment.reservation_id)
                .orElseThrow(() -> new RuntimeException("Reserva asociada al pago no encontrada"));

        int rowsAffected = paymentRepository.deletePayment(id);

        // Recalcular estado de la reserva
        BigDecimal paidTotal = paymentRepository.getTotalPaid(reservation.id);

        if (paidTotal.compareTo(reservation.total_price) < 0) {
            reservation.setStatus(ReservationStatus.PENDING);
            reservationRepository.updateReservation(reservation);
        }

        return rowsAffected;
    }
}