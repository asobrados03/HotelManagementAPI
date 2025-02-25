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

    @DynamicPropertySource
    public static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> mariaDB.getJdbcUrl());
        registry.add("spring.datasource.username", mariaDB::getUsername);
        registry.add("spring.datasource.password", mariaDB::getPassword);
    }

    @BeforeAll
    public void initializeUsers() {
        // Limpieza inicial de usuarios
        userRepository.getAllUsers().forEach(u -> userRepository.deleteUser(u.id));

        // Crear usuario cliente
        User clientUser = new User();
        clientUser.setEmail("client@test.com");
        clientUser.setPassword(passwordEncoder.encode("password"));
        clientUser.setRole(Role.CLIENT);
        long clientId = userRepository.createUser(clientUser);
        clientUser.setId(clientId); // Asignar el id devuelto
        clientToken = jwtService.getToken(clientUser);

        // Crear usuario administrador
        User adminUser = new User();
        adminUser.setEmail("admin@test.com");
        adminUser.setPassword(passwordEncoder.encode("password")); // Es recomendable codificar la contraseña
        adminUser.setRole(Role.SUPERADMIN);
        long adminId = userRepository.createUser(adminUser);
        adminUser.setId(adminId); // Asignar el id devuelto
        adminToken = jwtService.getToken(adminUser);
    }

    @BeforeEach
    public void ensureAdminUserExists() {
        // Verifica si existe un usuario con email "admin@test.com"
        Optional<User> adminOpt = userRepository.getAllUsers().stream()
                .filter(u -> u.email.equals("admin@test.com"))
                .findFirst();
        if (adminOpt.isEmpty()) {
            User adminUser = new User();
            adminUser.setEmail("admin@test.com");
            adminUser.setPassword(passwordEncoder.encode("password"));
            adminUser.setRole(Role.SUPERADMIN);
            long adminId = userRepository.createUser(adminUser);
            adminUser.setId(adminId); // Es esencial asignar el identificador retornado
            adminToken = jwtService.getToken(adminUser);
        }
    }

    @BeforeEach
    public void resetDatabase() {
        administratorRepository.deleteAll();
        clientRepository.deleteAll();
    }

    @Test
    public void testLoginSuccess() throws Exception {
        // Preparar
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("client@test.com");
        loginRequest.setPassword("password");

        String jsonContent = objectMapper.writeValueAsString(loginRequest);

        // Ejecutar y Verificar
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                // Se asume que la respuesta contiene un campo "token"
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    public void testLoginFailure_badCredentials_thenReturnsNotFound() throws Exception {
        // Preparar
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("client@test.com");
        loginRequest.setPassword("wrongpassword");

        String jsonContent = objectMapper.writeValueAsString(loginRequest);

        // Ejecutar y Verificar
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Credenciales inválidas"));
    }

    @Test
    public void testLoginFailure_nullFields_thenReturnsBadRequest() throws Exception {
        // Preparar
        LoginRequest loginRequest = new LoginRequest();

        String jsonContent = objectMapper.writeValueAsString(loginRequest);

        // Ejecutar y Verificar
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Los campos obligatorios no pueden ser nulos"));
    }

    @Test
    public void testRegisterClientSuccess() throws Exception {
        // Preparar
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("newclient@test.com");
        registerRequest.setPassword("newpassword");
        registerRequest.setRole(Role.CLIENT);
        registerRequest.setFirst_name("Client");
        registerRequest.setLast_name("Test");
        registerRequest.setPhone("+34 612 567 890");

        String jsonContent = objectMapper.writeValueAsString(registerRequest);

        // Ejecutar y Verificar
        mockMvc.perform(post("/api/auth/register/client")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    public void testRegisterClientFailure_nullFields_thenReturnsBadRequest() throws Exception {
        // Se intenta registrar un cliente con campos obligatorios vacíos
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("new_client@test.com");
        registerRequest.setPassword("password");

        String jsonContent = objectMapper.writeValueAsString(registerRequest);

        // Ejecutar y Verificar
        mockMvc.perform(post("/api/auth/register/client")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testRegisterAdministratorSuccess() throws Exception {
        // Preparar
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("AdminSuccess@test.com");
        registerRequest.setPassword("adminpassword");
        registerRequest.setRole(Role.ADMIN);
        registerRequest.setName("Test Admin");

        String jsonContent = objectMapper.writeValueAsString(registerRequest);

        // Ejecutar y Verificar
        mockMvc.perform(post("/api/auth/register/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    public void testRegisterAdministratorFailure_nullFields_thenReturnBadRequest() throws Exception {
        // Se intenta registrar un administrador con dos campos obligatorios vacíos
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("newAdmin@test.com");
        registerRequest.setPassword("password");

        String jsonContent = objectMapper.writeValueAsString(registerRequest);

        // Ejecutar y Verificar
        mockMvc.perform(post("/api/auth/register/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testRegisterAdministratorFailure_withoutAuthentication_thenReturnsForbidden() throws Exception {
        // Preparar
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("newadmin@test.com");
        registerRequest.setPassword("adminpassword");
        registerRequest.setRole(Role.ADMIN);
        registerRequest.setName("Test Admin");

        String jsonContent = objectMapper.writeValueAsString(registerRequest);

        // Ejecutar y Verificar
        mockMvc.perform(post("/api/auth/register/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testGetInfoClientSuccess() throws Exception {
        // Obtener el usuario cliente existente (se supone que ya fue creado en initializeUsers)
        User clientUser = userRepository.getAllUsers().stream()
                .filter(u -> u.email.equals("client@test.com"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Usuario cliente no encontrado"));

        // Crear el registro en la tabla de clientes asociado al usuario
        Client client = new Client();
        client.setUser_id(clientUser.id);
        client.setFirst_name("Test");
        client.setLast_name("Client");
        client.setPhone("+34 678 123 445");
        clientRepository.createClient(client);

        // Ejecutar y verificar la petición GET para obtener la información del cliente
        mockMvc.perform(get("/api/auth/me/client")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.firstName").value("Test"))
                .andExpect(jsonPath("$.lastName").value("Client"))
                .andExpect(jsonPath("$.email").value("client@test.com"))
                .andExpect(jsonPath("$.phone").value("+34 678 123 445"));
    }

    @Test
    public void testGetInfoClientFailure_registerClientDoesNotExistInDatabase_thenReturnsInternalServerError()
            throws Exception {
        // Ejecutar la petición GET y Verificar
        mockMvc.perform(get("/api/auth/me/client")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error")
                        .value("Autenticación no válida o error en el servicio"));
    }

    @Test
    public void testGetInfoClientFailure_withoutAuthentication_thenReturnsForbidden() throws Exception {
        // Sin token o con token inválido se espera que falle la autenticación (403 Forbidden)
        mockMvc.perform(get("/api/auth/me/client"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testGetInfoAdminSuccess() throws Exception {
        // Obtener el usuario administrador existente (se creó en initializeUsers)
        User adminUser = userRepository.getUserByEmail("admin@test.com")
                .orElseThrow(() -> new RuntimeException("Usuario administrador no encontrado"));

        // Crear el registro en la tabla Administrator asociado al usuario
        Administrator admin = new Administrator();
        admin.setUser_id(adminUser.id);
        admin.setName("Admin Test");
        administratorRepository.createAdministrator(admin);

        // Ejecutar la petición GET para obtener la información del administrador
        mockMvc.perform(get("/api/auth/me/admin")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.email").value("admin@test.com"))
                .andExpect(jsonPath("$.role").value("SUPERADMIN"))
                .andExpect(jsonPath("$.name").value("Admin Test"));
    }

    @Test
    public void testGetInfoAdminFailure_registerAdminDoesNotExistInDatabase_thenReturnsInternalServerError()
            throws Exception {
        // Ejecutar la petición GET
        mockMvc.perform(get("/api/auth/me/admin")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error")
                        .value("Autenticación no válida o error en el servicio"));
    }

    @Test
    public void testGetInfoAdminFailure_thenThrowsInternalServerError() throws Exception {
        // Ejecutar y Verificar
        mockMvc.perform(get("/api/auth/me/admin")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error")
                        .value("Autenticación no válida o error en el servicio"));
    }

    @Test
    public void testGetInfoAdminFailure_withoutAuthentication_thenReturnsForbidden() throws Exception {
        // Sin token o con token inválido se espera que falle la autenticación (403 Forbidden)
        mockMvc.perform(get("/api/auth/me/admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testUpdateProfileSuccess() throws Exception {
        // Obtener el usuario cliente existente (suponiendo que ya fue creado en initializeUsers)
        User clientUser = userRepository.getAllUsers().stream()
                .filter(u -> u.email.equals("client@test.com"))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Usuario cliente no encontrado"));

        // Crear el registro de cliente asociado
        Client client = new Client();
        client.setUser_id(clientUser.id);
        client.setFirst_name("Test");
        client.setLast_name("Client");
        client.setPhone("+34 678 123 445");
        clientRepository.createClient(client);

        // Preparar la solicitud de actualización de perfil
        UpdateProfileRequest updateRequest = new UpdateProfileRequest();
        updateRequest.setFirstName("Updated");
        updateRequest.setLastName("Client");
        updateRequest.setPhone("+34 654 123 567");

        String jsonContent = objectMapper.writeValueAsString(updateRequest);

        // Ejecutar la petición PUT y esperar status 200
        mockMvc.perform(put("/api/auth/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent)
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk());
    }

    @Test
    public void testUpdateProfileFailure_withoutAuthentication_thenReturnsForbidden() throws Exception {
        // Preparar la solicitud de actualización de perfil
        UpdateProfileRequest updateRequest = new UpdateProfileRequest();
        updateRequest.setFirstName("Updated");
        updateRequest.setLastName("Client");
        updateRequest.setPhone("+34 654 123 567");

        String jsonContent = objectMapper.writeValueAsString(updateRequest);

        // Ejecutar la petición PUT y esperar status 200
        mockMvc.perform(put("/api/auth/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testUpdateProfileFailure_registerClientDoesNotExistInDatabase_thenReturnsNotFound() throws Exception {
        // Preparar
        UpdateProfileRequest updateRequest = new UpdateProfileRequest();
        updateRequest.setFirstName("Updated");
        updateRequest.setLastName("Client");
        updateRequest.setPhone("+34 654 123 567");

        String jsonContent = objectMapper.writeValueAsString(updateRequest);

        // Ejecutar y Verificar
        mockMvc.perform(put("/api/auth/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent)
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testUpdateProfileFailure_nullFieldsSent_thenReturnsBadRequest() throws Exception {
        // Se envían datos inválidos para actualizar el perfil (por ejemplo, campos requeridos vacíos)
        UpdateProfileRequest updateRequest = new UpdateProfileRequest();
        // No se establecen valores obligatorios

        String jsonContent = objectMapper.writeValueAsString(updateRequest);

        // Ejecutar y Verificar
        mockMvc.perform(put("/api/auth/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent)
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testChangePasswordSuccess() throws Exception {
        // Preparar
        UserDTO passwordDTO = new UserDTO();
        passwordDTO.setNewPassword("newpassword123");

        String jsonContent = objectMapper.writeValueAsString(passwordDTO);

        // Ejecutar y Verificar
        mockMvc.perform(put("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent)
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isOk());
    }

    @Test
    public void testChangePasswordFailure_withoutAuthentication_thenReturnsForbidden() throws Exception {
        // Sin token de autenticación se espera un fallo (403 Forbidden)
        UserDTO passwordDTO = new UserDTO();
        passwordDTO.setNewPassword("newpassword123");

        String jsonContent = objectMapper.writeValueAsString(passwordDTO);

        // Ejecutar y Verificar
        mockMvc.perform(put("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testChangeEmailSuccess() throws Exception {
        // Preparar
        UserDTO emailDTO = new UserDTO();
        emailDTO.setNewEmail("updatedAdmin@test.com");

        String jsonContent = objectMapper.writeValueAsString(emailDTO);

        // Ejecutar y Verificar
        mockMvc.perform(put("/api/auth/change-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    public void testChangeEmailFailure_nullField_thenReturnsBadRequest() throws Exception {
        // Intentar cambiar el email sin indicar el email
        UserDTO emailDTO = new UserDTO();

        String jsonContent = objectMapper.writeValueAsString(emailDTO);

        // Ejecutar y Verificar
        mockMvc.perform(put("/api/auth/change-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testChangeEmailFailure_duplicatePrimaryKeyField_thenReturnsInternalServerError() throws Exception {
        // Preparar
        UserDTO emailDTO = new UserDTO();
        emailDTO.newEmail = "client@test.com";

        String jsonContent = objectMapper.writeValueAsString(emailDTO);

        // Ejecutar y Verificar
        mockMvc.perform(put("/api/auth/change-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isInternalServerError());
    }

    @Test
    public void testChangeEmailFailure_withoutAuthentication_thenReturnsForbidden() throws Exception {
        // Preparar
        UserDTO emailDTO = new UserDTO();
        emailDTO.newEmail = "test@test.com";

        String jsonContent = objectMapper.writeValueAsString(emailDTO);

        // Sin token o con token inválido se espera que falle la autenticación (403 Forbidden)
        // Ejecutar y Verificar
        mockMvc.perform(put("/api/auth/change-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonContent))
                .andExpect(status().isForbidden());
    }
}