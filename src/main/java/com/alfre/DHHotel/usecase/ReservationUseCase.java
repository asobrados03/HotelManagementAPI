package com.alfre.DHHotel.usecase;

import com.alfre.DHHotel.domain.model.*;
import com.alfre.DHHotel.domain.repository.ClientRepository;
import com.alfre.DHHotel.domain.repository.PaymentRepository;
import com.alfre.DHHotel.domain.repository.ReservationRepository;
import com.alfre.DHHotel.domain.repository.RoomRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Service class handling reservation-related business logic.
 * <p>
 * This class provides methods for creating, updating, canceling, and retrieving reservations,
 * as well as managing payments associated with reservations.
 * </p>
 *
 * @author Alfredo Sobrados González
 */
@Service
@AllArgsConstructor
public class ReservationUseCase {
    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;
    private final RoomRepository roomRepository;
    private final ClientRepository clientRepository;
    private static final Logger logger = LoggerFactory.getLogger(ReservationUseCase.class);

    /**
     * Retrieves all reservations.
     *
     * @return A list of all reservations.
     */
    public List<Reservation> getAllReservations() {
        return reservationRepository.getAllReservations();
    }

    /**
     * Retrieves a reservation by its ID.
     *
     * @param id The reservation ID.
     * @return An {@link Optional} containing the reservation if found.
     */
    public Optional<Reservation> getReservationById(long id) {
        return reservationRepository.getReservationById(id);
    }

    /**
     * Retrieves all reservations associated with a client.
     *
     * @param user The authenticated user.
     * @return A list of reservations for the client.
     */
    public List<Reservation> getReservationsByClient(User user) {
        Client client = clientRepository.getClientByUserId(user.id)
                .orElseThrow(() -> new RuntimeException("El cliente autenticado no existe"));
        return reservationRepository.getReservationsByClientId(client.id);
    }

    /**
     * Creates a new reservation for a given user.
     *
     * @param newReservation The reservation details.
     * @param user The authenticated user making the reservation.
     * @return The generated reservation ID.
     * @throws RuntimeException If the room is unavailable, in maintenance, or has invalid dates.
     */
    @Transactional
    public long createReservation(Reservation newReservation, User user) {
        if (!reservationRepository.isRoomAvailable(newReservation.room_id, newReservation.start_date,
                newReservation.end_date)) {
            throw new RuntimeException("Habitación no disponible en las fechas solicitadas");
        }

        BigDecimal totalPrice = calculateTotal(newReservation.start_date, newReservation.end_date, newReservation.room_id);

        if (totalPrice.compareTo(BigDecimal.valueOf(-1.0)) == 0) {
            if (newReservation.start_date.equals(newReservation.end_date)) {
                throw new RuntimeException("La fecha de entrada y salida no pueden ser el mismo día. Debe haber al" +
                        " menos una noche de estancia.");
            } else if (newReservation.end_date.isBefore(newReservation.start_date)) {
                throw new RuntimeException("La fecha de salida no puede ser anterior a la fecha de entrada.");
            } else {
                throw new RuntimeException("Error al calcular el precio total de la reserva. Verifica las fechas y el" +
                        " precio de la habitación o si la habitación realmente existe.");
            }
        }

        newReservation.setTotal_price(totalPrice);

        Room room = roomRepository.getRoomById(newReservation.room_id)
                .orElseThrow(() -> new RuntimeException("Error interno."));
        if (room.status == RoomStatus.MAINTENANCE) {
            throw new RuntimeException("No se puede reservar una habitación en mantenimiento.");
        }

        if (user.role == Role.CLIENT) {
            Client client = clientRepository.getClientByUserId(user.id)
                    .orElseThrow(() -> new RuntimeException("Cliente no encontrado en la base de datos."));
            newReservation.setClient_id(client.id);
        }

        newReservation.setStatus(ReservationStatus.PENDING);
        return reservationRepository.createReservation(newReservation);
    }

    /**
     * Calculates the total price for a reservation based on the stay duration and room price.
     *
     * @param startDate The check-in date.
     * @param endDate The check-out date.
     * @param roomId The ID of the room.
     * @return The total reservation price.
     */
    public BigDecimal calculateTotal(LocalDate startDate, LocalDate endDate, long roomId) {
        if (startDate == null || endDate == null) {
            return BigDecimal.valueOf(-1.0);
        }

        if (endDate.isBefore(startDate) || startDate.equals(endDate)) {
            return BigDecimal.valueOf(-1.0);
        }

        BigDecimal pricePerNight = roomRepository.getRoomById(roomId)
                .map(room -> room.price_per_night)
                .orElse(BigDecimal.ZERO);

        if (pricePerNight.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.valueOf(-1.0);
        }

        long nights = ChronoUnit.DAYS.between(startDate, endDate);
        return pricePerNight.multiply(BigDecimal.valueOf(nights));
    }

    /**
     * Updates an existing reservation.
     *
     * @param id The reservation ID.
     * @param updatedReservation The updated reservation details.
     * @param user The authenticated user.
     * @return The number of rows affected.
     * @throws RuntimeException If the reservation cannot be modified.
     */
    public int updateReservation(long id, Reservation updatedReservation, User user) {
        Reservation reservation = reservationRepository.getReservationById(id)
                .orElseThrow(() -> new IllegalArgumentException("La reserva no existe"));

        if (user.role == Role.CLIENT) {
            Client client = clientRepository.getClientByUserId(user.id)
                    .orElseThrow(() -> new RuntimeException("Cliente no encontrado para validar la autorización"));

            if (reservation.client_id != client.id) {
                throw new AccessDeniedException("No autorizado");
            }
        }

        if (reservation.status != ReservationStatus.PENDING && user.role == Role.CLIENT) {
            throw new RuntimeException("No se pueden modificar reservas canceladas o confirmadas si eres cliente.");
        }

        if (updatedReservation.start_date == null || updatedReservation.end_date == null) {
            throw new RuntimeException("Hay que indicar las fechas de inicio y salida de la reserva");
        }

        reservation.setRoom_id(updatedReservation.room_id);
        reservation.setStart_date(updatedReservation.start_date);
        reservation.setEnd_date(updatedReservation.end_date);

        BigDecimal totalPrice = calculateTotal(reservation.start_date, reservation.end_date, reservation.room_id);
        if (totalPrice.compareTo(BigDecimal.valueOf(-1.0)) == 0) {
            throw new RuntimeException("Fechas de inicio y salida de la reserva erróneas.");
        }

        reservation.setTotal_price(totalPrice);
        return reservationRepository.updateReservation(reservation);
    }

    /**
     * Cancels a reservation.
     *
     * @param reservationId The ID of the reservation to cancel.
     * @return The number of rows affected.
     * @throws RuntimeException If the reservation is already confirmed.
     */
    public int cancelReservation(long reservationId) {
        Reservation reservation = reservationRepository.getReservationById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("La reserva no existe"));

        if (reservation.status == ReservationStatus.CONFIRMED) {
            throw new RuntimeException("No se puede cancelar una reserva confirmada.");
        }

        reservation.setStatus(ReservationStatus.CANCELED);
        return reservationRepository.updateReservation(reservation);
    }

    /**
     * Registers a payment for a reservation.
     *
     * @param newPayment The payment details.
     * @param reservationId The reservation ID.
     * @param user The authenticated user.
     * @return The generated payment ID.
     * @throws RuntimeException If the payment exceeds the remaining balance or if the reservation is canceled.
     */
    @Transactional
    public long createPayment(Payment newPayment, long reservationId, User user) {
        Reservation reservation = reservationRepository.getReservationById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("La reserva no existe"));

        if (user.role == Role.CLIENT && reservation.client_id != user.id) {
            throw new AccessDeniedException("No autorizado");
        }

        if (reservation.status == ReservationStatus.CANCELED) {
            throw new RuntimeException("No se pueden registrar pagos para reservas canceladas");
        }

        BigDecimal paidTotal = paymentRepository.getTotalPaid(reservationId);
        BigDecimal remaining = reservation.total_price.subtract(paidTotal);

        if (newPayment.amount.compareTo(remaining) > 0 && reservation.status == ReservationStatus.CONFIRMED) {
            throw new RuntimeException("El pago no se ha realizado ya que la reserva ya está pagada y confirmada");
        } else if (newPayment.amount.compareTo(remaining) > 0) {
            throw new RuntimeException("El pago excede el monto pendiente");
        }

        newPayment.setReservation_id(reservationId);
        long savedPaymentId = paymentRepository.createPayment(newPayment);

        BigDecimal newPaidTotal = paidTotal.add(newPayment.amount);
        if (newPaidTotal.compareTo(reservation.total_price) >= 0) {
            reservation.setStatus(ReservationStatus.CONFIRMED);
            reservationRepository.updateReservation(reservation);
        }

        return savedPaymentId;
    }

    /**
     * Retrieves all payments associated with a client.
     *
     * @param clientId The client ID.
     * @return A list of payments for the client.
     */
    public List<Payment> getPaymentsByClient(long clientId) {
        return paymentRepository.getPaymentsByClient(clientId);
    }
}