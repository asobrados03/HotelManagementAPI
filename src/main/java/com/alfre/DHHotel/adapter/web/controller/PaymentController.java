package com.alfre.DHHotel.adapter.web.controller;

import com.alfre.DHHotel.domain.model.Payment;
import com.alfre.DHHotel.usecase.PaymentUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static java.util.Collections.emptyList;

@RestController
@RequestMapping("/api")
public class PaymentController {
    private final PaymentUseCase paymentUseCase;

    public PaymentController(PaymentUseCase paymentUseCase) {
        this.paymentUseCase = paymentUseCase;
    }

    @GetMapping("/admin/payments")
    public ResponseEntity<?> getAllPayments() {
        try {
            List<Payment> response = paymentUseCase.getAllPayments();

            if (response.equals(emptyList())) {
                throw new RuntimeException("No hay pagos registrados en el sistema.");
            } else {
                return ResponseEntity.ok(response);
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/admin/payment/{id}")
    public ResponseEntity<?> getPaymentById(@PathVariable long id) {
        try {
            Payment response = paymentUseCase.getPaymentById(id)
                .orElseThrow(() -> new RuntimeException("El pago solicitado no existe"));

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/admin/payment/reservation/id/{reservationId}")
    public ResponseEntity<?> getPaymentsByReservationId(@PathVariable long reservationId) {
        try {
            List<Payment> response = paymentUseCase.getPaymentsByReservationId(reservationId);

            if (response.equals(emptyList())) {
                throw new RuntimeException("No hay pagos registrados asociados a la reserva en el sistema.");
            } else {
                return ResponseEntity.ok(response);
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PutMapping("/superadmin/payment/{id}")
    public ResponseEntity<?> updatePayment(@RequestBody Payment payment, @PathVariable long id) {
        try {
            int rowsAffected = paymentUseCase.updatePayment(payment, id);

            if (rowsAffected == 1) {
                return ResponseEntity.ok("La actualización se ha hecho correctamente");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No se ha podido actualizar.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @DeleteMapping("/superadmin/payment/{id}")
    public ResponseEntity<?> deletePayment(@PathVariable long id) {
        try {
            int rowsAffected = paymentUseCase.deletePayment(id);

            if (rowsAffected == 1) {
                return ResponseEntity.ok("El pago con id: " + id + " se ha eliminado correctamente");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No se ha podido eliminar.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error de servicio.");
        }
    }
}