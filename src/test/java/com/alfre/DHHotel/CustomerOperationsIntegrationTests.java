package com.alfre.DHHotel;

import com.alfre.DHHotel.adapter.security.jwt.JwtService;
import com.alfre.DHHotel.domain.model.Client;
import com.alfre.DHHotel.domain.model.Role;
import com.alfre.DHHotel.domain.model.User;
import com.alfre.DHHotel.domain.repository.ClientRepository;
import com.alfre.DHHotel.domain.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * This class contains the attributes and methods for realize the integration tests
 * of the customers operations.
 *
 * @author Alfredo Sobrados González
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CustomerOperationsIntegrationTests {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;

    private static String adminToken;
    private static String clientToken;
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
     * Performs initial setup before all tests.
     * <p>
     * This method deletes all existing users, creates a default admin user and client user,
     * generates their JWT tokens, and creates a client record for the client user.
     * </p>
     */
    @BeforeAll
    public void setup() {
        userRepository.getAllUsers().forEach(u -> userRepository.deleteUser(u.id));

        User adminUser = createUser("admin@test.com", Role.SUPERADMIN);
        adminToken = jwtService.getToken(adminUser);

        User clientUser = createUser("client@test.com", Role.CLIENT);
        clientToken = jwtService.getToken(clientUser);
        createClient(clientUser.id, "Test", "Client", "+34123456789");
    }

    /**
     * Creates and persists a new user with the specified email and role.
     *
     * @param email the email address for the new user
     * @param role the role to assign to the new user
     * @return the created User instance with its generated id
     */
    private User createUser(String email, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("password"));
        user.setRole(role);
        user.setId(userRepository.createUser(user));
        return user;
    }

    /**
     * Creates and persists a new client record associated with the given user id.
     *
     * @param userId    the id of the user to associate with the client record
     * @param firstName the first name of the client
     * @param lastName  the last name of the client
     * @param phone     the phone number of the client
     */
    private void createClient(long userId, String firstName, String lastName, String phone) {
        Client client = new Client();
        client.setUser_id(userId);
        client.setFirst_name(firstName);
        client.setLast_name(lastName);
        client.setPhone(phone);
        long id = clientRepository.createClient(client);
        client.setId(id);
    }

    /**
     * Resets test data before each test.
     * <p>
     * This method deletes all client records and removes any user (except the default admin
     * and client users). If the default client user does not exist, it recreates the client user
     * and its associated client record.
     * </p>
     */
    @BeforeEach
    public void resetData() {
        clientRepository.deleteAll();
        userRepository.getAllUsers().stream()
                .filter(u -> !u.email.equals("admin@test.com") && !u.email.equals("client@test.com"))
                .forEach(u -> userRepository.deleteUser(u.id));
        Optional<User> clientUser = userRepository.getUserByEmail("client@test.com");
        if (clientUser.isEmpty()) {
            User newClient = createUser("client@test.com", Role.CLIENT);
            createClient(newClient.id, "Test", "Client", "+34123456789");
        }
    }

    /**
     * Tests that retrieving all clients returns a successful response when clients exist.
     * <p>
     * This test creates an additional client for the default client user and verifies that a GET request
     * to "/api/admin/clients" returns an array containing the expected client(s).
     * </p>
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    public void testGetAllClients_Success() throws Exception {
        // Prepare: Retrieve the default client user and add an extra client record.
        User clientUser = userRepository.getUserByEmail("client@test.com")
                .orElseThrow(() -> new RuntimeException("Client not found with email client@test.com"));
        createClient(clientUser.id, "Client1", "Last1", "+34111111111");

        // Execute and verify
        mockMvc.perform(get("/api/admin/clients")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    /**
     * Tests that retrieving all clients when no client records exist returns a Not Found status.
     * <p>
     * This test verifies that a GET request to "/api/admin/clients" returns a 404 status and an error
     * message when no clients are registered.
     * </p>
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    public void testGetAllClients_EmptyList_ReturnsNotFound() throws Exception {
        // Execute and verify
        mockMvc.perform(get("/api/admin/clients")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value("No hay clientes registrados en el sistema."));
    }

    /**
     * Tests that accessing the get-all-clients endpoint without authentication returns Forbidden.
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    public void testGetAllClients_WithoutAuth_ReturnsForbidden() throws Exception {
        // Execute and verify
        mockMvc.perform(get("/api/admin/clients"))
                .andExpect(status().isForbidden());
    }

    /**
     * Tests that retrieving a client by its id returns the correct client details.
     * <p>
     * This test creates a new client, retrieves it, and verifies that the GET request to
     * "/api/admin/client/{id}" returns the expected user id, first name, last name, and phone number.
     * </p>
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    public void testGetClientById_Success() throws Exception {
        // Prepare: Create a new client user and associated client record.
        User user = createUser("email@test.com", Role.CLIENT);
        createClient(user.id, "New", "Client", "+34 654 123 456");
        Client client = clientRepository.getClientByUserId(user.id)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        // Execute and verify
        mockMvc.perform(get("/api/admin/client/" + client.id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_id").value(user.id))
                .andExpect(jsonPath("$.first_name").value("New"))
                .andExpect(jsonPath("$.last_name").value("Client"))
                .andExpect(jsonPath("$.phone").value("+34 654 123 456"));
    }

    /**
     * Tests that retrieving a client by an invalid id returns a Not Found error.
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    public void testGetClientById_NotFound_ReturnsError() throws Exception {
        // Execute and verify
        mockMvc.perform(get("/api/admin/client/999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value("El cliente solicitado no existe"));
    }

    /**
     * Tests that deleting a client successfully removes the client and its associated user record.
     * <p>
     * This test creates a new client, sends a DELETE request to remove it, and verifies that both the
     * client record and the corresponding user record are deleted.
     * </p>
     *
     * @throws Exception if an error occurs during the deletion process
     */
    @Test
    public void testDeleteClient_Success() throws Exception {
        // Prepare: Create a new client user and associated client record.
        User newUser = createUser("newclient@test.com", Role.CLIENT);
        createClient(newUser.id, "New", "Client", "+34222222222");

        Client newClient = clientRepository.getClientByUserId(newUser.id)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        // Execute and verify the DELETE request
        mockMvc.perform(delete("/api/admin/client/" + newClient.id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().string("Cliente con id: " + newClient.id + "eliminado correctamente."));

        // Verify deletion in the repository
        assertThat(clientRepository.getClientById(newClient.id)).isEmpty();
        assertThat(userRepository.getUserById(newUser.id)).isEmpty();
    }

    /**
     * Tests that attempting to delete a non-existent client returns a Not Found error.
     *
     * @throws Exception if an error occurs during the deletion process
     */
    @Test
    public void testDeleteClient_NotFound_ReturnsError() throws Exception {
        // Execute and verify
        mockMvc.perform(delete("/api/admin/client/999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No existe el cliente que quieres eliminar"));
    }

    /**
     * Tests that admin-restricted endpoints return Forbidden when accessed with a client role.
     * <p>
     * This test verifies that a user with a client role cannot access endpoints reserved for admin users,
     * such as retrieving all clients, fetching a client by id, or deleting a client.
     * </p>
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    public void testClientEndpoints_WithClientRole_ReturnsForbidden() throws Exception {
        // Execute and verify GET request for all clients
        mockMvc.perform(get("/api/admin/clients")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isForbidden());

        // Execute and verify GET request for a specific client
        mockMvc.perform(get("/api/admin/client/1")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isForbidden());

        // Execute and verify DELETE request for a client
        mockMvc.perform(delete("/api/admin/client/1")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isForbidden());
    }
}
