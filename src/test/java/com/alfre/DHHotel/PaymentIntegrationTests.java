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

    @DynamicPropertySource
    public static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mariaDB::getJdbcUrl);
        registry.add("spring.datasource.username", mariaDB::getUsername);
        registry.add("spring.datasource.password", mariaDB::getPassword);
    }

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

    @BeforeEach
    public void setupTestData() {
        // Limpiar todas las tablas en orden correcto (dependencias primero)
        paymentRepository.deleteAll();
        reservationRepository.deleteAll();
        roomRepository.deleteAll();
        clientRepository.deleteAll();

        // Eliminar usuarios excepto superadmin
        userRepository.getAllUsers().stream()
                .filter(u -> !u.email.equals("superadmin@test.com"))
                .forEach(u -> userRepository.deleteUser(u.id));

        // Crear Room con número único
        Room newRoom = new Room();
        newRoom.setStatus(RoomStatus.AVAILABLE);
        newRoom.setRoom_number((int) (Math.random() * 1000) + 100); // Número aleatorio
        newRoom.setType(RoomType.DOUBLE);
        newRoom.setPrice_per_night(BigDecimal.valueOf(120.00));
        long roomId = roomRepository.createRoom(newRoom);

        User newUser = new User();
        newUser.setEmail("test@example.com");
        newUser.setRole(Role.CLIENT);
        newUser.setPassword(passwordEncoder.encode("password"));
        long userId = userRepository.createUser(newUser);

        Client newClient = new Client();
        newClient.setFirst_name("First");
        newClient.setLast_name("Last");
        newClient.setPhone("+34 656 122 356");
        newClient.setUser_id(userId);
        long clientId = clientRepository.createClient(newClient);

        Reservation reservation = new Reservation();
        reservation.setTotal_price(new BigDecimal("500.00"));
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setClient_id(clientId);
        reservation.setRoom_id(roomId);
        reservation.setStart_date(new Date(1000));
        reservation.setEnd_date(new Date(4000));
        testReservationId = reservationRepository.createReservation(reservation);

        Payment payment = new Payment();
        payment.setReservation_id(testReservationId);
        payment.setAmount(new BigDecimal("200.00"));
        payment.setMethod(MethodPayment.CARD);

        LocalDate today = LocalDate.now();

        payment.setPayment_date(Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        testPaymentId = paymentRepository.createPayment(payment);
    }

    @Test
    public void testGetAllPayments_Success() throws Exception {
        // Ejecutar y Verificar
        mockMvc.perform(get("/api/admin/payments")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount").value(200.00));
    }

    @Test
    public void testGetAllPayments_EmptyList_ReturnsNotFound() throws Exception {
        // Preparar
        paymentRepository.deleteAll();

        // Ejecutar y Verificar
        mockMvc.perform(get("/api/admin/payments")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No hay pagos registrados en el sistema."));
    }

    @Test
    public void testGetPaymentById_Success() throws Exception {
        // Ejecutar y Verificar
        mockMvc.perform(get("/api/admin/payment/" + testPaymentId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("CARD"));
    }

    @Test
    public void testGetPaymentById_NotFound() throws Exception {
        // Ejecutar y Verificar
        mockMvc.perform(get("/api/admin/payment/999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(content().string("El pago solicitado no existe"));
    }

    @Test
    public void testGetPaymentsByReservationId_Success() throws Exception {
        // Ejecutar y Verificar
        mockMvc.perform(get("/api/admin/payment/reservation/id/" + testReservationId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount").value(200.00));
    }

    @Test
    public void testGetPaymentsByReservationId_EmptyList() throws Exception {
        // Preparar
        paymentRepository.deleteAll();

        // Ejecutar y Verificar
        mockMvc.perform(get("/api/admin/payment/reservation/id/" + testReservationId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No hay pagos registrados asociados a la reserva en el sistema."));
    }

    @Test
    public void testUpdatePayment_Success() throws Exception {
        // Preparar
        Payment updatedPayment = new Payment();
        updatedPayment.setAmount(new BigDecimal("300.00"));

        // Ejecutar y Verificar
        mockMvc.perform(put("/api/superadmin/payment/" + testPaymentId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedPayment)))
                .andExpect(status().isOk())
                .andExpect(content().string("La actualización se ha hecho correctamente"));

        Reservation reservation = reservationRepository.getReservationById(testReservationId)
                .orElseThrow();
        assertThat(reservation.status).isEqualTo(ReservationStatus.PENDING);
    }

    @Test
    public void testUpdatePayment_ExceedTotal_ReturnsError() throws Exception {
        // Preparar
        Payment updatedPayment = new Payment();
        updatedPayment.setAmount(new BigDecimal("600.00"));

        // Ejecutar y Verificar
        mockMvc.perform(put("/api/superadmin/payment/" + testPaymentId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedPayment)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$").value("El importe del pago excede el precio total de la reserva asociada"));
    }

    @Test
    public void testDeletePayment_Success() throws Exception {
        // Ejecutar y Verificar
        mockMvc.perform(delete("/api/superadmin/payment/" + testPaymentId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().string("El pago con id: " + testPaymentId + " se ha eliminado correctamente"));

        assertThat(paymentRepository.getPaymentById(testPaymentId)).isEmpty();
    }

    @Test
    public void testDeletePayment_infoNotFound_ReturnsInternalServerError() throws Exception {
        // Ejecutar y Verificar
        mockMvc.perform(delete("/api/superadmin/payment/999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Error de servicio."));
    }

    @Test
    public void testPaymentEndpoints_Unauthorized_ReturnsForbidden() throws Exception {
        // Ejecutar y Verificar
        mockMvc.perform(get("/api/admin/payments"))
                .andExpect(status().isForbidden());

        // Ejecutar y Verificar
        mockMvc.perform(put("/api/superadmin/payment/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        // Ejecutar y Verificar
        mockMvc.perform(get("/api/superadmin/payment/1"))
                .andExpect(status().isForbidden());

        // Ejecutar y Verificar
        mockMvc.perform(delete("/api/superadmin/payment/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        // Ejecutar y Verificar
        mockMvc.perform(get("/api/admin/payment/reservation/id/2"))
                .andExpect(status().isForbidden());
    }
}