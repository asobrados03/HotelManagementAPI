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

/**
 * This class contains the attributes and methods for realize the integration tests
 * of the rooms management operations.
 *
 * @author Alfredo Sobrados González
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RoomManagementIntegrationTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;

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

    /**
     * Configures dynamic properties for database connection using the MariaDB TestContainers container.
     * <p>
     * This method registers the URL, username, and password obtained from the MariaDB container in the
     * {@link DynamicPropertyRegistry}, so that Spring Boot correctly configures the data source in the test context.
     * </p>
     * @param registry the dynamic property record where the database connection properties are added
     */
    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mariaDB::getJdbcUrl);
        registry.add("spring.datasource.username", mariaDB::getUsername);
        registry.add("spring.datasource.password", mariaDB::getPassword);
    }

    /**
     * Initializes the admin user before all tests.
     * <p>
     * This method clears all existing users and creates an admin user with the SUPERADMIN role.
     * The user's password is encoded before persisting, and a JWT token is generated for authentication.
     * </p>
     */
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

    /**
     * Sets up test data before each test.
     * <p>
     * This method deletes all rooms, then creates a new room with a fixed room number (101),
     * a DOUBLE type, a nightly price of 150.00, and an AVAILABLE status.
     * The created room's ID is stored in {@code testRoomId}.
     * </p>
     */
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

    /**
     * Tests that retrieving all rooms returns a successful response.
     * <p>
     * Sends a GET request to "/api/admin/rooms" with a valid admin token and expects the room list to contain a room with room number 101.
     * </p>
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    void testGetAllRooms_Success() throws Exception {
        mockMvc.perform(get("/api/admin/rooms")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].room_number").value(101));
    }

    /**
     * Tests that retrieving all rooms returns Not Found when there are no rooms.
     * <p>
     * Deletes all rooms and sends a GET request to "/api/admin/rooms", expecting a 404 status and an appropriate message.
     * </p>
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    void testGetAllRooms_Empty_ReturnsNotFound() throws Exception {
        roomRepository.deleteAll();
        mockMvc.perform(get("/api/admin/rooms")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No hay habitaciones registrados en el sistema."));
    }

    /**
     * Tests that retrieving a room by its ID returns the expected room details.
     * <p>
     * Sends a GET request to "/api/admin/room/{id}" with the test room ID and expects the room type to be "DOUBLE".
     * </p>
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    void testGetRoomById_Success() throws Exception {
        mockMvc.perform(get("/api/admin/room/" + testRoomId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("DOUBLE"));
    }

    /**
     * Tests that retrieving a room with a non-existent ID returns a Not Found error.
     * <p>
     * Sends a GET request to "/api/admin/room/999" and expects a 404 status with an appropriate error message.
     * </p>
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    void testGetRoomById_NotFound() throws Exception {
        mockMvc.perform(get("/api/admin/room/999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(content().string("La habitación solicitada no existe."));
    }

    /**
     * Tests that retrieving a room with an invalid ID format returns a Bad Request.
     * <p>
     * Sends a GET request to "/api/admin/room/notanumber" and expects a 400 Bad Request status.
     * </p>
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    void testGetRoomById_InvalidIdFormat_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/admin/room/notanumber")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    /**
     * Tests that creating a new room is successful.
     * <p>
     * Sends a POST request to "/api/admin/room" with a valid room object and expects a numeric response representing the new room ID.
     * </p>
     *
     * @throws Exception if an error occurs during the HTTP request
     */
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

    /**
     * Tests that creating a room with an invalid (negative) price returns a Bad Request.
     * <p>
     * Sends a POST request with a room object having a negative price and expects a 400 status with an error message.
     * </p>
     *
     * @throws Exception if an error occurs during the HTTP request
     */
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

    /**
     * Tests that creating a room with missing required fields returns a Bad Request.
     * <p>
     * Sends a POST request with an empty room object and expects a 400 Bad Request status.
     * </p>
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    void testCreateRoom_MissingRequiredFields_ReturnsBadRequest() throws Exception {
        Room invalidRoom = new Room(); // Missing room_number, type, etc.

        mockMvc.perform(post("/api/admin/room")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRoom)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Tests that creating a room with a duplicate room number returns an error.
     * <p>
     * Attempts to create a room with room number 101, which already exists from the setup.
     * Expects a 400 Bad Request (or similar error status based on implementation).
     * </p>
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    void testCreateRoom_DuplicateRoomNumber_ReturnsError() throws Exception {
        Room duplicateRoom = new Room();
        duplicateRoom.room_number = 101; // Already exists in setup
        duplicateRoom.type = RoomType.DOUBLE;
        duplicateRoom.price_per_night = BigDecimal.valueOf(150.00);

        mockMvc.perform(post("/api/admin/room")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRoom)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Tests that creating a room with a zero price returns a Bad Request.
     * <p>
     * Sends a POST request with a room object that has a price of zero and expects a 400 status with an error message.
     * </p>
     *
     * @throws Exception if an error occurs during the HTTP request
     */
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

    /**
     * Tests that updating an existing room is successful.
     * <p>
     * Sends a PUT request to update the room's price, status, room number, and type.
     * Expects a success message confirming the update.
     * </p>
     *
     * @throws Exception if an error occurs during the HTTP request
     */
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

    /**
     * Tests that updating a room that does not exist returns an error.
     * <p>
     * Sends a PUT request to update a room with an ID of 999 and expects an Internal Server Error.
     * </p>
     *
     * @throws Exception if an error occurs during the HTTP request
     */
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

    /**
     * Tests that updating the room status with an invalid status value returns a Bad Request.
     * <p>
     * Sends a PUT request to update the room status to an invalid value ("INVALIDO") and expects a 400 status.
     * </p>
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    void testUpdateStatus_InvalidStatus_ReturnsBadRequest() throws Exception {
        mockMvc.perform(put("/api/admin/rooms/" + testRoomId + "/status/INVALIDO")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    /**
     * Tests that deleting an existing room is successful.
     * <p>
     * Sends a DELETE request for the test room and expects a success message confirming the deletion.
     * </p>
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    void testDeleteRoom_Success() throws Exception {
        mockMvc.perform(delete("/api/admin/room/" + testRoomId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().string("La habitación con id: " + testRoomId + " se ha eliminado correctamente"));
    }

    /**
     * Tests that attempting to delete a room with a non-existent ID returns an error.
     * <p>
     * Sends a DELETE request for a room with ID 999 and expects an Internal Server Error.
     * </p>
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    void testDeleteRoom_NotFound_ReturnsInternalServerError() throws Exception {
        mockMvc.perform(delete("/api/admin/room/999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isInternalServerError());
    }

    /**
     * Tests that attempting to delete a room with an invalid non-existent ID returns an error.
     * <p>
     * Sends a DELETE request for a room with ID 9999 and expects an Internal Server Error.
     * </p>
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    void testDeleteRoom_NonExistentId_ReturnsInternalError() throws Exception {
        mockMvc.perform(delete("/api/admin/room/9999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isInternalServerError());
    }

    /**
     * Tests that retrieving rooms by type is successful.
     * <p>
     * Sends a GET request to "/api/public/rooms/type/DOUBLE" and expects a room with room number 101.
     * </p>
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    void testGetRoomsByType_Success() throws Exception {
        mockMvc.perform(get("/api/public/rooms/type/DOUBLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].room_number").value(101));
    }

    /**
     * Tests that retrieving rooms by a type that does not exist returns Not Found.
     * <p>
     * Sends a GET request to "/api/public/rooms/type/SUITE" and expects a 404 status with an appropriate message.
     * </p>
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    void testGetRoomsByType_Empty_ReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/public/rooms/type/SUITE"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No hay habitaciones del tipo solicitado en el sistema."));
    }

    /**
     * Tests that retrieving rooms by an invalid room type returns a Bad Request.
     * <p>
     * Sends a GET request to "/api/public/rooms/type/INVALIDO" and expects a 400 Bad Request status.
     * </p>
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    void testGetRoomsByType_InvalidType_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/public/rooms/type/INVALIDO"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Tests that retrieving available rooms is successful.
     * <p>
     * Sends a GET request to "/api/public/rooms/available" and expects the room status to be "AVAILABLE".
     * </p>
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    void testGetAvailableRooms_Success() throws Exception {
        mockMvc.perform(get("/api/public/rooms/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"));
    }

    /**
     * Tests that retrieving available rooms returns Not Found when none are available.
     * <p>
     * Deletes all rooms and sends a GET request to "/api/public/rooms/available",
     * expecting a 404 status with an appropriate message.
     * </p>
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    void testGetAvailableRooms_Empty_ReturnsNotFound() throws Exception {
        roomRepository.deleteAll();
        mockMvc.perform(get("/api/public/rooms/available"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No hay habitaciones disponibles en el sistema."));
    }

    /**
     * Tests that updating the room status is successful.
     * <p>
     * Sends a PUT request to update the status of the test room to MAINTENANCE.
     * Verifies that the update is successful and that the room's status is updated accordingly.
     * </p>
     *
     * @throws Exception if an error occurs during the HTTP request
     */
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

    /**
     * Tests that updating the status for a non-existent room ID returns an error.
     * <p>
     * Sends a PUT request to update the status of a room with ID 9999 and expects an Internal Server Error with a specific message.
     * </p>
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    void updateStatus_NonExistentRoomId_ReturnsInternalError() throws Exception {
        mockMvc.perform(put("/api/admin/rooms/9999/status/MAINTENANCE")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("No se ha podido actualizar."));
    }

    /**
     * Tests that updating the status with an invalid room ID format returns a Bad Request.
     * <p>
     * Sends a PUT request with a non-numeric room ID and expects a 400 Bad Request status.
     * </p>
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    void updateStatus_InvalidRoomIdFormat_ReturnsBadRequest() throws Exception {
        mockMvc.perform(put("/api/admin/rooms/invalid/status/MAINTENANCE")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    /**
     * Tests that retrieving rooms in maintenance is successful.
     * <p>
     * Updates the test room's status to MAINTENANCE, then sends a GET request to "/api/admin/rooms/maintenance"
     * and expects the room's status to be "MAINTENANCE".
     * </p>
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    void testGetRoomsInMaintenance_Success() throws Exception {
        roomRepository.updateStatus(testRoomId, RoomStatus.MAINTENANCE);

        mockMvc.perform(get("/api/admin/rooms/maintenance")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("MAINTENANCE"));
    }

    /**
     * Tests that accessing rooms in maintenance with an unauthorized (non-admin) user returns Forbidden.
     * <p>
     * Creates a user with the CLIENT role, generates a token, and sends a GET request to "/api/admin/rooms/maintenance".
     * Expects a 403 Forbidden status.
     * </p>
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    void getRoomsInMaintenance_UnauthorizedAccess_ReturnsForbidden() throws Exception {
        // Create a user with the CLIENT role
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

    /**
     * Tests that accessing rooms in maintenance with an invalid Authorization header returns Forbidden.
     * <p>
     * Sends a GET request with an invalid Authorization header and expects a 403 Forbidden status.
     * </p>
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    void getRoomsInMaintenance_InvalidAuthHeader_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/rooms/maintenance")
                        .header("Authorization", "InvalidHeader"))
                .andExpect(status().isForbidden());
    }

    /**
     * Tests that admin endpoints are protected and return Forbidden when accessed without a token.
     * <p>
     * Sends requests to admin endpoints without authentication and expects a 403 Forbidden status.
     * </p>
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    void testAdminEndpoints_Unauthorized_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/rooms"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/room")
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    /**
     * Tests that admin endpoints return Forbidden when accessed with a user having a CLIENT role.
     * <p>
     * Creates a user with the CLIENT role, generates a token, and sends a GET request to an admin endpoint.
     * Expects a 403 Forbidden status.
     * </p>
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    void testAdminEndpoints_WithUserRole_ReturnsForbidden() throws Exception {
        // Create a user with CLIENT role
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

    /**
     * Tests that public endpoints are accessible without an authentication token.
     * <p>
     * Sends a GET request to a public endpoint and expects a 200 OK status.
     * </p>
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    void testPublicEndpoints_WithoutToken_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/public/rooms/type/DOUBLE"))
                .andExpect(status().isOk());
    }
}