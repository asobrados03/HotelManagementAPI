package com.alfre.DHHotel.domain.repository;

import com.alfre.DHHotel.domain.model.Payment;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository {
    List<Payment> getAllPayments();
    Optional<Payment> getPaymentById(long id);
    List<Payment> getPaymentsByReservationId(long reservationId);
    long createPayment(Payment payment);
    int updatePayment(Payment payment, long id);
    int deletePayment(long id);
    BigDecimal getTotalPaid(long reservationId);
    List<Payment> getPaymentsByClient(long clientId);
    void deleteAll();
}
