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

    @DynamicPropertySource
    public static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mariaDB::getJdbcUrl);
        registry.add("spring.datasource.username", mariaDB::getUsername);
        registry.add("spring.datasource.password", mariaDB::getPassword);
    }

    @BeforeAll
    public void setup() {
        userRepository.getAllUsers().forEach(u -> userRepository.deleteUser(u.id));

        User adminUser = createUser("admin@test.com", Role.SUPERADMIN);
        adminToken = jwtService.getToken(adminUser);

        User clientUser = createUser("client@test.com", Role.CLIENT);
        clientToken = jwtService.getToken(clientUser);
        createClient(clientUser.id, "Test", "Client", "+34123456789");
    }

    private User createUser(String email, Role role) {
        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("password"));
        user.setRole(role);
        user.setId(userRepository.createUser(user));
        return user;
    }

    private void createClient(long userId, String firstName, String lastName, String phone) {
        Client client = new Client();
        client.setUser_id(userId);
        client.setFirst_name(firstName);
        client.setLast_name(lastName);
        client.setPhone(phone);
        long id = clientRepository.createClient(client);
        client.setId(id);
    }

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

    @Test
    public void testGetAllClients_Success() throws Exception {
        // Preparar
        User clientUser = userRepository.getUserByEmail("client@test.com")
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado con email client@test.com"));
        createClient(clientUser.id, "Client1", "Last1", "+34111111111");

        // Ejecutar y Verificar
        mockMvc.perform(get("/api/admin/clients")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    public void testGetAllClients_EmptyList_ReturnsNotFound() throws Exception {
        // Ejecutar y Verificar
        mockMvc.perform(get("/api/admin/clients")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value("No hay clientes registrados en el sistema."));
    }

    @Test
    public void testGetAllClients_WithoutAuth_ReturnsForbidden() throws Exception {
        // Ejecutar y Verificar
        mockMvc.perform(get("/api/admin/clients"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testGetClientById_Success() throws Exception {
        // Preparar
        User user = createUser("email@test.com", Role.CLIENT);
        createClient(user.id, "New", "Client", "+34 654 123 456");
        Client client = clientRepository.getClientByUserId(user.id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        // Ejecutar y Verificar
        mockMvc.perform(get("/api/admin/client/" + client.id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user_id").value(user.id))
                .andExpect(jsonPath("$.first_name").value("New"))
                .andExpect(jsonPath("$.last_name").value("Client"))
                .andExpect(jsonPath("$.phone").value("+34 654 123 456"));
    }

    @Test
    public void testGetClientById_NotFound_ReturnsError() throws Exception {
        // Ejecutar y Verificar
        mockMvc.perform(get("/api/admin/client/999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$").value("El cliente solicitado no existe"));
    }

    @Test
    public void testDeleteClient_Success() throws Exception {
        // Preparar
        User newUser = createUser("newclient@test.com", Role.CLIENT);
        createClient(newUser.id, "New", "Client", "+34222222222");

        Client newClient = clientRepository.getClientByUserId(newUser.id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        // Ejecutar y Verificar
        mockMvc.perform(delete("/api/admin/client/" + newClient.id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().string("Cliente con id: " + newClient.id + "eliminado correctamente."));

        assertThat(clientRepository.getClientById(newClient.id)).isEmpty();
        assertThat(userRepository.getUserById(newUser.id)).isEmpty();
    }

    @Test
    public void testDeleteClient_NotFound_ReturnsError() throws Exception {
        // Ejecutar y Verificar
        mockMvc.perform(delete("/api/admin/client/999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No existe el cliente que quieres eliminar"));
    }

    @Test
    public void testClientEndpoints_WithClientRole_ReturnsForbidden() throws Exception {
        // Ejecutar y Verificar
        mockMvc.perform(get("/api/admin/clients")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isForbidden());

        // Ejecutar y Verificar
        mockMvc.perform(get("/api/admin/client/1")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isForbidden());

        // Ejecutar y Verificar
        mockMvc.perform(delete("/api/admin/client/1")
                        .header("Authorization", "Bearer " + clientToken))
                .andExpect(status().isForbidden());
    }
}
