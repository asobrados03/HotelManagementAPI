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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * This class contains the attributes and methods for realize the unit tests
 * of the reservations operations business logic.
 *
 * @author Alfredo Sobrados González
 */
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

    /**
     * Initializes the test environment before each test by creating a spy for ReservationUseCase.
     */
    @BeforeEach
    public void setup() {
        reservationUseCase = spy(new ReservationUseCase(reservationRepository, paymentRepository, roomRepository,
                clientRepository));
    }

    /**
     * Tests that getAllReservations() returns the expected list of reservations from the repository.
     */
    @Test
    public void getAllReservations_success() {
        // Arrange: Prepare a list of reservations
        List<Reservation> reservationList = new ArrayList<>();
        Reservation reservation = new Reservation();
        // Configure reservation properties if necessary
        reservationList.add(reservation);
        when(reservationRepository.getAllReservations()).thenReturn(reservationList);

        // Act: Execute the method
        List<Reservation> result = reservationUseCase.getAllReservations();

        // Assert: Verify that the result matches the expected list and that the repository method was called
        assertEquals(reservationList, result);
        verify(reservationRepository).getAllReservations();
    }

    /**
     * Tests that getReservationById() returns the reservation when a valid id is provided.
     */
    @Test
    public void getReservationById_success() {
        // Arrange: Create a reservation and simulate its return for the given identifier
        Reservation reservation = new Reservation();
        when(reservationRepository.getReservationById(10L)).thenReturn(Optional.of(reservation));

        // Act
        Optional<Reservation> result = reservationUseCase.getReservationById(10L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(reservation, result.get());
        verify(reservationRepository).getReservationById(10L);
    }

    /**
     * Tests that getReservationsByClient() returns the correct list of reservations for a given client.
     */
    @Test
    public void getReservationsByClient_success() {
        // Arrange: Create a reservation, client, and the corresponding list of reservations
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

    /**
     * Tests that getReservationsByClient() throws a RuntimeException when the client is not found.
     */
    @Test
    public void getReservationsByClient_failure_shouldThrowException() {
        // Arrange: Create a user for which no client is found
        User user = new User();
        user.setId(5L);

        when(clientRepository.getClientByUserId(user.id)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () ->
                reservationUseCase.getReservationsByClient(user)
        );

        assertEquals("El cliente autenticado no existe", exception.getMessage());
        verify(clientRepository).getClientByUserId(user.id);
    }

    /**
     * Tests that a client can successfully create a reservation when all conditions are met.
     */
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

    /**
     * Tests that an admin can successfully create a reservation without requiring client verification.
     */
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

    /**
     * Tests that createReservation() throws an exception when the room is not available for the selected dates.
     */
    @Test
    public void createReservation_roomIsNotAvailable_shouldThrowException() {
        // Arrange
        Reservation newReservation = new Reservation();
        User user = new User();

        when(reservationRepository.isRoomAvailable(newReservation.room_id, newReservation.start_date,
                newReservation.end_date)).thenReturn(false);

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () ->
                reservationUseCase.createReservation(newReservation, user)
        );

        assertNotNull(exception);
        assertEquals("Habitación no disponible en las fechas solicitadas", exception.getMessage());
    }

    /**
     * Tests that createReservation() throws an exception when the check-in and check-out dates are the same.
     */
    @Test
    public void createReservation_totalPriceMinusOne_sameDay_shouldThrowException() {
        // Arrange
        Reservation newReservation = new Reservation();
        newReservation.setRoom_id(2L);
        // Simulate a reservation requested for the same day
        LocalDate sameDay = LocalDate.now();
        newReservation.setStart_date(sameDay);
        newReservation.setEnd_date(sameDay);

        User user = new User();
        user.setRole(Role.ADMIN); // Role does not affect this validation

        when(reservationRepository.isRoomAvailable(newReservation.room_id, newReservation.start_date,
                newReservation.end_date)).thenReturn(true);
        // Stub calculateTotal to return -1.0
        when(reservationUseCase.calculateTotal(newReservation.start_date, newReservation.end_date,
                newReservation.room_id)).thenReturn(BigDecimal.valueOf(-1.0));

        // Act & Assert
        Exception ex = assertThrows(RuntimeException.class, () ->
                reservationUseCase.createReservation(newReservation, user));
        assertEquals("La fecha de entrada y salida no pueden ser el mismo día. Debe haber al menos una noche de"
                + " estancia.", ex.getMessage());
    }

    /**
     * Tests that createReservation() throws an exception when the end date is before the start date.
     */
    @Test
    public void createReservation_totalPriceMinusOne_endDateBeforeStartDate_shouldThrowException() {
        // Arrange
        Reservation newReservation = new Reservation();
        newReservation.setRoom_id(2L);
        // Set dates: end date is before start date
        LocalDate startDate = LocalDate.now().plusDays(5);
        LocalDate endDate = LocalDate.now();
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

    /**
     * Tests that createReservation() throws an exception for other conditions that result in an invalid total price
     * calculation.
     */
    @Test
    public void createReservation_totalPriceMinusOne_otherCondition_shouldThrowException() {
        // Arrange
        Reservation newReservation = new Reservation();
        newReservation.setRoom_id(2L);
        // Set valid dates (different and with end_date after start_date)
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(5);
        newReservation.setStart_date(startDate);
        newReservation.setEnd_date(endDate);

        User user = new User();
        user.setRole(Role.ADMIN);

        when(reservationRepository.isRoomAvailable(newReservation.room_id, newReservation.start_date,
                newReservation.end_date)).thenReturn(true);
        // Stub calculateTotal to return -1.0
        when(reservationUseCase.calculateTotal(newReservation.start_date, newReservation.end_date, newReservation.room_id))
                .thenReturn(BigDecimal.valueOf(-1.0));

        // Act & Assert
        Exception ex = assertThrows(RuntimeException.class, () ->
                reservationUseCase.createReservation(newReservation, user));
        assertEquals("Error al calcular el precio total de la reserva. Verifica las fechas y el precio de la " +
                "habitación o si la habitación realmente existe.", ex.getMessage());
    }

    /**
     * Tests that createReservation() throws an exception when attempting to reserve a room that is under maintenance.
     */
    @Test
    public void createReservation_roomInMaintenance_shouldThrowException() {
        // Arrange
        Reservation newReservation = new Reservation();
        newReservation.setRoom_id(2L);
        // Set valid dates
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(5);
        newReservation.setStart_date(startDate);
        newReservation.setEnd_date(endDate);

        User user = new User();
        user.setRole(Role.ADMIN);

        when(reservationRepository.isRoomAvailable(newReservation.room_id, newReservation.start_date,
                newReservation.end_date)).thenReturn(true);
        // Stub to return a valid total (e.g., 10)
        when(reservationUseCase.calculateTotal(newReservation.start_date, newReservation.end_date,
                newReservation.room_id)).thenReturn(BigDecimal.TEN);
        // Simulate that the room is under maintenance
        Room room = new Room();
        room.status = RoomStatus.MAINTENANCE;
        when(roomRepository.getRoomById(newReservation.room_id)).thenReturn(Optional.of(room));

        // Act & Assert
        Exception ex = assertThrows(RuntimeException.class, () ->
                reservationUseCase.createReservation(newReservation, user));
        assertEquals("No se puede reservar una habitación en mantenimiento.", ex.getMessage());
    }

    /**
     * Tests that createReservation() throws an exception when the specified room is not found.
     */
    @Test
    public void createReservation_roomNotFound_shouldThrowException() {
        // Arrange
        Reservation newReservation = new Reservation();
        newReservation.setRoom_id(2L);
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(5);
        newReservation.setStart_date(startDate);
        newReservation.setEnd_date(endDate);

        User user = new User();

        when(reservationRepository.isRoomAvailable(newReservation.room_id, newReservation.start_date,
                newReservation.end_date)).thenReturn(true);
        // Stub to calculate a valid total (e.g., 10)
        when(reservationUseCase.calculateTotal(newReservation.start_date, newReservation.end_date,
                newReservation.room_id)).thenReturn(BigDecimal.TEN);
        // Simulate that the room is not found
        when(roomRepository.getRoomById(newReservation.room_id)).thenReturn(Optional.empty());

        // Act & Assert
        Exception ex = assertThrows(RuntimeException.class, () ->
                reservationUseCase.createReservation(newReservation, user));
        assertEquals("Error interno.", ex.getMessage());
        verify(roomRepository, times(2)).getRoomById(newReservation.room_id);
    }

    /**
     * Tests that createReservation() throws an exception when the client associated with the user is not found.
     */
    @Test
    public void createReservation_clientNotFound_shouldThrowException() {
        // Arrange
        Reservation newReservation = new Reservation();
        newReservation.setRoom_id(2L);
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(5);
        newReservation.setStart_date(startDate);
        newReservation.setEnd_date(endDate);

        User user = new User();
        user.setId(6L);
        user.setRole(Role.CLIENT);

        when(reservationRepository.isRoomAvailable(newReservation.room_id, newReservation.start_date,
                newReservation.end_date)).thenReturn(true);
        when(reservationUseCase.calculateTotal(newReservation.start_date, newReservation.end_date,
                newReservation.room_id)).thenReturn(BigDecimal.TEN);

        // Simulate that a valid room is found (not under maintenance)
        Room room = new Room();
        room.status = RoomStatus.AVAILABLE;
        when(roomRepository.getRoomById(newReservation.room_id)).thenReturn(Optional.of(room));

        // Simulate that the client for the user is not found
        when(clientRepository.getClientByUserId(user.id)).thenReturn(Optional.empty());

        // Act & Assert
        Exception ex = assertThrows(RuntimeException.class, () ->
                reservationUseCase.createReservation(newReservation, user));
        assertEquals("Cliente no encontrado en la base de datos.", ex.getMessage());
        verify(clientRepository).getClientByUserId(user.id);
    }

    /**
     * Tests that calculateTotal() returns -1 when the start date is null.
     */
    @Test
    public void calculateTotal_startDateNull_returnsMinusOne() {
        // Arrange
        LocalDate endDate = LocalDate.now();
        long roomId = 1L;

        // Act & Assert
        BigDecimal result = reservationUseCase.calculateTotal(null, endDate, roomId);
        assertEquals(BigDecimal.valueOf(-1.0), result);
    }

    /**
     * Tests that calculateTotal() returns -1 when the end date is null.
     */
    @Test
    public void calculateTotal_endDateNull_returnsMinusOne() {
        // Arrange
        LocalDate startDate = LocalDate.now();
        long roomId = 1L;

        // Act & Assert
        BigDecimal result = reservationUseCase.calculateTotal(startDate, null, roomId);
        assertEquals(BigDecimal.valueOf(-1.0), result);
    }

    /**
     * Tests that calculateTotal() returns -1 when the end date is before the start date.
     */
    @Test
    public void calculateTotal_endDateBeforeStartDate_returnsMinusOne() {
        // Arrange
        // Set startDate as today and endDate as yesterday
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().minusDays(1);
        long roomId = 1L;

        // Act & Assert
        BigDecimal result = reservationUseCase.calculateTotal(startDate, endDate, roomId);
        assertEquals(BigDecimal.valueOf(-1.0), result);
    }

    /**
     * Tests that calculateTotal() returns -1 when the check-in and check-out dates are the same.
     */
    @Test
    public void calculateTotal_sameDay_returnsMinusOne() {
        // Arrange
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now();
        long roomId = 1L;

        // Act & Assert
        BigDecimal result = reservationUseCase.calculateTotal(startDate, endDate, roomId);
        assertEquals(BigDecimal.valueOf(-1.0), result);
    }

    /**
     * Tests that calculateTotal() returns -1 when the room's price per night is zero or not positive.
     */
    @Test
    public void calculateTotal_roomPriceZero_returnsMinusOne() {
        // Arrange
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = startDate.plusDays(1);
        long roomId = 1L;

        Room room = new Room();
        room.price_per_night = BigDecimal.ZERO; // Non-positive price
        when(roomRepository.getRoomById(roomId)).thenReturn(Optional.of(room));

        // Act & Assert
        BigDecimal result = reservationUseCase.calculateTotal(startDate, endDate, roomId);
        assertEquals(BigDecimal.valueOf(-1.0), result);
    }

    /**
     * Tests that calculateTotal() returns -1 when the room is not found.
     */
    @Test
    public void calculateTotal_roomNotFound_returnsMinusOne() {
        // Arrange
        LocalDate startDate = LocalDate.now().plusDays(1);
        LocalDate endDate = startDate.plusDays(1);
        long roomId = 1L;

        when(roomRepository.getRoomById(roomId)).thenReturn(Optional.empty());

        // Act & Assert
        BigDecimal result = reservationUseCase.calculateTotal(startDate, endDate, roomId);
        assertEquals(BigDecimal.valueOf(-1.0), result);
    }

    /**
     * Tests that calculateTotal() returns the room's price per night when the reservation is for one night.
     */
    @Test
    public void calculateTotal_oneNight_returnsPricePerNight() {
        // Arrange
        LocalDate startDate = LocalDate.of(2023, 3, 1);
        LocalDate endDate = startDate.plusDays(1); // 1 night
        long roomId = 1L;

        Room room = new Room();
        room.price_per_night = new BigDecimal("100.00");
        when(roomRepository.getRoomById(roomId)).thenReturn(Optional.of(room));

        // Act & Assert
        BigDecimal result = reservationUseCase.calculateTotal(startDate, endDate, roomId);
        assertEquals(new BigDecimal("100.00"), result);
    }

    /**
     * Tests that calculateTotal() returns the correct total price for a reservation spanning multiple nights.
     */
    @Test
    public void calculateTotal_multipleNights_returnsTotalPrice() {
        // Arrange
        // Example: checkIn = 2023-03-01, checkOut = 2023-03-05 → 4 nights
        LocalDate startDate = LocalDate.of(2023, 3, 1);
        LocalDate endDate = startDate.plusDays(4);
        long roomId = 1L;

        Room room = new Room();
        room.price_per_night = new BigDecimal("150.00");
        when(roomRepository.getRoomById(roomId)).thenReturn(Optional.of(room));

        // Act & Assert
        BigDecimal result = reservationUseCase.calculateTotal(startDate, endDate, roomId);
        // 4 nights * 150.00 = 600.00
        assertEquals(new BigDecimal("600.00"), result);
    }

    /**
     * Tests that updateReservation() throws an exception when the reservation is not found.
     */
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

    /**
     * Tests that updateReservation() throws an exception when the client is not found for a client making the update.
     */
    @Test
    public void updateReservation_clientNotFound_throwsException() {
        // Arrange
        long id = 1L;
        Reservation reservation = new Reservation();
        reservation.client_id = 10L; // Reservation associated with client id 10
        reservation.status = ReservationStatus.PENDING;
        Reservation updatedReservation = new Reservation();
        // Dates must be provided to avoid date check failures
        updatedReservation.start_date = LocalDate.now();
        updatedReservation.end_date = LocalDate.now().plusDays(2);

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

    /**
     * Tests that updateReservation() throws an AccessDeniedException when a client attempts to update a reservation that does not belong to them.
     */
    @Test
    public void updateReservation_unauthorizedClient_throwsAccessDeniedException() {
        // Arrange
        long id = 1L;
        Reservation reservation = new Reservation();
        reservation.client_id = 10L; // Reservation belongs to client 10
        reservation.status = ReservationStatus.PENDING;
        Reservation updatedReservation = new Reservation();
        updatedReservation.start_date = LocalDate.now();
        updatedReservation.end_date = LocalDate.now().plusDays(2);

        User user = new User();
        user.id = 5L;
        user.role = Role.CLIENT;
        Client client = new Client();
        client.id = 5L; // Client obtained from repository

        when(reservationRepository.getReservationById(id)).thenReturn(Optional.of(reservation));
        when(clientRepository.getClientByUserId(user.id)).thenReturn(Optional.of(client));

        // Act & Assert
        Exception ex = assertThrows(AccessDeniedException.class, () ->
                reservationUseCase.updateReservation(id, updatedReservation, user)
        );
        assertEquals("No autorizado", ex.getMessage());
    }

    /**
     * Tests that updateReservation() throws an exception when a client attempts to update a reservation that is not in a pending state.
     */
    @Test
    public void updateReservation_clientNonPendingReservation_throwsException() {
        // Arrange
        long id = 1L;
        Reservation reservation = new Reservation();
        reservation.client_id = 10L;
        reservation.status = ReservationStatus.CONFIRMED; // Not pending
        Reservation updatedReservation = new Reservation();
        updatedReservation.start_date = LocalDate.now();
        updatedReservation.end_date = LocalDate.now().plusDays(2);

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
        assertEquals("No se pueden modificar reservas canceladas o confirmadas si eres cliente.",
                ex.getMessage());
    }

    /**
     * Tests that updateReservation() throws an exception when the updated reservation is missing the start and end
     * dates.
     */
    @Test
    public void updateReservation_missingDates_throwsException() {
        // Arrange
        long id = 1L;
        Reservation reservation = new Reservation();
        reservation.client_id = 10L;
        reservation.status = ReservationStatus.PENDING;
        reservation.room_id = 2L;
        Reservation updatedReservation = new Reservation();
        // start_date and end_date are not set

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

    /**
     * Tests that updateReservation() throws an exception when the total price calculation returns an invalid value.
     */
    @Test
    public void updateReservation_invalidTotalPrice_throwsException() {
        // Arrange
        long id = 1L;
        Reservation reservation = new Reservation();
        reservation.client_id = 10L;
        reservation.status = ReservationStatus.PENDING;
        reservation.room_id = 2L;
        Reservation updatedReservation = new Reservation();
        updatedReservation.start_date = LocalDate.now();
        updatedReservation.end_date = LocalDate.now().plusDays(2);
        updatedReservation.room_id = 2L;

        User user = new User();
        user.id = 10L;
        user.role = Role.CLIENT;
        Client client = new Client();
        client.id = 10L;

        when(reservationRepository.getReservationById(id)).thenReturn(Optional.of(reservation));
        when(clientRepository.getClientByUserId(user.id)).thenReturn(Optional.of(client));
        // Stub calculateTotal in the spy
        when(reservationUseCase.calculateTotal(updatedReservation.start_date, updatedReservation.end_date,
                reservation.room_id)).thenReturn(BigDecimal.valueOf(-1.0));

        // Act & Assert
        Exception ex = assertThrows(RuntimeException.class, () ->
                reservationUseCase.updateReservation(id, updatedReservation, user)
        );
        assertEquals("Fechas de inicio y salida de la reserva erróneas.", ex.getMessage());
    }

    /**
     * Tests that a client successfully updates a reservation, and that the reservation's details are updated
     * accordingly.
     */
    @Test
    public void updateReservation_success_client() {
        // Arrange
        long id = 1L;
        Reservation reservation = new Reservation();
        reservation.client_id = 10L;
        reservation.status = ReservationStatus.PENDING;
        reservation.room_id = 2L;
        // Original dates
        reservation.start_date = LocalDate.now();
        reservation.end_date = LocalDate.now().plusDays(2);

        User user = new User();
        user.id = 10L;
        user.role = Role.CLIENT;
        Client client = new Client();
        client.id = 10L;

        // Data to update
        Reservation updatedReservation = new Reservation();
        updatedReservation.room_id = 3L; // changed
        LocalDate newStart = LocalDate.now().plusDays(4);
        LocalDate newEnd = LocalDate.now().plusDays(6);
        updatedReservation.start_date = newStart;
        updatedReservation.end_date = newEnd;

        when(reservationRepository.getReservationById(id)).thenReturn(Optional.of(reservation));
        when(clientRepository.getClientByUserId(user.id)).thenReturn(Optional.of(client));
        // Stub for calculateTotal (using the new room_id to be updated)
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

    /**
     * Tests that an admin successfully updates a reservation, with the updated dates applied while preserving
     * the room ID.
     */
    @Test
    public void updateReservation_success_admin() {
        // Arrange
        long id = 1L;
        Reservation reservation = new Reservation();
        // For ADMIN, no client validation is required; only the reservation must exist.
        reservation.status = ReservationStatus.PENDING;
        reservation.room_id = 2L;
        reservation.start_date = LocalDate.now();
        reservation.end_date = LocalDate.now().plusDays(2);

        User user = new User();
        user.id = 20L;
        user.role = Role.ADMIN;

        Reservation updatedReservation = new Reservation();
        // Update only the dates (room_id remains null, so the previous value is kept)
        LocalDate newStart = LocalDate.now().plusDays(4);
        LocalDate newEnd = LocalDate.now().plusDays(6);
        updatedReservation.start_date = newStart;
        updatedReservation.end_date = newEnd;
        updatedReservation.room_id = 2L;

        when(reservationRepository.getReservationById(id)).thenReturn(Optional.of(reservation));
        // For ADMIN, clientRepository is not called
        when(reservationUseCase.calculateTotal(newStart, newEnd, reservation.room_id))
                .thenReturn(new BigDecimal("300.00"));
        when(reservationRepository.updateReservation(reservation)).thenReturn(1);

        // Act & Assert
        int updatedRows = reservationUseCase.updateReservation(id, updatedReservation, user);
        assertEquals(1, updatedRows);
        // Room remains the same
        assertEquals(2L, reservation.room_id);
        assertEquals(newStart, reservation.start_date);
        assertEquals(newEnd, reservation.end_date);
        assertEquals(new BigDecimal("300.00"), reservation.total_price);

        verify(reservationRepository).updateReservation(reservation);
    }

    /**
     * Tests that cancelReservation() successfully cancels a pending reservation and updates its status to CANCELED.
     */
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

    /**
     * Tests that cancelReservation() throws an exception when the reservation to be canceled is not found.
     */
    @Test
    public void cancelReservation_failure_reservationNotFound_shouldThrowException() {
        // Arrange
        long reservationId = 1L;

        when(reservationRepository.getReservationById(reservationId)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () ->
                reservationUseCase.cancelReservation(reservationId)
        );
        assertNotNull(exception);
        assertEquals("La reserva no existe", exception.getMessage());
    }

    /**
     * Tests that cancelReservation() throws an exception when attempting to cancel a confirmed reservation.
     */
    @Test
    public void cancelReservation_failure_canNotCancelledConfirmedReservation_shouldThrowException() {
        // Arrange
        long reservationId = 1L;
        Reservation reservation = new Reservation();
        reservation.setStatus(ReservationStatus.CONFIRMED);

        when(reservationRepository.getReservationById(reservationId)).thenReturn(Optional.of(reservation));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () ->
                reservationUseCase.cancelReservation(reservationId)
        );
        assertNotNull(exception);
        assertEquals("No se puede cancelar una reserva confirmada.", exception.getMessage());
    }

    /**
     * Tests that createPayment() throws an exception when the reservation for the payment is not found.
     */
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

    /**
     * Tests that createPayment() throws an AccessDeniedException when the client is not authorized to make a payment
     * for the reservation.
     */
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
        reservation.client_id = 20L;  // Does not match user.id
        reservation.total_price = new BigDecimal("200.00");
        reservation.status = ReservationStatus.PENDING;

        when(reservationRepository.getReservationById(reservationId)).thenReturn(Optional.of(reservation));

        // Act & Assert
        Exception ex = assertThrows(AccessDeniedException.class, () ->
                reservationUseCase.createPayment(newPayment, reservationId, user)
        );
        assertEquals("No autorizado", ex.getMessage());
    }

    /**
     * Tests that createPayment() throws an exception when attempting to make a payment for a canceled reservation.
     */
    @Test
    public void createPayment_reservationCanceled_throwsException() {
        // Arrange
        long reservationId = 1L;

        Payment newPayment = new Payment();
        newPayment.amount = new BigDecimal("50.00");

        User user = new User();
        user.role = Role.ADMIN; // ADMIN role to avoid client validation

        Reservation reservation = new Reservation();
        reservation.total_price = new BigDecimal("200.00");
        reservation.status = ReservationStatus.CANCELED;

        when(reservationRepository.getReservationById(reservationId)).thenReturn(Optional.of(reservation));

        // Act & Assert
        Exception ex = assertThrows(RuntimeException.class, () ->
                reservationUseCase.createPayment(newPayment, reservationId, user)
        );
        assertEquals("No se pueden registrar pagos para reservas canceladas", ex.getMessage());
    }

    /**
     * Tests that createPayment() throws an exception when the payment amount exceeds the remaining balance
     * for a confirmed reservation.
     */
    @Test
    public void createPayment_amountExceedsRemainingAndConfirmed_throwsException() {
        // Arrange
        long reservationId = 1L;

        Payment newPayment = new Payment();
        newPayment.amount = new BigDecimal("60.00");  // Exceeds the remaining amount

        User user = new User();
        user.role = Role.ADMIN;

        Reservation reservation = new Reservation();
        reservation.total_price = new BigDecimal("200.00");
        reservation.status = ReservationStatus.CONFIRMED;

        when(reservationRepository.getReservationById(reservationId)).thenReturn(Optional.of(reservation));
        // Assume that 150 has already been paid, so remaining = 50
        when(paymentRepository.getTotalPaid(reservationId)).thenReturn(new BigDecimal("150.00"));

        // Act & Assert
        Exception ex = assertThrows(RuntimeException.class, () ->
                reservationUseCase.createPayment(newPayment, reservationId, user)
        );
        assertEquals("El pago no se ha realizado ya que la reserva ya está pagada y confirmada", ex.getMessage());
    }

    /**
     * Tests that createPayment() throws an exception when the payment amount exceeds the remaining balance for a
     * pending reservation.
     */
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
        // Assume that 150 has already been paid, remaining = 50.
        when(paymentRepository.getTotalPaid(reservationId)).thenReturn(new BigDecimal("150.00"));

        // Act & Assert
        Exception ex = assertThrows(RuntimeException.class, () ->
                reservationUseCase.createPayment(newPayment, reservationId, user)
        );
        assertEquals("El pago excede el monto pendiente", ex.getMessage());
    }

    /**
     * Tests that createPayment() successfully processes an exact payment that fulfills the reservation's total price,
     * and updates the reservation status to CONFIRMED.
     */
    @Test
    public void createPayment_success_exactPayment_updatesReservationToConfirmed() {
        // Arrange
        long reservationId = 1L;

        Payment newPayment = new Payment();
        newPayment.amount = new BigDecimal("50.00"); // Exactly the remaining amount
        newPayment.payment_date = LocalDate.now();

        User user = new User();
        user.id = 10L;
        user.role = Role.CLIENT;

        Reservation reservation = new Reservation();
        reservation.total_price = new BigDecimal("200.00");
        reservation.status = ReservationStatus.PENDING;
        reservation.client_id = 10L;

        when(reservationRepository.getReservationById(reservationId)).thenReturn(Optional.of(reservation));
        // For this case, assume that 150 has already been paid.
        when(paymentRepository.getTotalPaid(reservationId)).thenReturn(new BigDecimal("150.00"));
        // Simulate payment creation
        when(paymentRepository.createPayment(newPayment)).thenReturn(7L);

        // Act & Assert
        long savedPaymentId = reservationUseCase.createPayment(newPayment, reservationId, user);
        assertEquals(7L, savedPaymentId);
        // Verify that the payment's date and reservationId are set
        assertNotNull(newPayment.payment_date);
        assertEquals(reservationId, newPayment.reservation_id);
        // Since 150 + 50 = 200, the status is updated to CONFIRMED
        assertEquals(ReservationStatus.CONFIRMED, reservation.status);
        verify(paymentRepository).createPayment(newPayment);
        verify(reservationRepository).updateReservation(reservation);
    }

    /**
     * Tests that createPayment() processes a partial payment and does not update the reservation status.
     */
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
        // Assume that 100 has already been paid, so remaining = 100 and 50 < 100.
        when(paymentRepository.getTotalPaid(reservationId)).thenReturn(new BigDecimal("100.00"));
        when(paymentRepository.createPayment(newPayment)).thenReturn(7L);

        // Act & Assert
        long savedPaymentId = reservationUseCase.createPayment(newPayment, reservationId, user);

        assertEquals(7L, savedPaymentId);
        // Reservation remains in PENDING status
        assertEquals(ReservationStatus.PENDING, reservation.status);

        verify(paymentRepository).createPayment(newPayment);
        verify(reservationRepository, never()).updateReservation(any());
    }

    /**
     * Tests that getPaymentsByClient() returns the correct list of payments for the given client ID.
     */
    @Test
    public void getPaymentsByClient_success() {
        // Arrange: Create a list of payments and simulate its return for the given client ID
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