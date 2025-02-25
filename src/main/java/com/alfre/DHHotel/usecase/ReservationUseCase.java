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
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class ReservationUseCase {
    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;
    private final RoomRepository roomRepository;
    private final ClientRepository clientRepository;
    private static final Logger logger = LoggerFactory.getLogger(ReservationUseCase.class);

    public List<Reservation> getAllReservations() {
        return reservationRepository.getAllReservations();
    }

    public Optional<Reservation> getReservationById(long id) {
        return reservationRepository.getReservationById(id);
    }

    public List<Reservation> getReservationsByClient(User user) {
        Client client = clientRepository.getClientByUserId(user.id)
                .orElseThrow(() -> new RuntimeException("El cliente autenticado no existe"));

        return reservationRepository.getReservationsByClientId(client.id);
    }

    public long createReservation(Reservation newReservation, User user){
        // Verificar disponibilidad en las fechas solicitadas
        if (!reservationRepository.isRoomAvailable(newReservation.room_id, newReservation.start_date,
                newReservation.end_date)) {
            throw new RuntimeException("Habitación no disponible en las fechas solicitadas");
        }

        // Calcular precio total
        BigDecimal totalPrice = calculateTotal(newReservation.start_date, newReservation.end_date, newReservation.room_id);

        if (totalPrice.compareTo(BigDecimal.valueOf(-1.0)) == 0) {
            if (newReservation.start_date.equals(newReservation.end_date)) {
                throw new RuntimeException("La fecha de entrada y salida no pueden ser el mismo día. Debe haber al" +
                        " menos una noche de estancia.");
            } else if (newReservation.end_date.before(newReservation.start_date)) {
                throw new RuntimeException("La fecha de salida no puede ser anterior a la fecha de entrada.");
            } else {
                throw new RuntimeException("Error al calcular el precio total de la reserva. Verifica las fechas y el" +
                        " precio de la habitación o si la habitación realmente existe.");
            }
        }

        newReservation.setTotal_price(totalPrice);

        // Comprobar que la habitación no esté en mantenimiento
        Room room = roomRepository.getRoomById(newReservation.room_id)
                .orElseThrow(() -> new RuntimeException("Error interno."));

        if(room.status == RoomStatus.MAINTENANCE){
            throw new RuntimeException("No se puede reservar una habitación en mantenimiento.");
        }

        // Crear reserva
        if(user.role == Role.CLIENT){
            Client client = clientRepository.getClientByUserId(user.id)
                    .orElseThrow(() -> new RuntimeException("Cliente no encontrado en la base de datos."));

            newReservation.setClient_id(client.id);
        }

        // Una reserva cuando se crea siempre tiene el estado pendiente de confirmación
        newReservation.setStatus(ReservationStatus.PENDING);

        return reservationRepository.createReservation(newReservation);
    }

    public BigDecimal calculateTotal(Date startDate, Date endDate, long roomId) {
        // Validación de fechas null
        if (startDate == null || endDate == null) {
            return BigDecimal.valueOf(-1.0);
        }

        // Convertimos las fechas a LocalDate para un manejo más preciso
        LocalDate checkIn = startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate checkOut = endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

        // Para una reserva de hotel válida:
        // - La fecha de salida debe ser posterior a la fecha de entrada
        // - Debe haber al menos una noche de estancia
        if (checkOut.isBefore(checkIn) || checkIn.equals(checkOut)) {
            return BigDecimal.valueOf(-1.0);
        }

        // Obtenemos el precio por noche
        BigDecimal pricePerNight = roomRepository.getRoomById(roomId)
                .map(room -> room.price_per_night)
                .orElse(BigDecimal.ZERO);

        if (pricePerNight.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.valueOf(-1.0);
        }

        // Calculamos las noches de estancia
        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);

        // Calculamos el precio total
        return pricePerNight.multiply(BigDecimal.valueOf(nights));
    }

    public int updateReservation(long id, Reservation updatedReservation, User user) {
        // Extraer la reserva o lanzar excepción si no existe
        Reservation reservation = reservationRepository.getReservationById(id)
                .orElseThrow(() -> new IllegalArgumentException("La reserva no existe"));

        // Validar autorización
        if (user.role == Role.CLIENT) {
            Client client = clientRepository.getClientByUserId(user.id)
                    .orElseThrow(() -> new RuntimeException("Cliente no encontrado para validar la autorización"));

            if (reservation.client_id != client.id) {
                throw new AccessDeniedException("No autorizado");
            }
        }

        // Validar estado y rol del usuario
        if (reservation.status != ReservationStatus.PENDING && user.role == Role.CLIENT) {
            throw new RuntimeException("No se pueden modificar reservas canceladas o confirmadas si eres cliente.");
        }

        // Actualizar habitación asociada a la reserva
        if (updatedReservation.room_id != null) {
            reservation.setRoom_id(updatedReservation.room_id);
        }

        // Actualizar fechas
        if (updatedReservation.start_date == null || updatedReservation.end_date == null) {
            throw new RuntimeException("Hay que indicar las fechas de inicio y salida de la reserva");
        }

        reservation.setStart_date(updatedReservation.start_date);
        reservation.setEnd_date(updatedReservation.end_date);

        // Calcular el nuevo precio total con los valores actualizados
        BigDecimal totalPrice = calculateTotal(
                reservation.start_date,
                reservation.end_date,
                reservation.room_id
        );

        // Validar que el precio es correcto
        if (totalPrice.compareTo(BigDecimal.valueOf(-1.0)) == 0) {
            throw new RuntimeException("Fechas de inicio y salida de la reserva erróneas.");
        }

        // Asignar el precio a reservation
        reservation.setTotal_price(totalPrice);

        // Guardar cambios en la base de datos
        int updatedRows = reservationRepository.updateReservation(reservation);

        // Verificar si la actualización fue exitosa
        logger.debug("Filas actualizadas en la BD: {}", updatedRows);

        return updatedRows;
    }

    public int cancelReservation(long reservationId) {
        Reservation reservation = reservationRepository.getReservationById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("La reserva no existe"));

        // Validar estado
        if (reservation.status == ReservationStatus.CONFIRMED) {
            throw new RuntimeException("No se puede cancelar una reserva confirmada.");
        }

        reservation.setStatus(ReservationStatus.CANCELED);
        return reservationRepository.updateReservation(reservation);
    }

    @Transactional
    public long createPayment(Payment newPayment, long reservationId, User user) {
        Reservation reservation = reservationRepository.getReservationById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("La reserva no existe"));

        // Validar autorización
        if (user.role == Role.CLIENT && reservation.client_id != user.id) {
            throw new AccessDeniedException("No autorizado");
        }

        // Validar vigencia de la reserva
        if (reservation.status == ReservationStatus.CANCELED) {
            throw new RuntimeException("No se pueden registrar pagos para reservas canceladas");
        }

        // Validar monto
        BigDecimal paidTotal = paymentRepository.getTotalPaid(reservationId);
        BigDecimal remaining = reservation.total_price.subtract(paidTotal);

        if(newPayment.amount.compareTo(remaining) > 0 && reservation.status == ReservationStatus.CONFIRMED) {
            throw new RuntimeException("El pago no se ha realizado ya que la reserva ya está pagada y confirmada");
        } else if (newPayment.amount.compareTo(remaining) > 0) {
            throw new RuntimeException("El pago excede el monto pendiente");
        }

        // Registrar pago
        newPayment.setPayment_date(new Date());
        newPayment.setReservation_id(reservationId);
        long savedPaymentId = paymentRepository.createPayment(newPayment);

        // Actualizar estado de reserva si está pagada
        BigDecimal newPaidTotal = paidTotal.add(newPayment.amount);

        if (newPaidTotal.compareTo(reservation.total_price) >= 0) {
            reservation.setStatus(ReservationStatus.CONFIRMED);
            reservationRepository.updateReservation(reservation);
        }

        return savedPaymentId;
    }

    public List<Payment> getPaymentsByClient(long clientId) {
        return paymentRepository.getPaymentsByClient(clientId);
    }
}