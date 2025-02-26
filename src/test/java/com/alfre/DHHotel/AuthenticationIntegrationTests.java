package com.alfre.DHHotel;

import com.alfre.DHHotel.adapter.security.jwt.JwtService;
import com.alfre.DHHotel.adapter.web.dto.LoginRequest;
import com.alfre.DHHotel.adapter.web.dto.RegisterRequest;
import com.alfre.DHHotel.adapter.web.dto.UpdateProfileRequest;
import com.alfre.DHHotel.adapter.web.dto.UserDTO;
import com.alfre.DHHotel.domain.model.Administrator;
import com.alfre.DHHotel.domain.model.Client;
import com.alfre.DHHotel.domain.model.User;
import com.alfre.DHHotel.domain.model.Role;
import com.alfre.DHHotel.domain.repository.AdministratorRepository;
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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * This class contains the attributes and methods for realize the integration tests
 * of the authentication and authorization operations.
 *
 * @author Alfredo Sobrados González
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AuthenticationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdministratorRepository administratorRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static String clientToken;
    private static String adminToken;

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
        registry.add("spring.datasource.url", () -> mariaDB.getJdbcUrl());
        registry.add("spring.datasource.username", mariaDB::getUsername);
        registry.add("spring.datasource.password", mariaDB::getPassword);
    }

    /**
     * Initializes the users required for authentication tests by cleaning up any existing users,
     * creating a client user and an administrator user, and generating their respective JWT tokens.
     */
    @BeforeAll
    public void initializeUsers() {
        // Initial cleanup of users
        userRepository.getAllUsers().forEach(u -> userRepository.deleteUser(u.id));

        // Create client user
        User clientUser = new User();
        clientUser.setEmail("client@test.com");
        clientUser.setPassword(passwordEncoder.encode("password"));
        clientUser.setRole(Role.CLIENT);
        long clientId = userRepository.createUser(clientUser);
        clientUser.setId(clientId); // Assign the returned id
        clientToken = jwtService.getToken(clientUser);

        // Create administrator user
        User adminUser = new User();
        adminUser.setEmail("admin@test.com");
        adminUser.setPassword(passwordEncoder.encode("password")); // It's recommended to encode the password
        adminUser.setRole(Role.SUPERADMIN);
        long adminId = userRepository.createUser(adminUser);
        adminUser.setId(adminId); // Assign the returned id
        adminToken = jwtService.getToken(adminUser);
    }

    /**
     * Ensures that an administrator user with the email "admin@test.com" exists before each test.
     * If the user is not found in the repository, this method creates one and generates its JWT token.
     */
    @BeforeEach
    public void ensureAdminUserExists() {
        // Check if an admin user with email "admin@test.com" exists
        Optional<User> adminOpt = userRepository.getAllUsers().stream()
                .filter(u -> u.email.equals("admin@test.com"))
                .findFirst();
        if (adminOpt.isEmpty()) {
            User adminUser = new User();
            adminUser.setEmail("admin@test.com");
            adminUser.setPassword(passwordEncoder.encode("password"));
            adminUser.setRole(Role.SUPERADMIN);
            long adminId = userRepository.createUser(adminUser);
            adminUser.setId(adminId); // Assign the returned identifier
            adminToken = jwtService.getToken(adminUser);
        }
    }

    /**
     * Resets the database state before each test by deleting all records from the administrator
     * and client repositories, ensuring a clean slate for each test case.
     */
    @BeforeEach
    public void resetDatabase() {
        administratorRepository.deleteAll();
        clientRepository.deleteAll();
    }

    /**
     * Tests that a client can successfully log in with valid credentials.
     *
     * @throws Exception if an error occurs during the login request.
     */
    @Test
    public void testLoginSuccess() throws Exception {
        // Prepare the login request
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("client@test.com");
        loginRequest.setPassword("password");

        String jsonContent = objectMapper.writeValueAsString(loginRequest);

        // Execute and verify
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                // Assumes that the response contains a "token" field
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    /**
     * Tests that logging in with invalid credentials (bad password) returns a Not Found status
     * along with an appropriate error message.
     *
     * @throws Exception if an error occurs during the login request.
     */
    @Test
    public void testLoginFailure_badCredentials_thenReturnsNotFound() throws Exception {
        // Prepare the login request with a wrong password
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("client@test.com");
        loginRequest.setPassword("wrongpassword");

        String jsonContent = objectMapper.writeValueAsString(loginRequest);

        // Execute and verify
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Credenciales inválidas"));
    }

    /**
     * Tests that attempting to log in with missing required fields returns a Bad Request status
     * along with an error message indicating that required fields cannot be null.
     *
     * @throws Exception if an error occurs during the login request.
     */
    @Test
    public void testLoginFailure_nullFields_thenReturnsBadRequest() throws Exception {
        // Prepare a login request with null fields
        LoginRequest loginRequest = new LoginRequest();

        String jsonContent = objectMapper.writeValueAsString(loginRequest);

        // Execute and verify
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Los campos obligatorios no pueden ser nulos"));
    }

    /**
     * Tests that registering a new client is successful and returns a valid JWT token.
     *
     * @throws Exception if an error occurs during the registration process.
     */
    @Test
    public void testRegisterClientSuccess() throws Exception {
        // Prepare the registration request for a client
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("newclient@test.com");
        registerRequest.setPassword("newpassword");
        registerRequest.setRole(Role.CLIENT);
        registerRequest.setFirst_name("Client");
        registerRequest.setLast_name("Test");
        registerRequest.setPhone("+34 612 567 890");

        String jsonContent = objectMapper.writeValueAsString(registerRequest);

        // Execute and verify
        mockMvc.perform(post("/api/auth/register/client")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    /**
     * Tests that attempting to register a client with missing required fields returns a Bad Request status.
     *
     * @throws Exception if an error occurs during the registration process.
     */
    @Test
    public void testRegisterClientFailure_nullFields_thenReturnsBadRequest() throws Exception {
        // Attempt to register a client with missing mandatory fields
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("new_client@test.com");
        registerRequest.setPassword("password");

        String jsonContent = objectMapper.writeValueAsString(registerRequest);

        // Execute and verify
        mockMvc.perform(post("/api/auth/register/client")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isBadRequest());
    }

    /**
     * Tests that registering a new administrator is successful when authenticated as an admin,
     * and that a valid JWT token is returned.
     *
     * @throws Exception if an error occurs during the registration process.
     */
    @Test
    public void testRegisterAdministratorSuccess() throws Exception {
        // Prepare the registration request for an administrator
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("AdminSuccess@test.com");
        registerRequest.setPassword("adminpassword");
        registerRequest.setRole(Role.ADMIN);
        registerRequest.setName("Test Admin");

        String jsonContent = objectMapper.writeValueAsString(registerRequest);

        // Execute and verify using a valid admin token for authentication
        mockMvc.perform(post("/api/auth/register/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    /**
     * Tests that attempting to register an administrator with missing required fields returns a Bad Request status.
     *
     * @throws Exception if an error occurs during the registration process.
     */
    @Test
    public void testRegisterAdministratorFailure_nullFields_thenReturnBadRequest() throws Exception {
        // Attempt to register an administrator with missing mandatory fields
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("newAdmin@test.com");
        registerRequest.setPassword("password");

        String jsonContent = objectMapper.writeValueAsString(registerRequest);

        // Execute and verify
        mockMvc.perform(post("/api/auth/register/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    /**
     * Tests that attempting to register an administrator without authentication returns a Forbidden status.
     *
     * @throws Exception if an error occurs during the registration process.
     */
    @Test
    public void testRegisterAdministratorFailure_withoutAuthentication_thenReturnsForbidden() throws Exception {
        // Prepare the registration request for an administrator
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("newadmin@test.com");
        registerRequest.setPassword("adminpassword");
        registerRequest.setRole(Role.ADMIN);
        registerRequest.setName("Test Admin");

        String jsonContent = objectMapper.writeValueAsString(registerRequest);

        // Execute and verify without an authentication header
        mockMvc.perform(post("/api/auth/register/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isForbidden());
    }

    /**
     * Tests that retrieving client information is successful when the client exists in the database,
     * returning the correct client details.
     *
     * @throws Exception if an error occurs during the retrieval process.
     */
    @Test
    public void testGetInfoClientSuccess() throws Exception {
        // Retrieve the existing client user (assumes it was created in initializeUsers)
        User clientUser = userRepository.getAllUsers().stream()
                .filter(u -> u.email.equals("client@test.com"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Usuario cliente no encontrado"));

        // Create a record in the client table associated with the user
        Client client = new Client();
        client.setUser_id(clientUser.id);
        client.setFirst_name("Test");
        client.setLast_name("Client");
        client.setPhone("+34 678 123 445");
        clientRepository.createClient(client);

        // Execute and verify the GET request for client information
        mockMvc.perform(get("/api/auth/me/client")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.firstName").value("Test"))
                .andExpect(jsonPath("$.lastName").value("Client"))
                .andExpect(jsonPath("$.email").value("client@test.com"))
                .andExpect(jsonPath("$.phone").value("+34 678 123 445"));
    }

    /**
     * Tests that retrieving client information returns an Internal Server Error when the client record
     * does not exist in the database.
     *
     * @throws Exception if an error occurs during the retrieval process.
     */
    @Test
    public void testGetInfoClientFailure_registerClientDoesNotExistInDatabase_thenReturnsInternalServerError() throws Exception {
        // Execute the GET request and verify that it returns an internal server error
        mockMvc.perform(get("/api/auth/me/client")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error")
                        .value("Autenticación no válida o error en el servicio"));
    }

    /**
     * Tests that retrieving client information without providing authentication returns a Forbidden status.
     *
     * @throws Exception if an error occurs during the retrieval process.
     */
    @Test
    public void testGetInfoClientFailure_withoutAuthentication_thenReturnsForbidden() throws Exception {
        // Execute the GET request without an authentication token and verify a Forbidden status
        mockMvc.perform(get("/api/auth/me/client"))
                .andExpect(status().isForbidden());
    }

    /**
     * Tests that retrieving administrator information is successful when the administrator exists in the database,
     * returning the correct administrator details.
     *
     * @throws Exception if an error occurs during the retrieval process.
     */
    @Test
    public void testGetInfoAdminSuccess() throws Exception {
        // Retrieve the existing administrator user (created in initializeUsers)
        User adminUser = userRepository.getUserByEmail("admin@test.com")
                .orElseThrow(() -> new RuntimeException("Usuario administrador no encontrado"));

        // Create a record in the Administrator table associated with the user
        Administrator admin = new Administrator();
        admin.setUser_id(adminUser.id);
        admin.setName("Admin Test");
        administratorRepository.createAdministrator(admin);

        // Execute the GET request for administrator information
        mockMvc.perform(get("/api/auth/me/admin")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.email").value("admin@test.com"))
                .andExpect(jsonPath("$.role").value("SUPERADMIN"))
                .andExpect(jsonPath("$.name").value("Admin Test"));
    }

    /**
     * Tests that retrieving administrator information returns an Internal Server Error when the administrator record
     * does not exist in the database.
     *
     * @throws Exception if an error occurs during the retrieval process.
     */
    @Test
    public void testGetInfoAdminFailure_registerAdminDoesNotExistInDatabase_thenReturnsInternalServerError() throws Exception {
        // Execute the GET request for administrator information and verify an internal server error
        mockMvc.perform(get("/api/auth/me/admin")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error")
                        .value("Autenticación no válida o error en el servicio"));
    }

    /**
     * Tests that retrieving administrator information throws an Internal Server Error when an unexpected error occurs.
     *
     * @throws Exception if an error occurs during the retrieval process.
     */
    @Test
    public void testGetInfoAdminFailure_thenThrowsInternalServerError() throws Exception {
        // Execute the GET request for administrator information and verify an internal server error
        mockMvc.perform(get("/api/auth/me/admin")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error")
                        .value("Autenticación no válida o error en el servicio"));
    }

    /**
     * Tests that retrieving administrator information without providing authentication returns a Forbidden status.
     *
     * @throws Exception if an error occurs during the retrieval process.
     */
    @Test
    public void testGetInfoAdminFailure_withoutAuthentication_thenReturnsForbidden() throws Exception {
        // Execute the GET request without an authentication token and verify a Forbidden status
        mockMvc.perform(get("/api/auth/me/admin"))
                .andExpect(status().isForbidden());
    }

    /**
     * Tests that updating a client's profile is successful when valid data is provided.
     *
     * @throws Exception if an error occurs during the update process.
     */
    @Test
    public void testUpdateProfileSuccess() throws Exception {
        // Retrieve the existing client user (assumes it was created in initializeUsers)
        User clientUser = userRepository.getAllUsers().stream()
                .filter(u -> u.email.equals("client@test.com"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Usuario cliente no encontrado"));

        // Create the client record associated with the user
        Client client = new Client();
        client.setUser_id(clientUser.id);
        client.setFirst_name("Test");
        client.setLast_name("Client");
        client.setPhone("+34 678 123 445");
        clientRepository.createClient(client);

        // Prepare the update profile request
        UpdateProfileRequest updateRequest = new UpdateProfileRequest();
        updateRequest.setFirstName("Updated");
        updateRequest.setLastName("Client");
        updateRequest.setPhone("+34 654 123 567");

        String jsonContent = objectMapper.writeValueAsString(updateRequest);

        // Execute the PUT request and expect a success status
        mockMvc.perform(put("/api/auth/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent)
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk());
    }

    /**
     * Tests that updating a client's profile without authentication returns a Forbidden status.
     *
     * @throws Exception if an error occurs during the update process.
     */
    @Test
    public void testUpdateProfileFailure_withoutAuthentication_thenReturnsForbidden() throws Exception {
        // Prepare the update profile request
        UpdateProfileRequest updateRequest = new UpdateProfileRequest();
        updateRequest.setFirstName("Updated");
        updateRequest.setLastName("Client");
        updateRequest.setPhone("+34 654 123 567");

        String jsonContent = objectMapper.writeValueAsString(updateRequest);

        // Execute the PUT request without an authentication token and expect a Forbidden status
        mockMvc.perform(put("/api/auth/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isForbidden());
    }

    /**
     * Tests that updating a client's profile returns a Not Found status when the client record
     * does not exist in the database.
     *
     * @throws Exception if an error occurs during the update process.
     */
    @Test
    public void testUpdateProfileFailure_registerClientDoesNotExistInDatabase_thenReturnsNotFound() throws Exception {
        // Prepare the update profile request
        UpdateProfileRequest updateRequest = new UpdateProfileRequest();
        updateRequest.setFirstName("Updated");
        updateRequest.setLastName("Client");
        updateRequest.setPhone("+34 654 123 567");

        String jsonContent = objectMapper.writeValueAsString(updateRequest);

        // Execute the PUT request and expect a Not Found status
        mockMvc.perform(put("/api/auth/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent)
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isNotFound());
    }

    /**
     * Tests that updating a client's profile with missing required fields returns a Bad Request status.
     *
     * @throws Exception if an error occurs during the update process.
     */
    @Test
    public void testUpdateProfileFailure_nullFieldsSent_thenReturnsBadRequest() throws Exception {
        // Send invalid data for updating the profile (e.g., missing required fields)
        UpdateProfileRequest updateRequest = new UpdateProfileRequest();
        // No mandatory fields are set

        String jsonContent = objectMapper.writeValueAsString(updateRequest);

        // Execute the PUT request and expect a Bad Request status
        mockMvc.perform(put("/api/auth/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent)
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isBadRequest());
    }

    /**
     * Tests that changing the user's password is successful when valid data is provided.
     *
     * @throws Exception if an error occurs during the password change process.
     */
    @Test
    public void testChangePasswordSuccess() throws Exception {
        // Prepare the password change request
        UserDTO passwordDTO = new UserDTO();
        passwordDTO.setNewPassword("newpassword123");

        String jsonContent = objectMapper.writeValueAsString(passwordDTO);

        // Execute and verify the PUT request for changing the password
        mockMvc.perform(put("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent)
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk());
    }

    /**
     * Tests that changing the user's password without authentication returns a Forbidden status.
     *
     * @throws Exception if an error occurs during the password change process.
     */
    @Test
    public void testChangePasswordFailure_withoutAuthentication_thenReturnsForbidden() throws Exception {
        // Prepare the password change request
        UserDTO passwordDTO = new UserDTO();
        passwordDTO.setNewPassword("newpassword123");

        String jsonContent = objectMapper.writeValueAsString(passwordDTO);

        // Execute the PUT request without an authentication token and expect a Forbidden status
        mockMvc.perform(put("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isForbidden());
    }

    /**
     * Tests that changing the user's email is successful when valid data is provided.
     *
     * @throws Exception if an error occurs during the email change process.
     */
    @Test
    public void testChangeEmailSuccess() throws Exception {
        // Prepare the email change request
        UserDTO emailDTO = new UserDTO();
        emailDTO.setNewEmail("updatedAdmin@test.com");

        String jsonContent = objectMapper.writeValueAsString(emailDTO);

        // Execute and verify the PUT request for changing the email
        mockMvc.perform(put("/api/auth/change-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    /**
     * Tests that attempting to change the user's email with a null new email field returns a Bad Request status.
     *
     * @throws Exception if an error occurs during the email change process.
     */
    @Test
    public void testChangeEmailFailure_nullField_thenReturnsBadRequest() throws Exception {
        // Attempt to change the email without providing the new email
        UserDTO emailDTO = new UserDTO();

        String jsonContent = objectMapper.writeValueAsString(emailDTO);

        // Execute and verify the PUT request and expect a Bad Request status
        mockMvc.perform(put("/api/auth/change-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    /**
     * Tests that attempting to change the user's email to one that already exists (duplicate primary key)
     * returns an Internal Server Error.
     *
     * @throws Exception if an error occurs during the email change process.
     */
    @Test
    public void testChangeEmailFailure_duplicatePrimaryKeyField_thenReturnsInternalServerError() throws Exception {
        // Prepare the email change request with an email that already exists
        UserDTO emailDTO = new UserDTO();
        emailDTO.newEmail = "client@test.com";

        String jsonContent = objectMapper.writeValueAsString(emailDTO);

        // Execute and verify the PUT request and expect an Internal Server Error
        mockMvc.perform(put("/api/auth/change-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isInternalServerError());
    }

    /**
     * Tests that attempting to change the user's email without authentication returns a Forbidden status.
     *
     * @throws Exception if an error occurs during the email change process.
     */
    @Test
    public void testChangeEmailFailure_withoutAuthentication_thenReturnsForbidden() throws Exception {
        // Prepare the email change request
        UserDTO emailDTO = new UserDTO();
        emailDTO.newEmail = "test@test.com";

        String jsonContent = objectMapper.writeValueAsString(emailDTO);

        // Execute the PUT request without an authentication token and expect a Forbidden status
        mockMvc.perform(put("/api/auth/change-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isForbidden());
    }
}