package com.alfre.DHHotel.usecase;

import com.alfre.DHHotel.domain.model.*;
import com.alfre.DHHotel.domain.repository.ClientRepository;
import com.alfre.DHHotel.domain.repository.PaymentRepository;
import com.alfre.DHHotel.domain.repository.ReservationRepository;
import com.alfre.DHHotel.domain.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReservationUseCaseTest {
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private ClientRepository clientRepository;

    private ReservationUseCase reservationUseCase;

    @BeforeEach
    public void setup() {
        reservationUseCase = spy(new ReservationUseCase(reservationRepository, paymentRepository, roomRepository,
                clientRepository));
    }

    @Test
    public void getAllReservations_success() {
        // Arrange: Preparamos una lista de reservas
        List<Reservation> reservationList = new ArrayList<>();
        Reservation reservation = new Reservation();
        // Configuramos las propiedades de reservation si es necesario
        reservationList.add(reservation);
        when(reservationRepository.getAllReservations()).thenReturn(reservationList);

        // Act: Ejecutamos el método
        List<Reservation> result = reservationUseCase.getAllReservations();

        // Assert: Verificamos que el resultado sea el esperado y se llamó al método del repository
        assertEquals(reservationList, result);
        verify(reservationRepository).getAllReservations();
    }

    @Test
    public void getReservationById_success() {
        // Arrange: Creamos una reserva y simulamos su retorno para el identificador dado
        Reservation reservation = new Reservation();
        when(reservationRepository.getReservationById(10L)).thenReturn(Optional.of(reservation));

        // Act
        Optional<Reservation> result = reservationUseCase.getReservationById(10L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(reservation, result.get());
        verify(reservationRepository).getReservationById(10L);
    }

    @Test
    public void getReservationsByClient_success() {
        // Arrange: Creamos el usuario asociado al cliente, el cliente y la lista de reservas
        Reservation reservation = new Reservation();
        List<Reservation> reservationList = new ArrayList<>();
        reservationList.add(reservation);

        Client client = new Client();
        client.setId(4L);

        User user = new User();
        user.setId(5L);

        when(clientRepository.getClientByUserId(user.id)).thenReturn(Optional.of(client));
        when(reservationRepository.getReservationsByClientId(client.id)).thenReturn(reservationList);

        // Act
        List<Reservation> result = reservationUseCase.getReservationsByClient(user);

        // Assert
        assertEquals(result, reservationList);
        verify(clientRepository).getClientByUserId(user.id);
        verify(reservationRepository).getReservationsByClientId(client.id);
    }

    @Test
    public void getReservationsByClient_failure_shouldThrowException() {
        // Arrange: Creamos el usuario asociado al cliente
        User user = new User();
        user.setId(5L);

        when(clientRepository.getClientByUserId(user.id)).thenReturn(Optional.empty());

        // Act
        Exception exception = assertThrows(RuntimeException.class, () ->
                reservationUseCase.getReservationsByClient(user)
        );

        // Assert
        assertEquals("El cliente autenticado no existe", exception.getMessage());
        verify(clientRepository).getClientByUserId(user.id);
    }

    @Test
    public void createReservation_success_byClient() {
        // Arrange
        Reservation newReservation = new Reservation();
        newReservation.setRoom_id(2L);

        User user = new User();
        user.setId(6L);
        user.setRole(Role.CLIENT);
        Room room = new Room();
        Client client = new Client();

        when(reservationRepository.isRoomAvailable(newReservation.room_id, newReservation.start_date,
                newReservation.end_date)).thenReturn(true);
        when(reservationUseCase.calculateTotal(newReservation.start_date, newReservation.end_date,
                newReservation.room_id)).thenReturn(BigDecimal.ONE);
        when(roomRepository.getRoomById(newReservation.room_id)).thenReturn(Optional.of(room));
        when(clientRepository.getClientByUserId(user.id)).thenReturn(Optional.of(client));
        when(reservationRepository.createReservation(newReservation)).thenReturn(5L);

        // Act
        long keyValue = reservationUseCase.createReservation(newReservation, user);

        // Assert
        assertEquals(5L, keyValue);

        verify(reservationRepository).isRoomAvailable(newReservation.room_id, newReservation.start_date,
                newReservation.end_date);
        verify(reservationRepository).createReservation(newReservation);
        verify(reservationUseCase).calculateTotal(newReservation.start_date, newReservation.end_date,
                newReservation.room_id);
        verify(roomRepository).getRoomById(newReservation.room_id);
        verify(clientRepository).getClientByUserId(user.id);
    }

    @Test
    public void createReservation_success_byAdmin() {
        // Arrange
        Reservation newReservation = new Reservation();
        newReservation.setRoom_id(2L);

        User user = new User();
        user.setRole(Role.ADMIN);
        Room room = new Room();

        when(reservationRepository.isRoomAvailable(newReservation.room_id, newReservation.start_date,
                newReservation.end_date)).thenReturn(true);
        when(reservationUseCase.calculateTotal(newReservation.start_date, newReservation.end_date,
                newReservation.room_id)).thenReturn(BigDecimal.ONE);
        when(roomRepository.getRoomById(newReservation.room_id)).thenReturn(Optional.of(room));
        when(reservationRepository.createReservation(newReservation)).thenReturn(5L);

        // Act
        long keyValue = reservationUseCase.createReservation(newReservation, user);

        // Assert
        assertEquals(5L, keyValue);

        verify(reservationRepository).isRoomAvailable(newReservation.room_id, newReservation.start_date,
                newReservation.end_date);
        verify(reservationRepository).createReservation(newReservation);
        verify(roomRepository).getRoomById(newReservation.room_id);
    }

    @Test
    public void createReservation_roomIsNotAvailable_shouldThrowException() {
        // Arrange
        Reservation newReservation = new Reservation();
        User user = new User();

        when(reservationRepository.isRoomAvailable(newReservation.room_id, newReservation.start_date,
                newReservation.end_date)).thenReturn(false);

        // Act
        Exception exception = assertThrows(RuntimeException.class, () ->
            reservationUseCase.createReservation(newReservation, user)
        );

        // Assert
        assertNotNull(exception);
        assertEquals("Habitación no disponible en las fechas solicitadas", exception.getMessage());
    }

    @Test
    public void createReservation_totalPriceMinusOne_sameDay_shouldThrowException() {
        // Arrange
        Reservation newReservation = new Reservation();
        newReservation.setRoom_id(2L);
        // Para simular que la reserva se solicita para el mismo día:
        Date sameDay = new Date(1000L);
        newReservation.setStart_date(sameDay);
        newReservation.setEnd_date(sameDay);

        User user = new User();
        user.setRole(Role.ADMIN); // El rol no afecta esta validación

        when(reservationRepository.isRoomAvailable(newReservation.room_id, newReservation.start_date,
                newReservation.end_date)).thenReturn(true);
        // Stub para que calculateTotal devuelva -1.0
        when(reservationUseCase.calculateTotal(newReservation.start_date, newReservation.end_date,
                newReservation.room_id)).thenReturn(BigDecimal.valueOf(-1.0));

        // Act & Assert
        Exception ex = assertThrows(RuntimeException.class, () ->
                reservationUseCase.createReservation(newReservation, user));
        assertEquals("La fecha de entrada y salida no pueden ser el mismo día. Debe haber al menos una noche de estancia.", ex.getMessage());
    }

    @Test
    public void createReservation_totalPriceMinusOne_endDateBeforeStartDate_shouldThrowException() {
        // Arrange
        Reservation newReservation = new Reservation();
        newReservation.setRoom_id(2L);
        // Configuramos las fechas: la fecha de salida anterior a la de entrada
        Date startDate = new Date(2000L);
        Date endDate = new Date(1000L);
        newReservation.setStart_date(startDate);
        newReservation.setEnd_date(endDate);

        User user = new User();
        user.setRole(Role.ADMIN);

        when(reservationRepository.isRoomAvailable(newReservation.room_id, newReservation.start_date,
                newReservation.end_date)).thenReturn(true);
        when(reservationUseCase.calculateTotal(newReservation.start_date, newReservation.end_date,
                newReservation.room_id)).thenReturn(BigDecimal.valueOf(-1.0));

        // Act & Assert
        Exception ex = assertThrows(RuntimeException.class, () ->
                reservationUseCase.createReservation(newReservation, user));
        assertEquals("La fecha de salida no puede ser anterior a la fecha de entrada.", ex.getMessage());
    }

    @Test
    public void createReservation_totalPriceMinusOne_otherCondition_shouldThrowException() {
        // Arrange
        Reservation newReservation = new Reservation();
        newReservation.setRoom_id(2L);
        // Configuramos fechas válidas (diferentes y con end_date posterior a start_date)
        Date startDate = new Date(1000L);
        Date endDate = new Date(2000L);
        newReservation.setStart_date(startDate);
        newReservation.setEnd_date(endDate);

        User user = new User();
        user.setRole(Role.ADMIN);

        when(reservationRepository.isRoomAvailable(newReservation.room_id, newReservation.start_date,
                newReservation.end_date)).thenReturn(true);
        // Stub para que calculateTotal devuelva -1.0
        when(reservationUseCase.calculateTotal(newReservation.start_date, newReservation.end_date, newReservation.room_id))
                .thenReturn(BigDecimal.valueOf(-1.0));

        // Act & Assert
        Exception ex = assertThrows(RuntimeException.class, () ->
                reservationUseCase.createReservation(newReservation, user));
        assertEquals("Error al calcular el precio total de la reserva. Verifica las fechas y el precio de la " +
                "habitación o si la habitación realmente existe.", ex.getMessage());
    }

    @Test
    public void createReservation_roomInMaintenance_shouldThrowException() {
        // Arrange
        Reservation newReservation = new Reservation();
        newReservation.setRoom_id(2L);
        // Configuramos fechas válidas
        Date startDate = new Date(1000L);
        Date endDate = new Date(2000L);
        newReservation.setStart_date(startDate);
        newReservation.setEnd_date(endDate);

        User user = new User();
        user.setRole(Role.ADMIN);

        when(reservationRepository.isRoomAvailable(newReservation.room_id, newReservation.start_date,
                newReservation.end_date)).thenReturn(true);
        // Stub para un total válido (por ejemplo, 10)
        when(reservationUseCase.calculateTotal(newReservation.start_date, newReservation.end_date,
                newReservation.room_id)).thenReturn(BigDecimal.TEN);
        // Simulamos que la habitación está en mantenimiento
        Room room = new Room();
        room.status = RoomStatus.MAINTENANCE;
        when(roomRepository.getRoomById(newReservation.room_id)).thenReturn(Optional.of(room));

        // Act & Assert
        Exception ex = assertThrows(RuntimeException.class, () ->
                reservationUseCase.createReservation(newReservation, user));
        assertEquals("No se puede reservar una habitación en mantenimiento.", ex.getMessage());
    }

    @Test
    public void createReservation_roomNotFound_shouldThrowException() {
        // Arrange
        Reservation newReservation = new Reservation();
        newReservation.setRoom_id(2L);
        Date startDate = new Date(1000L);
        Date endDate = new Date(2000L);
        newReservation.setStart_date(startDate);
        newReservation.setEnd_date(endDate);

        User user = new User();

        when(reservationRepository.isRoomAvailable(newReservation.room_id, newReservation.start_date,
                newReservation.end_date)).thenReturn(true);
        // Stub para calcular un total válido (por ejemplo, 10)
        when(reservationUseCase.calculateTotal(newReservation.start_date, newReservation.end_date,
                newReservation.room_id)).thenReturn(BigDecimal.TEN);
        // Simulamos que no se encuentra la habitación
        when(roomRepository.getRoomById(newReservation.room_id)).thenReturn(Optional.empty());

        // Act & Assert
        Exception ex = assertThrows(RuntimeException.class, () ->
                reservationUseCase.createReservation(newReservation, user));
        assertEquals("Error interno.", ex.getMessage());
        verify(roomRepository).getRoomById(newReservation.room_id);
    }

    @Test
    public void createReservation_clientNotFound_shouldThrowException() {
        // Arrange
        Reservation newReservation = new Reservation();
        newReservation.setRoom_id(2L);
        Date startDate = new Date(1000L);
        Date endDate = new Date(2000L);
        newReservation.setStart_date(startDate);
        newReservation.setEnd_date(endDate);

        User user = new User();
        user.setId(6L);
        user.setRole(Role.CLIENT);

        when(reservationRepository.isRoomAvailable(newReservation.room_id, newReservation.start_date,
                newReservation.end_date)).thenReturn(true);
        when(reservationUseCase.calculateTotal(newReservation.start_date, newReservation.end_date,
                newReservation.room_id)).thenReturn(BigDecimal.TEN);

        // Simulamos que se encuentra una habitación válida (no en mantenimiento)
        Room room = new Room();
        room.status = RoomStatus.AVAILABLE;
        when(roomRepository.getRoomById(newReservation.room_id)).thenReturn(Optional.of(room));

        // Simulamos que no se encuentra el cliente para el usuario
        when(clientRepository.getClientByUserId(user.id)).thenReturn(Optional.empty());

        // Act & Assert
        Exception ex = assertThrows(RuntimeException.class, () ->
                reservationUseCase.createReservation(newReservation, user));
        assertEquals("Cliente no encontrado en la base de datos.", ex.getMessage());
        verify(clientRepository).getClientByUserId(user.id);
    }

    @Test
    public void calculateTotal_startDateNull_returnsMinusOne() {
        // Arrange
        Date endDate = new Date();
        long roomId = 1L;

        // Act & Assert
        BigDecimal result = reservationUseCase.calculateTotal(null, endDate, roomId);
        assertEquals(BigDecimal.valueOf(-1.0), result);
    }

    @Test
    public void calculateTotal_endDateNull_returnsMinusOne() {
        // Arrange
        Date startDate = new Date();
        long roomId = 1L;

        // Act & Assert
        BigDecimal result = reservationUseCase.calculateTotal(startDate, null, roomId);
        assertEquals(BigDecimal.valueOf(-1.0), result);
    }

    @Test
    public void calculateTotal_endDateBeforeStartDate_returnsMinusOne() {
        // Arrange
        // Configuramos startDate como hoy y endDate como ayer
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        Date startDate = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(yesterday.atStartOfDay(ZoneId.systemDefault()).toInstant());
        long roomId = 1L;

        // Act & Assert
        BigDecimal result = reservationUseCase.calculateTotal(startDate, endDate, roomId);
        assertEquals(BigDecimal.valueOf(-1.0), result);
    }

    @Test
    public void calculateTotal_sameDay_returnsMinusOne() {
        // Arrange
        LocalDate today = LocalDate.now();
        Date startDate = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
        long roomId = 1L;

        // Act & Assert
        BigDecimal result = reservationUseCase.calculateTotal(startDate, endDate, roomId);
        assertEquals(BigDecimal.valueOf(-1.0), result);
    }

    @Test
    public void calculateTotal_roomPriceZero_returnsMinusOne() {
        // Arrange
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = checkIn.plusDays(1);
        Date startDate = Date.from(checkIn.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(checkOut.atStartOfDay(ZoneId.systemDefault()).toInstant());
        long roomId = 1L;

        Room room = new Room();
        room.price_per_night = BigDecimal.ZERO; // Precio no positivo
        when(roomRepository.getRoomById(roomId)).thenReturn(Optional.of(room));

        // Act & Assert
        BigDecimal result = reservationUseCase.calculateTotal(startDate, endDate, roomId);
        assertEquals(BigDecimal.valueOf(-1.0), result);
    }

    @Test
    public void calculateTotal_roomNotFound_returnsMinusOne() {
        // Arrange
        LocalDate checkIn = LocalDate.now().plusDays(1);
        LocalDate checkOut = checkIn.plusDays(1);
        Date startDate = Date.from(checkIn.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(checkOut.atStartOfDay(ZoneId.systemDefault()).toInstant());
        long roomId = 1L;

        when(roomRepository.getRoomById(roomId)).thenReturn(Optional.empty());

        // Act & Assert
        BigDecimal result = reservationUseCase.calculateTotal(startDate, endDate, roomId);
        assertEquals(BigDecimal.valueOf(-1.0), result);
    }

    @Test
    public void calculateTotal_oneNight_returnsPricePerNight() {
        // Arrange
        LocalDate checkIn = LocalDate.of(2023, 3, 1);
        LocalDate checkOut = checkIn.plusDays(1); // 1 noche
        Date startDate = Date.from(checkIn.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(checkOut.atStartOfDay(ZoneId.systemDefault()).toInstant());
        long roomId = 1L;

        Room room = new Room();
        room.price_per_night = new BigDecimal("100.00");
        when(roomRepository.getRoomById(roomId)).thenReturn(Optional.of(room));

        // Act & Assert
        BigDecimal result = reservationUseCase.calculateTotal(startDate, endDate, roomId);
        assertEquals(new BigDecimal("100.00"), result);
    }

    @Test
    public void calculateTotal_multipleNights_returnsTotalPrice() {
        // Arrange
        // Ejemplo: checkIn = 2023-03-01, checkOut = 2023-03-05 → 4 noches
        LocalDate checkIn = LocalDate.of(2023, 3, 1);
        LocalDate checkOut = checkIn.plusDays(4);
        Date startDate = Date.from(checkIn.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(checkOut.atStartOfDay(ZoneId.systemDefault()).toInstant());
        long roomId = 1L;

        Room room = new Room();
        room.price_per_night = new BigDecimal("150.00");
        when(roomRepository.getRoomById(roomId)).thenReturn(Optional.of(room));

        // Act & Assert
        BigDecimal result = reservationUseCase.calculateTotal(startDate, endDate, roomId);
        // 4 noches * 150.00 = 600.00
        assertEquals(new BigDecimal("600.00"), result);
    }

    @Test
    public void updateReservation_reservationNotFound_throwsException() {
        // Arrange
        long id = 1L;
        Reservation updatedReservation = new Reservation();
        User user = new User();

        when(reservationRepository.getReservationById(id)).thenReturn(Optional.empty());

        // Act & Assert
        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                reservationUseCase.updateReservation(id, updatedReservation, user)
        );
        assertEquals("La reserva no existe", ex.getMessage());
    }

    @Test
    public void updateReservation_clientNotFound_throwsException() {
        // Arrange
        long id = 1L;
        Reservation reservation = new Reservation();
        reservation.client_id = 10L; // reserva asociada a cliente id 10
        reservation.status = ReservationStatus.PENDING;
        Reservation updatedReservation = new Reservation();
        // Se deben indicar fechas para evitar el chequeo de fechas
        updatedReservation.start_date = new Date(1000L);
        updatedReservation.end_date = new Date(2000L);

        User user = new User();
        user.id = 5L;
        user.role = Role.CLIENT;

        when(reservationRepository.getReservationById(id)).thenReturn(Optional.of(reservation));
        when(clientRepository.getClientByUserId(user.id)).thenReturn(Optional.empty());

        // Act & Assert
        Exception ex = assertThrows(RuntimeException.class, () ->
                reservationUseCase.updateReservation(id, updatedReservation, user)
        );
        assertEquals("Cliente no encontrado para validar la autorización", ex.getMessage());
    }

    @Test
    public void updateReservation_unauthorizedClient_throwsAccessDeniedException() {
        // Arrange
        long id = 1L;
        Reservation reservation = new Reservation();
        reservation.client_id = 10L; // reserva pertenece a cliente 10
        reservation.status = ReservationStatus.PENDING;
        Reservation updatedReservation = new Reservation();
        updatedReservation.start_date = new Date(1000L);
        updatedReservation.end_date = new Date(2000L);

        User user = new User();
        user.id = 5L;
        user.role = Role.CLIENT;
        Client client = new Client();
        client.id = 5L; // cliente obtenido del repositorio

        when(reservationRepository.getReservationById(id)).thenReturn(Optional.of(reservation));
        when(clientRepository.getClientByUserId(user.id)).thenReturn(Optional.of(client));

        // Act & Assert
        Exception ex = assertThrows(AccessDeniedException.class, () ->
                reservationUseCase.updateReservation(id, updatedReservation, user)
        );
        assertEquals("No autorizado", ex.getMessage());
    }

    @Test
    public void updateReservation_clientNonPendingReservation_throwsException() {
        // Arrange
        long id = 1L;
        Reservation reservation = new Reservation();
        reservation.client_id = 10L;
        reservation.status = ReservationStatus.CONFIRMED; // no es PENDING
        Reservation updatedReservation = new Reservation();
        updatedReservation.start_date = new Date(1000L);
        updatedReservation.end_date = new Date(2000L);

        User user = new User();
        user.id = 10L;
        user.role = Role.CLIENT;
        Client client = new Client();
        client.id = 10L;

        when(reservationRepository.getReservationById(id)).thenReturn(Optional.of(reservation));
        when(clientRepository.getClientByUserId(user.id)).thenReturn(Optional.of(client));

        // Act & Assert
        Exception ex = assertThrows(RuntimeException.class, () ->
                reservationUseCase.updateReservation(id, updatedReservation, user)
        );
        assertEquals("No se pueden modificar reservas canceladas o confirmadas si eres cliente.", ex.getMessage());
    }

    @Test
    public void updateReservation_missingDates_throwsException() {
        // Arrange
        long id = 1L;
        Reservation reservation = new Reservation();
        reservation.client_id = 10L;
        reservation.status = ReservationStatus.PENDING;
        Reservation updatedReservation = new Reservation();
        // No se establecen start_date ni end_date

        User user = new User();
        user.id = 10L;
        user.role = Role.CLIENT;
        Client client = new Client();
        client.id = 10L;

        when(reservationRepository.getReservationById(id)).thenReturn(Optional.of(reservation));
        when(clientRepository.getClientByUserId(user.id)).thenReturn(Optional.of(client));

        // Act & Assert
        Exception ex = assertThrows(RuntimeException.class, () ->
                reservationUseCase.updateReservation(id, updatedReservation, user)
        );
        assertEquals("Hay que indicar las fechas de inicio y salida de la reserva", ex.getMessage());
    }

    @Test
    public void updateReservation_invalidTotalPrice_throwsException() {
        // Arrange
        long id = 1L;
        Reservation reservation = new Reservation();
        reservation.client_id = 10L;
        reservation.status = ReservationStatus.PENDING;
        reservation.room_id = 2L;
        Reservation updatedReservation = new Reservation();
        updatedReservation.start_date = new Date(1000L);
        updatedReservation.end_date = new Date(2000L);
        // Si updatedReservation.room_id es null, se mantiene el anterior (2L)

        User user = new User();
        user.id = 10L;
        user.role = Role.CLIENT;
        Client client = new Client();
        client.id = 10L;

        when(reservationRepository.getReservationById(id)).thenReturn(Optional.of(reservation));
        when(clientRepository.getClientByUserId(user.id)).thenReturn(Optional.of(client));
        // Stub del método calculateTotal en el spy
        when(reservationUseCase.calculateTotal(updatedReservation.start_date, updatedReservation.end_date,
                reservation.room_id)).thenReturn(BigDecimal.valueOf(-1.0));

        // Act & Assert
        Exception ex = assertThrows(RuntimeException.class, () ->
                reservationUseCase.updateReservation(id, updatedReservation, user)
        );
        assertEquals("Fechas de inicio y salida de la reserva erróneas.", ex.getMessage());
    }

    @Test
    public void updateReservation_success_client() {
        // Arrange
        long id = 1L;
        Reservation reservation = new Reservation();
        reservation.client_id = 10L;
        reservation.status = ReservationStatus.PENDING;
        reservation.room_id = 2L;
        // Fechas originales
        reservation.start_date = new Date(1000L);
        reservation.end_date = new Date(2000L);

        User user = new User();
        user.id = 10L;
        user.role = Role.CLIENT;
        Client client = new Client();
        client.id = 10L;

        // Datos a actualizar
        Reservation updatedReservation = new Reservation();
        updatedReservation.room_id = 3L; // se cambia
        Date newStart = new Date(3000L);
        Date newEnd = new Date(4000L);
        updatedReservation.start_date = newStart;
        updatedReservation.end_date = newEnd;

        when(reservationRepository.getReservationById(id)).thenReturn(Optional.of(reservation));
        when(clientRepository.getClientByUserId(user.id)).thenReturn(Optional.of(client));
        // Stub para calculateTotal (usando el nuevo room_id que se actualizará)
        when(reservationUseCase.calculateTotal(newStart, newEnd, 3L))
                .thenReturn(new BigDecimal("500.00"));
        when(reservationRepository.updateReservation(reservation)).thenReturn(1);

        // Act & Assert
        int updatedRows = reservationUseCase.updateReservation(id, updatedReservation, user);
        assertEquals(1, updatedRows);
        assertEquals(3L, reservation.room_id);
        assertEquals(newStart, reservation.start_date);
        assertEquals(newEnd, reservation.end_date);
        assertEquals(new BigDecimal("500.00"), reservation.total_price);

        verify(reservationRepository).updateReservation(reservation);
    }

    @Test
    public void updateReservation_success_admin() {
        // Arrange
        long id = 1L;
        Reservation reservation = new Reservation();
        // Para un ADMIN, no se valida el cliente; solo se requiere que la reserva exista.
        reservation.status = ReservationStatus.PENDING;
        reservation.room_id = 2L;
        reservation.start_date = new Date(1000L);
        reservation.end_date = new Date(2000L);

        User user = new User();
        user.id = 20L;
        user.role = Role.ADMIN;

        Reservation updatedReservation = new Reservation();
        // Actualizamos solo las fechas (no se actualiza room_id porque es null)
        Date newStart = new Date(3000L);
        Date newEnd = new Date(4000L);
        updatedReservation.start_date = newStart;
        updatedReservation.end_date = newEnd;
        // updatedReservation.room_id se deja nulo, por lo que se mantiene el valor anterior (2L)

        when(reservationRepository.getReservationById(id)).thenReturn(Optional.of(reservation));
        // Para ADMIN, no se llama a clientRepository
        when(reservationUseCase.calculateTotal(newStart, newEnd, reservation.room_id))
                .thenReturn(new BigDecimal("300.00"));
        when(reservationRepository.updateReservation(reservation)).thenReturn(1);

        // Act & Assert
        int updatedRows = reservationUseCase.updateReservation(id, updatedReservation, user);
        assertEquals(1, updatedRows);
        // La habitación permanece igual
        assertEquals(2L, reservation.room_id);
        assertEquals(newStart, reservation.start_date);
        assertEquals(newEnd, reservation.end_date);
        assertEquals(new BigDecimal("300.00"), reservation.total_price);

        verify(reservationRepository).updateReservation(reservation);
    }

    @Test
    public void cancelReservation_success() {
        // Arrange
        long reservationId = 1L;
        Reservation reservation = new Reservation();
        reservation.setStatus(ReservationStatus.PENDING);

        when(reservationRepository.getReservationById(reservationId)).thenReturn(Optional.of(reservation));
        when(reservationRepository.updateReservation(reservation)).thenReturn(1);

        // Act
        int updatedRows = reservationUseCase.cancelReservation(reservationId);

        // Assert
        assertEquals(1, updatedRows);
        assertEquals(ReservationStatus.CANCELED, reservation.status);
    }

    @Test
    public void cancelReservation_failure_reservationNotFound_shouldThrowException() {
        // Arrange
        long reservationId = 1L;

        when(reservationRepository.getReservationById(reservationId)).thenReturn(Optional.empty());

        // Act && Assert
        Exception exception = assertThrows(RuntimeException.class, () ->
            reservationUseCase.cancelReservation(reservationId)
        );
        assertNotNull(exception);
        assertEquals("La reserva no existe", exception.getMessage());
    }

    @Test
    public void cancelReservation_failure_canNotCancelledConfirmedReservation_shouldThrowException() {
        // Arrange
        long reservationId = 1L;
        Reservation reservation = new Reservation();
        reservation.setStatus(ReservationStatus.CONFIRMED);

        when(reservationRepository.getReservationById(reservationId)).thenReturn(Optional.of(reservation));

        // Act && Assert
        Exception exception = assertThrows(RuntimeException.class, () ->
                reservationUseCase.cancelReservation(reservationId)
        );
        assertNotNull(exception);
        assertEquals("No se puede cancelar una reserva confirmada.", exception.getMessage());
    }

    @Test
    public void createPayment_reservationNotFound_throwsException() {
        // Arrange
        long reservationId = 1L;
        Payment newPayment = new Payment();
        newPayment.amount = new BigDecimal("50.00");
        User user = new User();
        when(reservationRepository.getReservationById(reservationId)).thenReturn(Optional.empty());

        // Act & Assert
        Exception ex = assertThrows(IllegalArgumentException.class, () ->
                reservationUseCase.createPayment(newPayment, reservationId, user)
        );
        assertEquals("La reserva no existe", ex.getMessage());
        verify(reservationRepository).getReservationById(reservationId);
    }

    @Test
    public void createPayment_clientNotAuthorized_throwsException() {
        // Arrange
        long reservationId = 1L;

        Payment newPayment = new Payment();
        newPayment.amount = new BigDecimal("50.00");

        User user = new User();
        user.id = 10L;
        user.role = Role.CLIENT;

        Reservation reservation = new Reservation();
        reservation.client_id = 20L;  // No coincide con user.id
        reservation.total_price = new BigDecimal("200.00");
        reservation.status = ReservationStatus.PENDING;

        when(reservationRepository.getReservationById(reservationId)).thenReturn(Optional.of(reservation));

        // Act & Assert
        Exception ex = assertThrows(AccessDeniedException.class, () ->
                reservationUseCase.createPayment(newPayment, reservationId, user)
        );
        assertEquals("No autorizado", ex.getMessage());
    }

    @Test
    public void createPayment_reservationCanceled_throwsException() {
        // Arrange
        long reservationId = 1L;

        Payment newPayment = new Payment();
        newPayment.amount = new BigDecimal("50.00");

        User user = new User();
        user.role = Role.ADMIN; // Rol ADMIN, para evitar validación de cliente

        Reservation reservation = new Reservation();
        reservation.total_price = new BigDecimal("200.00");
        reservation.status = ReservationStatus.CANCELED;

        when(reservationRepository.getReservationById(reservationId)).thenReturn(Optional.of(reservation));

        // Act && Assert
        Exception ex = assertThrows(RuntimeException.class, () ->
                reservationUseCase.createPayment(newPayment, reservationId, user)
        );
        assertEquals("No se pueden registrar pagos para reservas canceladas", ex.getMessage());
    }

    @Test
    public void createPayment_amountExceedsRemainingAndConfirmed_throwsException() {
        // Arrange
        long reservationId = 1L;

        Payment newPayment = new Payment();
        newPayment.amount = new BigDecimal("60.00");  // Excede lo pendiente

        User user = new User();
        user.role = Role.ADMIN;

        Reservation reservation = new Reservation();
        reservation.total_price = new BigDecimal("200.00");
        reservation.status = ReservationStatus.CONFIRMED;

        when(reservationRepository.getReservationById(reservationId)).thenReturn(Optional.of(reservation));
        // Supongamos que ya se han pagado 150, entonces remaining = 50
        when(paymentRepository.getTotalPaid(reservationId)).thenReturn(new BigDecimal("150.00"));

        // Act & Assert
        Exception ex = assertThrows(RuntimeException.class, () ->
                reservationUseCase.createPayment(newPayment, reservationId, user)
        );
        assertEquals("El pago no se ha realizado ya que la reserva ya esta pagada y confirmada", ex.getMessage());
    }

    @Test
    public void createPayment_amountExceedsRemaining_pending_throwsException() {
        // Arrange
        long reservationId = 1L;

        Payment newPayment = new Payment();
        newPayment.amount = new BigDecimal("60.00");

        User user = new User();
        user.role = Role.ADMIN;

        Reservation reservation = new Reservation();
        reservation.total_price = new BigDecimal("200.00");
        reservation.status = ReservationStatus.PENDING;

        when(reservationRepository.getReservationById(reservationId)).thenReturn(Optional.of(reservation));
        // Supongamos que ya se han pagado 150, remaining = 50.
        when(paymentRepository.getTotalPaid(reservationId)).thenReturn(new BigDecimal("150.00"));

        // Act & Assert
        Exception ex = assertThrows(RuntimeException.class, () ->
                reservationUseCase.createPayment(newPayment, reservationId, user)
        );
        assertEquals("El pago excede el monto pendiente", ex.getMessage());
    }

    @Test
    public void createPayment_success_exactPayment_updatesReservationToConfirmed() {
        // Arrange
        long reservationId = 1L;

        Payment newPayment = new Payment();
        newPayment.amount = new BigDecimal("50.00"); // Exactamente el restante

        User user = new User();
        user.id = 10L;
        user.role = Role.CLIENT;

        Reservation reservation = new Reservation();
        reservation.total_price = new BigDecimal("200.00");
        reservation.status = ReservationStatus.PENDING;
        reservation.client_id = 10L;

        when(reservationRepository.getReservationById(reservationId)).thenReturn(Optional.of(reservation));
        // Para este caso, se supone que ya se han pagado 150.
        when(paymentRepository.getTotalPaid(reservationId)).thenReturn(new BigDecimal("150.00"));
        // Simular creación del pago
        when(paymentRepository.createPayment(newPayment)).thenReturn(7L);

        // Act & Assert
        long savedPaymentId = reservationUseCase.createPayment(newPayment, reservationId, user);
        assertEquals(7L, savedPaymentId);
        // Verificar que se asigna la fecha y el reservationId al pago
        assertNotNull(newPayment.payment_date);
        assertEquals(reservationId, newPayment.reservation_id);
        // Como 150+50 = 200, se actualiza el estado a CONFIRMED
        assertEquals(ReservationStatus.CONFIRMED, reservation.status);
        verify(paymentRepository).createPayment(newPayment);
        verify(reservationRepository).updateReservation(reservation);
    }

    @Test
    public void createPayment_success_partialPayment_doesNotUpdateReservationStatus() {
        // Arrange
        long reservationId = 1L;

        Payment newPayment = new Payment();
        newPayment.amount = new BigDecimal("50.00");

        User user = new User();
        user.id = 10L;
        user.role = Role.CLIENT;

        Reservation reservation = new Reservation();
        reservation.total_price = new BigDecimal("200.00");
        reservation.status = ReservationStatus.PENDING;
        reservation.client_id = 10L;

        when(reservationRepository.getReservationById(reservationId)).thenReturn(Optional.of(reservation));
        // Supongamos que ya se han pagado 100, remaining = 100 y 50 < 100.
        when(paymentRepository.getTotalPaid(reservationId)).thenReturn(new BigDecimal("100.00"));
        when(paymentRepository.createPayment(newPayment)).thenReturn(7L);

        // Act & Assert
        long savedPaymentId = reservationUseCase.createPayment(newPayment, reservationId, user);

        assertEquals(7L, savedPaymentId);
        // La reserva permanece en estado PENDING
        assertEquals(ReservationStatus.PENDING, reservation.status);

        verify(paymentRepository).createPayment(newPayment);
        verify(reservationRepository, never()).updateReservation(any());
    }

    @Test
    public void getPaymentsByClient_success() {
        // Arrange: Creamos una lista de pagos, simulamos su retorno para el clientId creado
        long clientId = 5L;
        List<Payment> paymentList = new ArrayList<>();
        Payment payment = new Payment();
        paymentList.add(payment);

        when(paymentRepository.getPaymentsByClient(clientId)).thenReturn(paymentList);

        // Act
        List<Payment> result = reservationUseCase.getPaymentsByClient(clientId);

        // Assert
        assertEquals(paymentList, result);
        verify(paymentRepository).getPaymentsByClient(clientId);
    }
}