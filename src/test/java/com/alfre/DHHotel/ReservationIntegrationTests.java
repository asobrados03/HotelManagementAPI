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

    @DynamicPropertySource
    public static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mariaDB::getJdbcUrl);
        registry.add("spring.datasource.username", mariaDB::getUsername);
        registry.add("spring.datasource.password", mariaDB::getPassword);
    }

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

    @BeforeEach
    public void setupTestData() {
        // Limpiar todas las tablas relacionadas
        paymentRepository.deleteAll();
        reservationRepository.deleteAll();
        roomRepository.deleteAll();
        clientRepository.deleteAll();

        // Crear Room
        Room room = new Room();
        room.room_number = (int) (Math.random() * 1000) + 100; // Número único
        room.type = RoomType.DOUBLE;
        room.price_per_night = BigDecimal.valueOf(150.00);
        room.status = RoomStatus.AVAILABLE;
        testRoomId = roomRepository.createRoom(room);

        // Crear Client
        Client client = new Client();
        client.user_id = userRepository.getUserByEmail("client@test.com")
                .orElseThrow(() -> new RuntimeException("Usuario cliente no encotrado")).id;
        client.first_name = "Test";
        client.last_name = "Client";
        client.phone = "+34123456789";
        testClientId = clientRepository.createClient(client);

        // Crear Reservation
        Reservation reservation = new Reservation();
        reservation.client_id = testClientId;
        reservation.room_id = testRoomId;
        reservation.start_date = Date.from(Instant.now().plusSeconds(86400)); // Mañana
        reservation.end_date = Date.from(Instant.now().plusSeconds(172800));  // Pasado mañana
        reservation.total_price = BigDecimal.valueOf(300.00);
        reservation.status = ReservationStatus.PENDING;
        testReservationId = reservationRepository.createReservation(reservation);
    }

    @Test
    public void testGetAllReservations_Success() throws Exception {
        // Ejecutar y Verificar
        mockMvc.perform(get("/api/admin/reservations")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].total_price").value(300.00));
    }

    @Test
    public void testGetAllReservations_Empty_ReturnsNotFound() throws Exception {
        reservationRepository.deleteAll();

        // Ejecutar y Verificar
        mockMvc.perform(get("/api/admin/reservations")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No hay reservas registradas en el sistema."));
    }

    @Test
    public void testGetReservationById_Success() throws Exception {
        // Ejecutar y Verificar
        mockMvc.perform(get("/api/admin/reservation/" + testReservationId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    public void testGetReservationById_NotFound() throws Exception {
        // Ejecutar y Verificar
        mockMvc.perform(get("/api/admin/reservation/999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(content().string("La reserva solicitada no existe"));
    }

    @Test
    public void testGetClientReservations_Success() throws Exception {
        // Ejecutar y Verificar
        mockMvc.perform(get("/api/client/reservations/my")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].client_id").value(testClientId));
    }

    @Test
    public void testGetClientReservationsFailure_NotFound() throws Exception {
        // 1. Crear usuario y cliente vinculados
        User clientUser = new User();
        clientUser.email = "new_client@test.com";
        clientUser.password = passwordEncoder.encode("password");
        clientUser.role = Role.CLIENT;
        clientUser.id = userRepository.createUser(clientUser);

        // Crear registro en tabla Client
        Client client = new Client();
        client.user_id = clientUser.id; // ¡Vincular al usuario!
        client.first_name = "New";
        client.last_name = "Client";
        client.phone = "+34666777888";
        clientRepository.createClient(client);

        String newClientToken = jwtService.getToken(clientUser);

        // 2. Ejecutar y verificar
        mockMvc.perform(get("/api/client/reservations/my")
                        .header("Authorization", "Bearer " + newClientToken))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No hay reservas asociadas al cliente registradas en el sistema."));
    }

    @Test
    public void testCreateReservation_Success() throws Exception {
        Reservation newReservation = new Reservation();
        newReservation.room_id = testRoomId;
        newReservation.start_date = Date.from(Instant.now().plusSeconds(259200)); // +3 días
        newReservation.end_date = Date.from(Instant.now().plusSeconds(345600));  // +4 días

        // Ejecutar y Verificar
        mockMvc.perform(post("/api/client/reservation")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newReservation)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNumber());
    }

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

        // Ejecutar y Verificar
        mockMvc.perform(post("/api/client/reservation")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newReservation)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("No se puede reservar una habitación en mantenimiento."));
    }

    @Test
    public void testCreateReservation_Failure_InvalidDates() throws Exception {
        Reservation newReservation = new Reservation();
        newReservation.room_id = testRoomId;
        newReservation.start_date = new Date(System.currentTimeMillis() + 172800000);
        newReservation.end_date = new Date(System.currentTimeMillis() + 86400000); // Fecha final anterior

        // Ejecutar y Verificar
        mockMvc.perform(post("/api/client/reservation")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newReservation)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$")
                        .value(containsString("La fecha de salida no puede ser anterior a la fecha de entrada.")));
    }

    @Test
    public void testCreateReservation_Failure_RoomNotAvailable() throws Exception {
        // Crear cliente válido para la reserva conflictiva
        Client conflictClient = new Client();
        conflictClient.first_name = "Conflict";
        conflictClient.last_name = "Client";
        conflictClient.phone = "+34666111222";
        conflictClient.user_id = userRepository.getUserByEmail("client@test.com")
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado")).id;
        long conflictClientId = clientRepository.createClient(conflictClient);

        // Crear reserva que solapa las fechas CON CLIENTE VÁLIDO
        Reservation conflictingReservation = new Reservation();
        conflictingReservation.room_id = testRoomId;
        conflictingReservation.client_id = conflictClientId;  // Usar ID válido
        conflictingReservation.start_date = new Date(System.currentTimeMillis() - 86400000);
        conflictingReservation.end_date = new Date(System.currentTimeMillis() + 86400000);
        conflictingReservation.total_price = BigDecimal.valueOf(300.00);
        conflictingReservation.status = ReservationStatus.PENDING;
        reservationRepository.createReservation(conflictingReservation);

        // Crear nueva reserva con CLIENTE VÁLIDO (usar testClientId del setup)
        Reservation newReservation = new Reservation();
        newReservation.room_id = testRoomId;
        newReservation.client_id = testClientId;  // Usar ID del cliente de prueba principal
        newReservation.start_date = new Date();
        newReservation.end_date = new Date(System.currentTimeMillis() + 86400000);
        newReservation.total_price = BigDecimal.valueOf(300.00);
        newReservation.status = ReservationStatus.PENDING;

        // Ejecutar y Verificar
        mockMvc.perform(post("/api/client/reservation")
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newReservation)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Habitación no disponible en las fechas solicitadas"));
    }

    @Test
    public void testUpdateReservation_Success() throws Exception {
        Reservation updated = new Reservation();
        updated.start_date = Date.from(Instant.now().plusSeconds(172800));
        updated.end_date = Date.from(Instant.now().plusSeconds(259200));

        // Ejecutar y Verificar
        mockMvc.perform(put("/api/reservation/" + testReservationId)
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(content().string("La actualización se ha hecho correctamente"));
    }

    @Test
    public void testUpdateReservation_Failure_UpdateConfirmedReservation() throws Exception {
        // Primero confirmar la reserva
        Reservation reservation = reservationRepository.getReservationById(testReservationId)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));
        reservation.status = ReservationStatus.CONFIRMED;
        reservationRepository.updateReservation(reservation);

        Reservation updateRequest = new Reservation();
        updateRequest.start_date = new Date();
        updateRequest.end_date = new Date(System.currentTimeMillis() + 86400000);

        // Ejecutar y Verificar
        mockMvc.perform(put("/api/reservation/" + testReservationId)
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content()
                        .string("No se pueden modificar reservas canceladas o confirmadas si eres cliente."));
    }

    @Test
    public void testUpdateReservation_Failure_MissingDates() throws Exception {
        Reservation updateRequest = new Reservation();
        // Fechas no proporcionadas

        // Ejecutar y Verificar
        mockMvc.perform(put("/api/reservation/" + testReservationId)
                        .header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content()
                        .string("Hay que indicar las fechas de inicio y salida de la reserva"));
    }

    @Test
    public void testCancelReservation_Success() throws Exception {
        // Ejecutar y Verificar
        mockMvc.perform(delete("/api/admin/reservation/" + testReservationId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().string("La cancelación se ha hecho correctamente"));

        Reservation canceled = reservationRepository.getReservationById(testReservationId)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));
        assertThat(canceled.status).isEqualTo(ReservationStatus.CANCELED);
    }

    @Test
    public void testCancelReservation_Failure_CancelConfirmedReservation() throws Exception {
        // Confirmar reserva primero
        Reservation reservation = reservationRepository.getReservationById(testReservationId)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));
        reservation.status = ReservationStatus.CONFIRMED;
        reservationRepository.updateReservation(reservation);

        // Ejecutar y Verificar
        mockMvc.perform(delete("/api/admin/reservation/" + testReservationId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("No se puede cancelar una reserva confirmada."));
    }

    @Test
    public void testCancelReservation_Failure_UnauthorizedUser() throws Exception {
        // Ejecutar y Verificar
        mockMvc.perform(delete("/api/admin/reservation/" + testReservationId)
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testCreatePayment_Success() throws Exception {
        Payment payment = new Payment();
        payment.amount = BigDecimal.valueOf(150.00);
        payment.method = MethodPayment.CARD;

        // Ejecutar y Verificar
        mockMvc.perform(post("/api/admin/reservation/" + testReservationId + "/payment")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNumber());
    }

    @Test
    public void testCreatePayment_Failure_PaymentExceedsTotal() throws Exception {
        Payment payment = new Payment();
        payment.amount = BigDecimal.valueOf(600.00); // Mayor que el total de 500
        payment.method = MethodPayment.CARD;

        // Ejecutar y Verificar
        mockMvc.perform(post("/api/admin/reservation/" + testReservationId + "/payment")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payment)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("El pago excede el monto pendiente"));
    }

    @Test
    public void testCreatePayment_Failure_CanceledReservation() throws Exception {
        // Cancelar reserva primero
        Reservation reservation = reservationRepository.getReservationById(testReservationId)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

        reservation.setStatus(ReservationStatus.CANCELED);

        reservationRepository.updateReservation(reservation);

        Payment payment = new Payment();
        payment.amount = BigDecimal.valueOf(100.00);
        payment.method = MethodPayment.CARD;

        // Ejecutar y Verificar
        mockMvc.perform(post("/api/admin/reservation/" + testReservationId + "/payment")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payment)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("No se pueden registrar pagos para reservas canceladas"));
    }

    @Test
    public void testCreatePayment_Failure_ConfirmedReservationAlreadyPaid() throws Exception {
        // 1. Crear una reserva directamente en estado CONFIRMED
        Reservation confirmedReservation = new Reservation();
        confirmedReservation.room_id = testRoomId;
        confirmedReservation.client_id = testClientId;
        confirmedReservation.start_date = new Date(System.currentTimeMillis() + 86400000); // +1 día
        confirmedReservation.end_date = new Date(System.currentTimeMillis() + 172800000);   // +2 días
        confirmedReservation.total_price = new BigDecimal("400.00");
        confirmedReservation.status = ReservationStatus.CONFIRMED; // Estado inicial: CONFIRMED
        long reservationId = reservationRepository.createReservation(confirmedReservation);

        // 2. Asociar un pago completo a la reserva (opcional, pero recomendado para consistencia)
        Payment fullPayment = new Payment();
        fullPayment.reservation_id = reservationId;
        fullPayment.amount = new BigDecimal("400.00");
        fullPayment.method = MethodPayment.CARD;
        fullPayment.payment_date = new Date(System.currentTimeMillis());
        paymentRepository.createPayment(fullPayment);

        // 3. Intentar agregar un pago adicional
        Payment extraPayment = new Payment();
        extraPayment.reservation_id = reservationId;
        extraPayment.amount = new BigDecimal("100.00");
        extraPayment.method = MethodPayment.CASH;

        // Ejecutar y Verificar
        mockMvc.perform(post("/api/admin/reservation/" + reservationId + "/payment")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(extraPayment)))
                .andExpect(status().isBadRequest())
                .andExpect(content()
                        .string("El pago no se ha realizado ya que la reserva ya está pagada y confirmada"));
    }

    @Test
    public void testGetPaymentsByClient_Success() throws Exception {
        // 1. Crear un cliente con pagos
        Client client = new Client();
        client.first_name = "PaymentTest";
        client.last_name = "Client";
        client.phone = "+34666777888";
        client.user_id = userRepository.getUserByEmail("client@test.com")
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado")).id;
        long clientId = clientRepository.createClient(client);

        // 2. Crear reserva y pagos asociados
        Reservation reservation = new Reservation();
        reservation.room_id = testRoomId;
        reservation.client_id = clientId;
        reservation.total_price = new BigDecimal("500.00");
        reservation.status = ReservationStatus.CONFIRMED;
        reservation.start_date = new Date(System.currentTimeMillis() + 86400000); // +1 día
        reservation.end_date = new Date(System.currentTimeMillis() + 172800000);   // +2 días
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

        // 3. Ejecutar y verificar
        mockMvc.perform(get("/api/admin/reservations/" + clientId + "/payments")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].amount").value(300.00));
    }

    @Test
    public void testGetPaymentsByClient_NoPayments_ReturnsNotFound() throws Exception {
        // 1. Crear cliente sin pagos
        Client client = new Client();
        client.first_name = "NoPayments";
        client.last_name = "Client";
        client.phone = "+34666999888";
        client.user_id = userRepository.getUserByEmail("client@test.com")
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado")).id;
        long clientId = clientRepository.createClient(client);

        // 2. Ejecutar y verificar
        mockMvc.perform(get("/api/admin/reservations/" + clientId + "/payments")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No hay pagos asociados al cliente registrados en el sistema."));
    }

    @Test
    public void testGetPaymentsByClient_ClientNotFound_ReturnsNotFound() throws Exception {
        // Ejecutar y Verificar
        mockMvc.perform(get("/api/admin/reservations/999/payments")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No hay pagos asociados al cliente registrados en el sistema."));
    }

    @Test
    public void testSecurity_AllProtectedEndpoints_Unauthorized_ReturnsForbidden() throws Exception {
        // Todos los endpoints protegidos con sus métodos
        String[][] protectedEndpoints = {
                // Endpoints ADMIN
                {"/api/admin/reservations", "GET"},
                {"/api/admin/reservation/1", "GET"},
                {"/api/admin/reservation/1/payment", "POST"},
                {"/api/admin/reservations/1/payments", "GET"},
                {"/api/admin/reservation/1", "DELETE"},

                // Endpoints CLIENT
                {"/api/client/reservations/my", "GET"},
                {"/api/client/reservation", "POST"},
                {"/api/reservation/1", "PUT"}
        };

        for (String[] endpoint : protectedEndpoints) {
            mockMvc.perform(request(HttpMethod.valueOf(endpoint[1]), endpoint[0]))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    public void testSecurity_ClientEndpoints_AdminAccess_ReturnsForbidden() throws Exception {
        // Endpoints CLIENT que no deben ser accesibles por ADMIN
        mockMvc.perform(get("/api/client/reservations/my")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/client/reservation")
                        .header("Authorization", "Bearer " + adminToken)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testSecurity_UpdateReservation_CrossClientAccess_ReturnsForbidden() throws Exception {
        // 1. Crear usuario y cliente COMPLETAMENTE aislados
        User otherUser = new User();
        otherUser.email = "otherclient@test.com";
        otherUser.password = passwordEncoder.encode("password");
        otherUser.role = Role.CLIENT;
        otherUser.id = userRepository.createUser(otherUser); // ¡Asignar ID!

        Client otherClient = new Client();
        otherClient.user_id = otherUser.id; // ¡Vincular correctamente!
        otherClient.first_name = "Other";
        otherClient.last_name = "Client";
        otherClient.phone = "+34666555444";
        clientRepository.createClient(otherClient);

        // 2. Generar token con el ID correcto
        String otherClientToken = jwtService.getToken(otherUser);

        // 3. Ejecutar petición con cuerpo completo
        Reservation originalReservation = reservationRepository
                .getReservationById(testReservationId)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

        // Preparar el objeto de actualización e incluir el client_id de la reserva original
        Reservation updateRequest = new Reservation();
        updateRequest.client_id = originalReservation.client_id; // Asigna el client_id real
        updateRequest.start_date = new Date();
        updateRequest.end_date = new Date(System.currentTimeMillis() + 86400000);
        updateRequest.room_id = testRoomId; // Asegurar que la habitación existe

        // Ejecutar y Verificar
        mockMvc.perform(put("/api/reservation/" + testReservationId)
                        .header("Authorization", "Bearer " + otherClientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden()); // Se espera 403 Forbidden
    }

    @Test
    public void testSecurity_PaymentEndpoints_ClientAccess_ReturnsForbidden() throws Exception {
        // Endpoints de pagos que solo ADMIN puede acceder
        mockMvc.perform(post("/api/admin/reservation/1/payment")
                        .header("Authorization", "Bearer " + clientToken)
                        .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/reservations/1/payments")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isForbidden());
    }
}