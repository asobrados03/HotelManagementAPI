package com.alfre.DHHotel;

import com.alfre.DHHotel.adapter.security.jwt.JwtService;
import com.alfre.DHHotel.domain.model.*;
import com.alfre.DHHotel.domain.repository.RoomRepository;
import com.alfre.DHHotel.domain.repository.UserRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RoomManagementIntegrationTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private RoomRepository roomRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private static String adminToken;
    private Long testRoomId;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Container
    static MariaDBContainer<?> mariaDB = new MariaDBContainer<>(DockerImageName.parse("mariadb:10.6.5"))
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test1234");

    static {
        mariaDB.start();
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mariaDB::getJdbcUrl);
        registry.add("spring.datasource.username", mariaDB::getUsername);
        registry.add("spring.datasource.password", mariaDB::getPassword);
    }

    @BeforeAll
    void initializeUsers() {
        userRepository.getAllUsers().forEach(u -> userRepository.deleteUser(u.id));

        User admin = new User();
        admin.email = "admin@test.com";
        admin.password = passwordEncoder.encode("password");
        admin.role = Role.SUPERADMIN;
        admin.id = userRepository.createUser(admin);
        adminToken = jwtService.getToken(admin);
    }

    @BeforeEach
    void setupTestData() {
        roomRepository.deleteAll();

        Room room = new Room();
        room.room_number = 101;
        room.type = RoomType.DOUBLE;
        room.price_per_night = BigDecimal.valueOf(150.00);
        room.status = RoomStatus.AVAILABLE;
        testRoomId = roomRepository.createRoom(room);
    }

    // -------------------------
    // GET /api/admin/rooms
    // -------------------------
    @Test
    void testGetAllRooms_Success() throws Exception {
        mockMvc.perform(get("/api/admin/rooms")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].room_number").value(101));
    }

    @Test
    void testGetAllRooms_Empty_ReturnsNotFound() throws Exception {
        roomRepository.deleteAll();
        mockMvc.perform(get("/api/admin/rooms")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No hay habitaciones registrados en el sistema."));
    }

    // -------------------------
    // GET /api/admin/room/{id}
    // -------------------------
    @Test
    void testGetRoomById_Success() throws Exception {
        mockMvc.perform(get("/api/admin/room/" + testRoomId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("DOUBLE"));
    }

    @Test
    void testGetRoomById_NotFound() throws Exception {
        mockMvc.perform(get("/api/admin/room/999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(content().string("La habitación solicitada no existe."));
    }

    @Test
    void testGetRoomById_InvalidIdFormat_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/admin/room/notanumber")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    // -------------------------
    // POST /api/admin/room
    // -------------------------
    @Test
    void testCreateRoom_Success() throws Exception {
        Room newRoom = new Room();
        newRoom.room_number = 102;
        newRoom.type = RoomType.SINGLE;
        newRoom.price_per_night = BigDecimal.valueOf(100.00);
        newRoom.status = RoomStatus.AVAILABLE;

        mockMvc.perform(post("/api/admin/room")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newRoom)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isNumber());
    }

    @Test
    void testCreateRoom_InvalidPrice_ReturnsBadRequest() throws Exception {
        Room invalidRoom = new Room();
        invalidRoom.room_number = 103;
        invalidRoom.price_per_night = BigDecimal.valueOf(-50.00);

        mockMvc.perform(post("/api/admin/room")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRoom)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("El precio debe ser mayor que 0"));
    }

    @Test
    void testCreateRoom_MissingRequiredFields_ReturnsBadRequest() throws Exception {
        Room invalidRoom = new Room(); // Sin room_number, type, etc.

        mockMvc.perform(post("/api/admin/room")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRoom)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateRoom_DuplicateRoomNumber_ReturnsError() throws Exception {
        Room duplicateRoom = new Room();
        duplicateRoom.room_number = 101; // Ya existe en setup
        duplicateRoom.type = RoomType.DOUBLE;
        duplicateRoom.price_per_night = BigDecimal.valueOf(150.00);

        mockMvc.perform(post("/api/admin/room")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRoom)))
                .andExpect(status().isBadRequest()); // O BadRequest según implementación
    }

    @Test
    void testCreateRoom_ZeroPrice_ReturnsBadRequest() throws Exception {
        Room invalidRoom = new Room();
        invalidRoom.room_number = 102;
        invalidRoom.price_per_night = BigDecimal.ZERO;

        mockMvc.perform(post("/api/admin/room")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRoom)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("El precio debe ser mayor que 0"));
    }

    // -------------------------
    // PUT /api/admin/room/{id}
    // -------------------------
    @Test
    void testUpdateRoom_Success() throws Exception {
        Room updatedRoom = new Room();
        updatedRoom.price_per_night = BigDecimal.valueOf(200.00);
        updatedRoom.setStatus(RoomStatus.AVAILABLE);
        updatedRoom.setRoom_number(123);
        updatedRoom.setType(RoomType.DOUBLE);

        mockMvc.perform(put("/api/admin/room/" + testRoomId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedRoom)))
                .andExpect(status().isOk())
                .andExpect(content().string("La actualización se ha hecho correctamente"));
    }

    @Test
    void testUpdateRoom_NotFound_ReturnsBadRequest() throws Exception {
        Room updatedRoom = new Room();
        updatedRoom.price_per_night = BigDecimal.valueOf(200.00);

        mockMvc.perform(put("/api/admin/room/999")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatedRoom)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testUpdateStatus_InvalidStatus_ReturnsBadRequest() throws Exception {
        mockMvc.perform(put("/api/admin/rooms/" + testRoomId + "/status/INVALIDO")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    // -------------------------
    // DELETE /api/admin/room/{id}
    // -------------------------
    @Test
    void testDeleteRoom_Success() throws Exception {
        mockMvc.perform(delete("/api/admin/room/" + testRoomId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().string("La habitación con id: " + testRoomId + " se ha eliminado correctamente"));
    }

    @Test
    void testDeleteRoom_NotFound_ReturnsInternalServerError() throws Exception {
        mockMvc.perform(delete("/api/admin/room/999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testDeleteRoom_NonExistentId_ReturnsInternalError() throws Exception {
        mockMvc.perform(delete("/api/admin/room/9999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isInternalServerError());
    }

    // -------------------------
    // GET /api/public/rooms/type/{type}
    // -------------------------
    @Test
    void testGetRoomsByType_Success() throws Exception {
        mockMvc.perform(get("/api/public/rooms/type/DOUBLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].room_number").value(101));
    }

    @Test
    void testGetRoomsByType_Empty_ReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/public/rooms/type/SUITE"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No hay habitaciones del tipo solicitado en el sistema."));
    }

    @Test
    void testGetRoomsByType_InvalidType_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/public/rooms/type/INVALIDO"))
                .andExpect(status().isBadRequest());
    }

    // -------------------------
    // GET /api/public/rooms/available
    // -------------------------
    @Test
    void testGetAvailableRooms_Success() throws Exception {
        mockMvc.perform(get("/api/public/rooms/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));
    }

    @Test
    void testGetAvailableRooms_Empty_ReturnsNotFound() throws Exception {
        roomRepository.deleteAll();
        mockMvc.perform(get("/api/public/rooms/available"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No hay habitaciones disponibles en el sistema."));
    }

    // -------------------------
    // PUT /api/admin/rooms/{id}/status/{status}
    // -------------------------
    @Test
    void testUpdateStatus_Success() throws Exception {
        mockMvc.perform(put("/api/admin/rooms/" + testRoomId + "/status/MAINTENANCE")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().string("La actualización se ha hecho correctamente"));

        Room updated = roomRepository.getRoomById(testRoomId)
                .orElseThrow(() -> new RuntimeException("Habitación no encontrada"));
        assertThat(updated.status).isEqualTo(RoomStatus.MAINTENANCE);
    }

    @Test
    void updateStatus_NonExistentRoomId_ReturnsInternalError() throws Exception {
        mockMvc.perform(put("/api/admin/rooms/9999/status/MAINTENANCE")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("No se ha podido actualizar."));
    }

    @Test
    void updateStatus_InvalidRoomIdFormat_ReturnsBadRequest() throws Exception {
        mockMvc.perform(put("/api/admin/rooms/invalid/status/MAINTENANCE")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    // -------------------------
    // GET /api/admin/rooms/maintenance
    // -------------------------
    @Test
    void testGetRoomsInMaintenance_Success() throws Exception {
        roomRepository.updateStatus(testRoomId, RoomStatus.MAINTENANCE);

        mockMvc.perform(get("/api/admin/rooms/maintenance")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("MAINTENANCE"));
    }

    @Test
    void getRoomsInMaintenance_UnauthorizedAccess_ReturnsForbidden() throws Exception {
        // Crear usuario con rol USER
        User user = new User();
        user.email = "testuser@example.com";
        user.password = passwordEncoder.encode("password");
        user.role = Role.CLIENT;
        userRepository.createUser(user);
        String userToken = jwtService.getToken(user);

        mockMvc.perform(get("/api/admin/rooms/maintenance")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getRoomsInMaintenance_InvalidAuthHeader_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/rooms/maintenance")
                        .header("Authorization", "InvalidHeader"))
                .andExpect(status().isForbidden());
    }

    // -------------------------
    // Security Tests
    // -------------------------
    @Test
    void testAdminEndpoints_Unauthorized_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/rooms"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/room")
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testAdminEndpoints_WithUserRole_ReturnsForbidden() throws Exception {
        // Crear usuario con rol CLIENT
        User user = new User();
        user.email = "user@test.com";
        user.password = passwordEncoder.encode("password");
        user.role = Role.CLIENT;
        userRepository.createUser(user);
        String userToken = jwtService.getToken(user);

        mockMvc.perform(get("/api/admin/rooms")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testPublicEndpoints_WithoutToken_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/public/rooms/type/DOUBLE"))
                .andExpect(status().isOk());
    }
}