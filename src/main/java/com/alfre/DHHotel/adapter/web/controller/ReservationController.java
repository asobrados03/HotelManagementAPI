package com.alfre.DHHotel.adapter.web.controller;

import com.alfre.DHHotel.domain.model.Payment;
import com.alfre.DHHotel.domain.model.Reservation;
import com.alfre.DHHotel.domain.model.User;
import com.alfre.DHHotel.usecase.ReservationUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static java.util.Collections.emptyList;

/**
 * This class handles HTTP requests for managing reservation resources.
 * It delegates business logic to the ReservationUseCase.
 *
 * @author Alfredo Sobrados González
 */
@RestController
@RequestMapping("/api")
public class ReservationController {
    private final ReservationUseCase reservationUseCase;

    /**
     * Constructs a ReservationController with the provided ReservationUseCase.
     *
     * @param reservationUseCase the business logic component for managing reservations
     */
    public ReservationController(ReservationUseCase reservationUseCase) {
        this.reservationUseCase = reservationUseCase;
    }

    /**
     * Retrieves all reservations in the system.
     * <p>
     * If no reservations are found, a RuntimeException is thrown and a 404 Not Found response is returned.
     * </p>
     *
     * @return a ResponseEntity containing a list of Reservation objects, or an error message if none are found
     */
    @GetMapping("/admin/reservations")
    public ResponseEntity<?> getAllReservations() {
        try {
            List<Reservation> response = reservationUseCase.getAllReservations();
            if (response.equals(emptyList())) {
                throw new RuntimeException("No hay reservas registradas en el sistema.");
            } else {
                return ResponseEntity.ok(response);
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Retrieves a reservation by its unique identifier.
     *
     * @param id the unique identifier of the reservation
     * @return a ResponseEntity containing the Reservation if found,
     *         or a 404 Not Found response with an error message if not found
     */
    @GetMapping("/admin/reservation/{id}")
    public ResponseEntity<?> getReservationById(@PathVariable int id) {
        try {
            Reservation reservation = reservationUseCase.getReservationById(id)
                    .orElseThrow(() -> new RuntimeException("La reserva solicitada no existe"));
            return ResponseEntity.ok(reservation);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Retrieves all reservations associated with the currently authenticated client.
     *
     * @param user the currently authenticated user (client)
     * @return a ResponseEntity containing a list of Reservation objects for the client,
     *         or a 404 Not Found response with an error message if none are found
     */
    @GetMapping("/client/reservations/my")
    public ResponseEntity<?> getReservationsByClient(@AuthenticationPrincipal User user) {
        try {
            List<Reservation> response = reservationUseCase.getReservationsByClient(user);
            if (response.equals(emptyList())) {
                throw new RuntimeException("No hay reservas asociadas al cliente registradas en el sistema.");
            } else {
                return ResponseEntity.ok(response);
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Creates a new reservation for the authenticated client.
     *
     * @param newReservation the Reservation object containing the reservation details
     * @param user the currently authenticated user (client)
     * @return a ResponseEntity containing the identifier of the created reservation,
     *         or a 400 Bad Request response with an error message if creation fails
     */
    @PostMapping("/client/reservation")
    public ResponseEntity<?> createReservation(@RequestBody Reservation newReservation,
                                               @AuthenticationPrincipal User user) {
        try {
            return ResponseEntity.ok(reservationUseCase.createReservation(newReservation, user));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * Updates an existing reservation.
     * <p>
     * The reservation identified by the provided id is updated with the details in updatedReservation.
     * If the authenticated user is not authorized to update the reservation, a 403 Forbidden response is returned.
     * </p>
     *
     * @param id the unique identifier of the reservation to update
     * @param updatedReservation the Reservation object containing the updated information
     * @param user the currently authenticated user (used for authorization)
     * @return a ResponseEntity with a success message if the update is successful,
     *         a 403 Forbidden response if access is denied, or a 400/500 response with an error message otherwise
     */
    @PutMapping("/reservation/{id}")
    public ResponseEntity<?> updateReservation(@PathVariable long id, @RequestBody Reservation updatedReservation,
                                               @AuthenticationPrincipal User user) {
        try {
            int rowsAffected = reservationUseCase.updateReservation(id, updatedReservation, user);
            if (rowsAffected == 1) {
                return ResponseEntity.ok("La actualización se ha hecho correctamente");
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("No se ha podido actualizar.");
            }
        } catch (AccessDeniedException ade) {
            // Specific handling for access denied errors
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No autorizado");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * Cancels a reservation identified by its unique identifier.
     *
     * @param id the unique identifier of the reservation to cancel
     * @return a ResponseEntity with a success message if cancellation is successful,
     *         or a 400 Bad Request response with an error message if cancellation fails
     */
    @DeleteMapping("/admin/reservation/{id}")
    public ResponseEntity<?> cancelReservation(@PathVariable long id){
        try {
            int rowsAffected = reservationUseCase.cancelReservation(id);
            if (rowsAffected == 1) {
                return ResponseEntity.ok("La cancelación se ha hecho correctamente");
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("No se ha podido actualizar.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * Creates a payment for a specific reservation.
     *
     * @param newPayment the Payment object containing the payment details
     * @param id the unique identifier of the reservation for which the payment is made
     * @param user the currently authenticated user
     * @return a ResponseEntity containing the identifier of the created payment,
     *         or a 400 Bad Request response with an error message if creation fails
     */
    @PostMapping("/admin/reservation/{id}/payment")
    public ResponseEntity<?> createPaymentOfReservation(@RequestBody Payment newPayment, @PathVariable long id,
                                                        @AuthenticationPrincipal User user) {
        try {
            return ResponseEntity.ok(reservationUseCase.createPayment(newPayment, id, user));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * Retrieves all payments associated with a specific client reservation.
     *
     * @param id the unique identifier of the client (or reservation) for which to retrieve payments
     * @return a ResponseEntity containing a list of Payment objects if found,
     *         or a 404 Not Found response with an error message if no payments are found
     */
    @GetMapping("/admin/reservations/{id}/payments")
    public ResponseEntity<?> getPaymentsByClient(@PathVariable long id) {
        try {
            List<Payment> response = reservationUseCase.getPaymentsByClient(id);
            if (response.equals(emptyList())) {
                throw new RuntimeException("No hay pagos asociados al cliente registrados en el sistema.");
            } else {
                return ResponseEntity.ok(response);
            }
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}