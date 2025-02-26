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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * This class contains the attributes and methods for realize the integration tests
 * of the payments operations.
 *
 * @author Alfredo Sobrados González
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PaymentIntegrationTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private RoomRepository roomRepository;


    private static String adminToken;
    private Long testReservationId;
    private Long testPaymentId;
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
     * Initializes the super administrator user for the test suite.
     * <p>
     * This method clears all existing users, creates a new super admin with a preset email,
     * encrypted password, and role, and then generates a JWT token for authentication.
     * </p>
     */
    @BeforeAll
    public void initializeUsers() {
        userRepository.getAllUsers().forEach(u -> userRepository.deleteUser(u.id));

        User super_admin = new User();
        super_admin.setEmail("superadmin@test.com");
        super_admin.setPassword(passwordEncoder.encode("password"));
        super_admin.setRole(Role.SUPERADMIN);
        super_admin.setId(userRepository.createUser(super_admin));
        adminToken = jwtService.getToken(super_admin);
    }

    /**
     * Sets up the test data before each test.
     * <p>
     * This method clears the payment, reservation, room, and client repositories in the proper order
     * (to respect dependencies). It then deletes all users except the super admin, creates a new room,
     * a new client user with its associated client record, and finally creates a reservation and a payment.
     * </p>
     */
    @BeforeEach
    public void setupTestData() {
        // Clean all tables in the correct order (dependent entities first)
        paymentRepository.deleteAll();
        reservationRepository.deleteAll();
        roomRepository.deleteAll();
        clientRepository.deleteAll();

        // Delete all users except the superadmin
        userRepository.getAllUsers().stream()
                .filter(u -> !u.email.equals("superadmin@test.com"))
                .forEach(u -> userRepository.deleteUser(u.id));

        // Create a Room with a unique room number
        Room newRoom = new Room();
        newRoom.setStatus(RoomStatus.AVAILABLE);
        newRoom.setRoom_number((int) (Math.random() * 1000) + 100); // Random room number
        newRoom.setType(RoomType.DOUBLE);
        newRoom.setPrice_per_night(BigDecimal.valueOf(120.00));
        long roomId = roomRepository.createRoom(newRoom);

        // Create a new user for a client
        User newUser = new User();
        newUser.setEmail("test@example.com");
        newUser.setRole(Role.CLIENT);
        newUser.setPassword(passwordEncoder.encode("password"));
        long userId = userRepository.createUser(newUser);

        // Create the client record associated with the new user
        Client newClient = new Client();
        newClient.setFirst_name("First");
        newClient.setLast_name("Last");
        newClient.setPhone("+34 656 122 356");
        newClient.setUser_id(userId);
        long clientId = clientRepository.createClient(newClient);

        // Create a reservation for the client and room
        Reservation reservation = new Reservation();
        reservation.setTotal_price(new BigDecimal("500.00"));
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setClient_id(clientId);
        reservation.setRoom_id(roomId);
        reservation.setStart_date(new Date(1000));
        reservation.setEnd_date(new Date(4000));
        testReservationId = reservationRepository.createReservation(reservation);

        // Create a payment for the reservation
        Payment payment = new Payment();
        payment.setReservation_id(testReservationId);
        payment.setAmount(new BigDecimal("200.00"));
        payment.setMethod(MethodPayment.CARD);

        LocalDate today = LocalDate.now();
        payment.setPayment_date(Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        testPaymentId = paymentRepository.createPayment(payment);
    }

    /**
     * Tests that retrieving all payments returns a successful response with the correct payment data.
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    public void testGetAllPayments_Success() throws Exception {
        // Execute and verify that the payment amount is correctly returned.
        mockMvc.perform(get("/api/admin/payments")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount").value(200.00));
    }

    /**
     * Tests that retrieving all payments when there are no payments returns a Not Found status.
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    public void testGetAllPayments_EmptyList_ReturnsNotFound() throws Exception {
        // Prepare: Delete all payments.
        paymentRepository.deleteAll();

        // Execute and verify that the correct error message is returned.
        mockMvc.perform(get("/api/admin/payments")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No hay pagos registrados en el sistema."));
    }

    /**
     * Tests that retrieving a payment by its ID returns the correct payment details.
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    public void testGetPaymentById_Success() throws Exception {
        // Execute and verify that the payment method is "CARD".
        mockMvc.perform(get("/api/admin/payment/" + testPaymentId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("CARD"));
    }

    /**
     * Tests that retrieving a payment with a non-existent ID returns a Not Found error.
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    public void testGetPaymentById_NotFound() throws Exception {
        // Execute and verify that the correct error message is returned.
        mockMvc.perform(get("/api/admin/payment/999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(content().string("El pago solicitado no existe"));
    }

    /**
     * Tests that retrieving payments by reservation ID returns the correct payments.
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    public void testGetPaymentsByReservationId_Success() throws Exception {
        // Execute and verify that the payment amount for the reservation is correctly returned.
        mockMvc.perform(get("/api/admin/payment/reservation/id/" + testReservationId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount").value(200.00));
    }

    /**
     * Tests that retrieving payments by reservation ID when no payments exist returns a Not Found status.
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    public void testGetPaymentsByReservationId_EmptyList() throws Exception {
        // Prepare: Delete all payments.
        paymentRepository.deleteAll();

        // Execute and verify that the correct error message is returned.
        mockMvc.perform(get("/api/admin/payment/reservation/id/" + testReservationId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No hay pagos registrados asociados a la reserva en el sistema."));
    }

    /**
     * Tests that updating a payment with a valid amount is successful.
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    public void testUpdatePayment_Success() throws Exception {
        // Prepare: Create a Payment object with the updated amount.
        Payment updatedPayment = new Payment();
        updatedPayment.setAmount(new BigDecimal("300.00"));

        // Execute and verify the update request.
        mockMvc.perform(put("/api/superadmin/payment/" + testPaymentId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedPayment)))
                .andExpect(status().isOk())
                .andExpect(content().string("La actualización se ha hecho correctamente"));

        // Verify that the associated reservation status remains unchanged (PENDING).
        Reservation reservation = reservationRepository.getReservationById(testReservationId)
                .orElseThrow();
        assertThat(reservation.status).isEqualTo(ReservationStatus.PENDING);
    }

    /**
     * Tests that updating a payment with an amount that exceeds the total reservation price returns an error.
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    public void testUpdatePayment_ExceedTotal_ReturnsError() throws Exception {
        // Prepare: Create a Payment object with an excessive amount.
        Payment updatedPayment = new Payment();
        updatedPayment.setAmount(new BigDecimal("600.00"));

        // Execute and verify that the update fails with an Internal Server Error.
        mockMvc.perform(put("/api/superadmin/payment/" + testPaymentId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedPayment)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$").value("El importe del pago excede el precio total de la reserva asociada"));
    }

    /**
     * Tests that deleting a payment is successful and that the payment record is removed.
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    public void testDeletePayment_Success() throws Exception {
        // Execute the DELETE request and verify success.
        mockMvc.perform(delete("/api/superadmin/payment/" + testPaymentId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().string("El pago con id: " + testPaymentId + " se ha eliminado correctamente"));

        // Verify that the payment no longer exists.
        assertThat(paymentRepository.getPaymentById(testPaymentId)).isEmpty();
    }

    /**
     * Tests that attempting to delete a non-existent payment returns an Internal Server Error.
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    public void testDeletePayment_infoNotFound_ReturnsInternalServerError() throws Exception {
        // Execute and verify that the proper error message is returned when deleting a non-existent payment.
        mockMvc.perform(delete("/api/superadmin/payment/999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error de servicio."));
    }

    /**
     * Tests that payment endpoints return Forbidden when accessed without proper authorization.
     * <p>
     * This test verifies that endpoints for getting, updating, and deleting payments cannot be accessed
     * without the correct authentication token.
     * </p>
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    public void testPaymentEndpoints_Unauthorized_ReturnsForbidden() throws Exception {
        // Execute and verify GET request for all payments without authentication.
        mockMvc.perform(get("/api/admin/payments"))
                .andExpect(status().isForbidden());

        // Execute and verify PUT request for updating a payment without authentication.
        mockMvc.perform(put("/api/superadmin/payment/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        // Execute and verify GET request for a specific payment without authentication.
        mockMvc.perform(get("/api/superadmin/payment/1"))
                .andExpect(status().isForbidden());

        // Execute and verify DELETE request for a payment without authentication.
        mockMvc.perform(delete("/api/superadmin/payment/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        // Execute and verify GET request for payments by reservation id without authentication.
        mockMvc.perform(get("/api/admin/payment/reservation/id/2"))
                .andExpect(status().isForbidden());
    }
}