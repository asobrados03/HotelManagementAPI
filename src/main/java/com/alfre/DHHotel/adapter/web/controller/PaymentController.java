package com.alfre.DHHotel.adapter.web.controller;

import com.alfre.DHHotel.domain.model.Payment;
import com.alfre.DHHotel.usecase.PaymentUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static java.util.Collections.emptyList;

/**
 * This class handles HTTP requests for managing payment resources.
 * It delegates business logic to the PaymentUseCase.
 *
 * @author Alfredo Sobrados González
 */
@RestController
@RequestMapping("/api")
public class PaymentController {
    private final PaymentUseCase paymentUseCase;

    /**
     * Constructs a PaymentController with the specified PaymentUseCase.
     *
     * @param paymentUseCase the use case containing business logic for payment operations
     */
    public PaymentController(PaymentUseCase paymentUseCase) {
        this.paymentUseCase = paymentUseCase;
    }

    /**
     * Retrieves all payment records.
     * <p>
     * If no payments are found, a RuntimeException is thrown and a 404 Not Found response is returned.
     * </p>
     *
     * @return a ResponseEntity containing the list of payments if found, or an error message otherwise
     */
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

    /**
     * Retrieves a payment by its unique identifier.
     *
     * @param id the unique identifier of the payment
     * @return a ResponseEntity containing the Payment if found, or a 404 Not Found response with an error message if not
     */
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

    /**
     * Retrieves all payments associated with a specific reservation.
     *
     * @param reservationId the identifier of the reservation
     * @return a ResponseEntity containing the list of Payment objects if found,
     *         or a 404 Not Found response with an error message if none are found
     */
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

    /**
     * Updates an existing payment record.
     *
     * @param payment the Payment object containing updated information
     * @param id the unique identifier of the payment to update
     * @return a ResponseEntity with a success message if the update is successful,
     *         or an error response if the update fails
     */
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

    /**
     * Deletes a payment record by its unique identifier.
     *
     * @param id the unique identifier of the payment to delete
     * @return a ResponseEntity with a success message if deletion is successful,
     *         or an error response if deletion fails
     */
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