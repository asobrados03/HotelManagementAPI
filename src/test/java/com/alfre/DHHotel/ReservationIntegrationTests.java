package com.alfre.DHHotel;

import com.alfre.DHHotel.adapter.security.jwt.JwtService;
import com.alfre.DHHotel.domain.model.*;
import com.alfre.DHHotel.domain.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * This class contains the attributes and methods for realize the integration tests
 * of the reservations operations.
 *
 * @author Alfredo Sobrados González
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ReservationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;

    private static String adminToken;
    private static String clientToken;
    private Long testReservationId;
    private Long testRoomId;
    private Long testClientId;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Container
    public static MariaDBContainer<?> mariaDB = new MariaDBContainer<>(DockerImageName.parse("mariadb:10.6.5"))
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test1234");

    static {
        mariaDB.start();
    }

    /**
     * Configures dynamic properties for database connection using the MariaDB TestContainers container.
     * <p>
     * This method registers the URL, username, and password obtained from the MariaDB container in the
     * {@link DynamicPropertyRegistry}, so that Spring Boot correctly configures the data source in the test context.
     * </p>
     * @param registry the dynamic property record where the database connection properties are added
     */
    @DynamicPropertySource
    public static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mariaDB::getJdbcUrl);
        registry.add("spring.datasource.username", mariaDB::getUsername);
        registry.add("spring.datasource.password", mariaDB::getPassword);
    }

    /**
     * Initializes superadmin and client users before all tests.
     * <p>
     * This method clears all existing users, then creates a superadmin and a client user.
     * It encodes their passwords, sets their roles, persists them, and generates their JWT tokens.
     * </p>
     */
    @BeforeAll
    public void initializeUsers() {
        userRepository.getAllUsers().forEach(u -> userRepository.deleteUser(u.id));

        // Superadmin
        User superadmin = new User();
        superadmin.email = "superadmin@test.com";
        superadmin.password = passwordEncoder.encode("password");
        superadmin.role = Role.SUPERADMIN;
        superadmin.id = userRepository.createUser(superadmin);
        adminToken = jwtService.getToken(superadmin);

        // Client
        User clientUser = new User();
        clientUser.email = "client@test.com";
        clientUser.password = passwordEncoder.encode("password");
        clientUser.role = Role.CLIENT;
        clientUser.id = userRepository.createUser(clientUser);
        clientToken = jwtService.getToken(clientUser);
    }

    /**
     * Sets up the test data before each test.
     * <p>
     * This method clears all related tables (payments, reservations, rooms, and clients).
     * Then it creates a new room, a client record (using the client user), and a new reservation.
     * </p>
     */
    @BeforeEach
    public void setupTestData() {
        // Clear all related tables
        paymentRepository.deleteAll();
        reservationRepository.deleteAll();
        roomRepository.deleteAll();
        clientRepository.deleteAll();

        // Create Room
        Room room = new Room();
        room.room_number = (int) (Math.random() * 1000) + 100; // Unique room number
        room.type = RoomType.DOUBLE;
        room.price_per_night = BigDecimal.valueOf(150.00);
        room.status = RoomStatus.AVAILABLE;
        testRoomId = roomRepository.createRoom(room);

        // Create Client record using the client user
        Client client = new Client();
        client.user_id = userRepository.getUserByEmail("client@test.com")
                .orElseThrow(() -> new RuntimeException("Client user not found")).id;
        client.first_name = "Test";
        client.last_name = "Client";
        client.phone = "+34123456789";
        testClientId = clientRepository.createClient(client);

        // Create Reservation
        Reservation reservation = new Reservation();
        reservation.client_id = testClientId;
        reservation.room_id = testRoomId;
        reservation.start_date = Date.from(Instant.now().plusSeconds(86400)); // Tomorrow
        reservation.end_date = Date.from(Instant.now().plusSeconds(172800));  // Day after tomorrow
        reservation.total_price = BigDecimal.valueOf(300.00);
        reservation.status = ReservationStatus.PENDING;
        testReservationId = reservationRepository.createReservation(reservation);
    }

    /**
     * Tests that retrieving all reservations returns a successful response.
     * <p>
     * Sends a GET request to "/api/admin/reservations" and expects a list with a reservation
     * having a total price of 300.00.
     * </p>
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    public void testGetAllReservations_Success() throws Exception {
        mockMvc.perform(get("/api/admin/reservations")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].total_price").value(300.00));
    }

    /**
     * Tests that retrieving all reservations when none exist returns a Not Found status.
     * <p>
     * Deletes all reservations and then sends a GET request to "/api/admin/reservations",
     * expecting a 404 status with an appropriate message.
     * </p>
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    public void testGetAllReservations_Empty_ReturnsNotFound() throws Exception {
        reservationRepository.deleteAll();

        mockMvc.perform(get("/api/admin/reservations")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No hay reservas registradas en el sistema."));
    }

    /**
     * Tests that retrieving a reservation by its ID returns the correct reservation details.
     * <p>
     * Sends a GET request to "/api/admin/reservation/{id}" and expects the reservation status to be "PENDING".
     * </p>
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    public void testGetReservationById_Success() throws Exception {
        mockMvc.perform(get("/api/admin/reservation/" + testReservationId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    /**
     * Tests that retrieving a reservation with a non-existent ID returns a Not Found error.
     * <p>
     * Sends a GET request to "/api/admin/reservation/999" and expects a 404 status with an error message.
     * </p>
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    public void testGetReservationById_NotFound() throws Exception {
        mockMvc.perform(get("/api/admin/reservation/999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(content().string("La reserva solicitada no existe"));
    }

    /**
     * Tests that a client can successfully retrieve their own reservations.
     * <p>
     * Sends a GET request to "/api/client/reservations/my" using the client token and expects
     * the reservation to have the correct client_id.
     * </p>
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    public void testGetClientReservations_Success() throws Exception {
        mockMvc.perform(get("/api/client/reservations/my")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].client_id").value(testClientId));
    }

    /**
     * Tests that retrieving client reservations when none exist returns Not Found.
     * <p>
     * Creates a new client with no reservations and sends a GET request to "/api/client/reservations/my",
     * expecting a 404 status and an appropriate message.
     * </p>
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    public void testGetClientReservationsFailure_NotFound() throws Exception {
        // 1. Create a new client user
        User clientUser = new User();
        clientUser.email = "new_client@test.com";
        clientUser.password = passwordEncoder.encode("password");
        clientUser.role = Role.CLIENT;
        clientUser.id = userRepository.createUser(clientUser);

        // Create Client record for the new user
        Client client = new Client();
        client.user_id = clientUser.id;
        client.first_name = "New";
        client.last_name = "Client";
        client.phone = "+34666777888";
        clientRepository.createClient(client);

        String newClientToken = jwtService.getToken(clientUser);

        // 2. Execute and verify
        mockMvc.perform(get("/api/client/reservations/my")
                        .header("Authorization", "Bearer " + newClientToken))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No hay reservas asociadas al cliente registradas en el sistema."));
    }

    /**
     * Tests that creating a reservation is successful.
     * <p>
     * Sends a POST request to "/api/client/reservation" with valid reservation details and expects a numeric ID in response.
     * </p>
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    public void testCreateReservation_Success() throws Exception {
        Reservation newReservation = new Reservation();
        newReservation.room_id = testRoomId;
        newReservation.start_date = Date.from(Instant.now().plusSeconds(259200)); // +3 days
        newReservation.end_date = Date.from(Instant.now().plusSeconds(345600));  // +4 days

        mockMvc.perform(post("/api/client/reservation")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newReservation)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNumber());
    }

    /**
     * Tests that creating a reservation fails when the room is in maintenance.
     * <p>
     * Creates a room with MAINTENANCE status and attempts to reserve it, expecting a 400 Bad Request with an error message.
     * </p>
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    public void testCreateReservation_Failure_RoomInMaintenance() throws Exception {
        Room room = new Room();
        room.room_number = 999;
        room.status = RoomStatus.MAINTENANCE;
        room.type = RoomType.SINGLE;
        room.price_per_night = BigDecimal.valueOf(100.00);
        long maintenanceRoomId = roomRepository.createRoom(room);

        Reservation newReservation = new Reservation();
        newReservation.room_id = maintenanceRoomId;
        newReservation.start_date = new Date(System.currentTimeMillis() + 86400000);
        newReservation.end_date = new Date(System.currentTimeMillis() + 172800000);

        mockMvc.perform(post("/api/client/reservation")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newReservation)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("No se puede reservar una habitación en mantenimiento."));
    }

    /**
     * Tests that creating a reservation fails when the end date is before the start date.
     * <p>
     * Sends a POST request with invalid dates and expects a 400 Bad Request with an error message.
     * </p>
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    public void testCreateReservation_Failure_InvalidDates() throws Exception {
        Reservation newReservation = new Reservation();
        newReservation.room_id = testRoomId;
        newReservation.start_date = new Date(System.currentTimeMillis() + 172800000);
        newReservation.end_date = new Date(System.currentTimeMillis() + 86400000); // End date before start date

        mockMvc.perform(post("/api/client/reservation")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newReservation)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value(containsString("La fecha de salida no puede ser anterior a la fecha de entrada.")));
    }

    /**
     * Tests that creating a reservation fails when the room is not available due to a conflicting reservation.
     * <p>
     * Creates a conflicting reservation for the same room and then attempts to create a new reservation,
     * expecting a 400 Bad Request with an error message.
     * </p>
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    public void testCreateReservation_Failure_RoomNotAvailable() throws Exception {
        // Create a valid client for the conflicting reservation
        Client conflictClient = new Client();
        conflictClient.first_name = "Conflict";
        conflictClient.last_name = "Client";
        conflictClient.phone = "+34666111222";
        conflictClient.user_id = userRepository.getUserByEmail("client@test.com")
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado")).id;
        long conflictClientId = clientRepository.createClient(conflictClient);

        // Create a conflicting reservation overlapping the dates
        Reservation conflictingReservation = new Reservation();
        conflictingReservation.room_id = testRoomId;
        conflictingReservation.client_id = conflictClientId;
        conflictingReservation.start_date = new Date(System.currentTimeMillis() - 86400000);
        conflictingReservation.end_date = new Date(System.currentTimeMillis() + 86400000);
        conflictingReservation.total_price = BigDecimal.valueOf(300.00);
        conflictingReservation.status = ReservationStatus.PENDING;
        reservationRepository.createReservation(conflictingReservation);

        // Create a new reservation with the test client that conflicts with the existing reservation
        Reservation newReservation = new Reservation();
        newReservation.room_id = testRoomId;
        newReservation.client_id = testClientId;
        newReservation.start_date = new Date();
        newReservation.end_date = new Date(System.currentTimeMillis() + 86400000);
        newReservation.total_price = BigDecimal.valueOf(300.00);
        newReservation.status = ReservationStatus.PENDING;

        mockMvc.perform(post("/api/client/reservation")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newReservation)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Habitación no disponible en las fechas solicitadas"));
    }

    /**
     * Tests that updating a reservation is successful.
     * <p>
     * Sends a PUT request to update the start and end dates of an existing reservation,
     * and expects a success message.
     * </p>
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    public void testUpdateReservation_Success() throws Exception {
        Reservation updated = new Reservation();
        updated.start_date = Date.from(Instant.now().plusSeconds(172800));
        updated.end_date = Date.from(Instant.now().plusSeconds(259200));

        mockMvc.perform(put("/api/reservation/" + testReservationId)
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(content().string("La actualización se ha hecho correctamente"));
    }

    /**
     * Tests that updating a confirmed reservation fails for a client.
     * <p>
     * First confirms the reservation, then attempts to update it using a PUT request.
     * Expects a 400 Bad Request with an error message.
     * </p>
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    public void testUpdateReservation_Failure_UpdateConfirmedReservation() throws Exception {
        // Confirm the reservation first
        Reservation reservation = reservationRepository.getReservationById(testReservationId)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));
        reservation.status = ReservationStatus.CONFIRMED;
        reservationRepository.updateReservation(reservation);

        Reservation updateRequest = new Reservation();
        updateRequest.start_date = new Date();
        updateRequest.end_date = new Date(System.currentTimeMillis() + 86400000);

        mockMvc.perform(put("/api/reservation/" + testReservationId)
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("No se pueden modificar reservas canceladas o confirmadas si eres cliente."));
    }

    /**
     * Tests that updating a reservation fails when the required dates are missing.
     * <p>
     * Sends a PUT request without providing start and end dates and expects a 400 Bad Request with an error message.
     * </p>
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    public void testUpdateReservation_Failure_MissingDates() throws Exception {
        Reservation updateRequest = new Reservation();
        // Dates not provided

        mockMvc.perform(put("/api/reservation/" + testReservationId)
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Hay que indicar las fechas de inicio y salida de la reserva"));
    }

    /**
     * Tests that canceling a reservation is successful.
     * <p>
     * Sends a DELETE request to "/api/admin/reservation/{id}" and expects the reservation status to change to CANCELED.
     * </p>
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    public void testCancelReservation_Success() throws Exception {
        mockMvc.perform(delete("/api/admin/reservation/" + testReservationId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().string("La cancelación se ha hecho correctamente"));

        Reservation canceled = reservationRepository.getReservationById(testReservationId)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));
        assertThat(canceled.status).isEqualTo(ReservationStatus.CANCELED);
    }

    /**
     * Tests that canceling a confirmed reservation fails.
     * <p>
     * First confirms the reservation, then sends a DELETE request and expects a 400 Bad Request with an error message.
     * </p>
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    public void testCancelReservation_Failure_CancelConfirmedReservation() throws Exception {
        Reservation reservation = reservationRepository.getReservationById(testReservationId)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));
        reservation.status = ReservationStatus.CONFIRMED;
        reservationRepository.updateReservation(reservation);

        mockMvc.perform(delete("/api/admin/reservation/" + testReservationId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("No se puede cancelar una reserva confirmada."));
    }

    /**
     * Tests that canceling a reservation with a client token returns Forbidden.
     * <p>
     * Sends a DELETE request with a client token and expects a 403 Forbidden status.
     * </p>
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    public void testCancelReservation_Failure_UnauthorizedUser() throws Exception {
        mockMvc.perform(delete("/api/admin/reservation/" + testReservationId)
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isForbidden());
    }

    /**
     * Tests that creating a payment for a reservation is successful.
     * <p>
     * Sends a POST request to "/api/admin/reservation/{id}/payment" with valid payment details and expects a numeric ID.
     * </p>
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    public void testCreatePayment_Success() throws Exception {
        Payment payment = new Payment();
        payment.amount = BigDecimal.valueOf(150.00);
        payment.method = MethodPayment.CARD;

        mockMvc.perform(post("/api/admin/reservation/" + testReservationId + "/payment")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNumber());
    }

    /**
     * Tests that creating a payment fails if the payment amount exceeds the pending total.
     * <p>
     * Sends a POST request with a payment amount greater than the reservation total and expects a 400 Bad Request with an error message.
     * </p>
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    public void testCreatePayment_Failure_PaymentExceedsTotal() throws Exception {
        Payment payment = new Payment();
        payment.amount = BigDecimal.valueOf(600.00); // Exceeds the total of 500
        payment.method = MethodPayment.CARD;

        mockMvc.perform(post("/api/admin/reservation/" + testReservationId + "/payment")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payment)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("El pago excede el monto pendiente"));
    }

    /**
     * Tests that creating a payment fails for a canceled reservation.
     * <p>
     * Cancels the reservation first, then sends a POST request for payment creation,
     * expecting a 400 Bad Request with an appropriate error message.
     * </p>
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    public void testCreatePayment_Failure_CanceledReservation() throws Exception {
        Reservation reservation = reservationRepository.getReservationById(testReservationId)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

        reservation.setStatus(ReservationStatus.CANCELED);
        reservationRepository.updateReservation(reservation);

        Payment payment = new Payment();
        payment.amount = BigDecimal.valueOf(100.00);
        payment.method = MethodPayment.CARD;

        mockMvc.perform(post("/api/admin/reservation/" + testReservationId + "/payment")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payment)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("No se pueden registrar pagos para reservas canceladas"));
    }

    /**
     * Tests that creating an extra payment fails when the reservation is already fully paid and confirmed.
     * <p>
     * Creates a confirmed reservation with a full payment, then attempts to add an extra payment,
     * expecting a 400 Bad Request with an appropriate error message.
     * </p>
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    public void testCreatePayment_Failure_ConfirmedReservationAlreadyPaid() throws Exception {
        // 1. Create a reservation in CONFIRMED state
        Reservation confirmedReservation = new Reservation();
        confirmedReservation.room_id = testRoomId;
        confirmedReservation.client_id = testClientId;
        confirmedReservation.start_date = new Date(System.currentTimeMillis() + 86400000); // +1 day
        confirmedReservation.end_date = new Date(System.currentTimeMillis() + 172800000);   // +2 days
        confirmedReservation.total_price = new BigDecimal("400.00");
        confirmedReservation.status = ReservationStatus.CONFIRMED;
        long reservationId = reservationRepository.createReservation(confirmedReservation);

        // 2. Associate a full payment to the reservation
        Payment fullPayment = new Payment();
        fullPayment.reservation_id = reservationId;
        fullPayment.amount = new BigDecimal("400.00");
        fullPayment.method = MethodPayment.CARD;
        fullPayment.payment_date = new Date(System.currentTimeMillis());
        paymentRepository.createPayment(fullPayment);

        // 3. Attempt to add an extra payment
        Payment extraPayment = new Payment();
        extraPayment.reservation_id = reservationId;
        extraPayment.amount = new BigDecimal("100.00");
        extraPayment.method = MethodPayment.CASH;

        mockMvc.perform(post("/api/admin/reservation/" + reservationId + "/payment")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(extraPayment)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("El pago no se ha realizado ya que la reserva ya está pagada y confirmada"));
    }

    /**
     * Tests that retrieving payments by client is successful.
     * <p>
     * Creates a client with a confirmed reservation and associated payments,
     * then sends a GET request to retrieve the payments and verifies the returned data.
     * </p>
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    public void testGetPaymentsByClient_Success() throws Exception {
        // 1. Create a client with payments
        Client client = new Client();
        client.first_name = "PaymentTest";
        client.last_name = "Client";
        client.phone = "+34666777888";
        client.user_id = userRepository.getUserByEmail("client@test.com")
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado")).id;
        long clientId = clientRepository.createClient(client);

        // 2. Create a reservation and associated payments
        Reservation reservation = new Reservation();
        reservation.room_id = testRoomId;
        reservation.client_id = clientId;
        reservation.total_price = new BigDecimal("500.00");
        reservation.status = ReservationStatus.CONFIRMED;
        reservation.start_date = new Date(System.currentTimeMillis() + 86400000); // +1 day
        reservation.end_date = new Date(System.currentTimeMillis() + 172800000);   // +2 days
        long reservationId = reservationRepository.createReservation(reservation);

        Payment payment1 = new Payment();
        payment1.reservation_id = reservationId;
        payment1.amount = new BigDecimal("300.00");
        payment1.method = MethodPayment.CARD;
        payment1.payment_date = new Date(System.currentTimeMillis());
        paymentRepository.createPayment(payment1);

        Payment payment2 = new Payment();
        payment2.reservation_id = reservationId;
        payment2.amount = new BigDecimal("200.00");
        payment2.method = MethodPayment.CASH;
        payment2.payment_date = new Date(System.currentTimeMillis());
        paymentRepository.createPayment(payment2);

        mockMvc.perform(get("/api/admin/reservations/" + clientId + "/payments")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].amount").value(300.00));
    }

    /**
     * Tests that retrieving payments by client returns Not Found when there are no payments.
     * <p>
     * Creates a client with no payments, then sends a GET request and expects a 404 status with an error message.
     * </p>
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    public void testGetPaymentsByClient_NoPayments_ReturnsNotFound() throws Exception {
        // 1. Create a client without payments
        Client client = new Client();
        client.first_name = "NoPayments";
        client.last_name = "Client";
        client.phone = "+34666999888";
        client.user_id = userRepository.getUserByEmail("client@test.com")
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado")).id;
        long clientId = clientRepository.createClient(client);

        mockMvc.perform(get("/api/admin/reservations/" + clientId + "/payments")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No hay pagos asociados al cliente registrados en el sistema."));
    }

    /**
     * Tests that retrieving payments by client for a non-existent client returns Not Found.
     * <p>
     * Sends a GET request with an invalid client ID and expects a 404 status with an appropriate error message.
     * </p>
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    public void testGetPaymentsByClient_ClientNotFound_ReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/admin/reservations/999/payments")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No hay pagos asociados al cliente registrados en el sistema."));
    }

    /**
     * Tests that all protected endpoints return Forbidden when accessed without authorization.
     * <p>
     * Iterates over a list of endpoints (with their HTTP methods) and verifies that each returns a 403 Forbidden status when no token is provided.
     * </p>
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    public void testSecurity_AllProtectedEndpoints_Unauthorized_ReturnsForbidden() throws Exception {
        // Endpoints that require authentication: {endpoint, HTTP method}
        String[][] protectedEndpoints = {
                // ADMIN endpoints
                {"/api/admin/reservations", "GET"},
                {"/api/admin/reservation/1", "GET"},
                {"/api/admin/reservation/1/payment", "POST"},
                {"/api/admin/reservations/1/payments", "GET"},
                {"/api/admin/reservation/1", "DELETE"},

                // CLIENT endpoints
                {"/api/client/reservations/my", "GET"},
                {"/api/client/reservation", "POST"},
                {"/api/reservation/1", "PUT"}
        };

        for (String[] endpoint : protectedEndpoints) {
            mockMvc.perform(request(HttpMethod.valueOf(endpoint[1]), endpoint[0]))
                    .andExpect(status().isForbidden());
        }
    }

    /**
     * Tests that admin users cannot access client endpoints.
     * <p>
     * Sends requests to client endpoints using an admin token and expects a 403 Forbidden status.
     * </p>
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    public void testSecurity_ClientEndpoints_AdminAccess_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/client/reservations/my")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/client/reservation")
                        .header("Authorization", "Bearer " + adminToken)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    /**
     * Tests that a client cannot update a reservation that belongs to another client.
     * <p>
     * Creates a separate client, generates a token for that client, and attempts to update a reservation
     * belonging to the original test client. Expects a 403 Forbidden status.
     * </p>
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    public void testSecurity_UpdateReservation_CrossClientAccess_ReturnsForbidden() throws Exception {
        // 1. Create an isolated client user
        User otherUser = new User();
        otherUser.email = "otherclient@test.com";
        otherUser.password = passwordEncoder.encode("password");
        otherUser.role = Role.CLIENT;
        otherUser.id = userRepository.createUser(otherUser);

        Client otherClient = new Client();
        otherClient.user_id = otherUser.id;
        otherClient.first_name = "Other";
        otherClient.last_name = "Client";
        otherClient.phone = "+34666555444";
        clientRepository.createClient(otherClient);

        // 2. Generate token for the new client
        String otherClientToken = jwtService.getToken(otherUser);

        // 3. Prepare update request with the original reservation details
        Reservation originalReservation = reservationRepository
                .getReservationById(testReservationId)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

        Reservation updateRequest = new Reservation();
        updateRequest.client_id = originalReservation.client_id; // Original client id
        updateRequest.start_date = new Date();
        updateRequest.end_date = new Date(System.currentTimeMillis() + 86400000);
        updateRequest.room_id = testRoomId;

        mockMvc.perform(put("/api/reservation/" + testReservationId)
                        .header("Authorization", "Bearer " + otherClientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());
    }

    /**
     * Tests that payment endpoints are inaccessible to client users.
     * <p>
     * Sends POST and GET requests to payment-related endpoints using a client token and expects a 403 Forbidden status.
     * </p>
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    public void testSecurity_PaymentEndpoints_ClientAccess_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/admin/reservation/1/payment")
                        .header("Authorization", "Bearer " + clientToken)
                        .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/reservations/1/payments")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isForbidden());
    }
}