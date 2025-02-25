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

@RestController
@RequestMapping("/api")
public class ReservationController {
    private final ReservationUseCase reservationUseCase;

    public ReservationController(ReservationUseCase reservationUseCase) {
        this.reservationUseCase = reservationUseCase;
    }

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

    @PostMapping("/client/reservation")
    public ResponseEntity<?> createReservation(@RequestBody Reservation newReservation,
                                                  @AuthenticationPrincipal User user) {
        try {
            return ResponseEntity.ok(reservationUseCase.createReservation(newReservation, user));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

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
            // Capturamos específicamente el error de acceso denegado y devolvemos 403.
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("No autorizado");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

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

    @PostMapping("/admin/reservation/{id}/payment")
    public ResponseEntity<?> createPaymentOfReservation(@RequestBody Payment newPayment, @PathVariable long id,
                                           @AuthenticationPrincipal User user) {
        try {
            return ResponseEntity.ok(reservationUseCase.createPayment(newPayment, id, user));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

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