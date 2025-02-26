package com.alfre.DHHotel;

import com.alfre.DHHotel.adapter.security.jwt.JwtService;
import com.alfre.DHHotel.domain.model.Administrator;
import com.alfre.DHHotel.domain.model.Role;
import com.alfre.DHHotel.domain.model.User;
import com.alfre.DHHotel.domain.repository.AdministratorRepository;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * This class contains the attributes and methods for realize the integration tests
 * of the system administrators operations.
 *
 * @author Alfredo Sobrados González
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AdminOperationsIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AdministratorRepository administratorRepository;

	@Autowired
	private UserRepository userRepository;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	private JwtService jwtService;

	private static String superAdminToken;

    @Container
	public static MariaDBContainer<?> mariaDB = new MariaDBContainer<>(DockerImageName
			.parse("mariadb:10.6.5"))
			.withDatabaseName("testdb")
			.withUsername("test")
			.withPassword("test1234");

	// Bloque estático que fuerza el arranque del contenedor antes de que se ejecute DynamicPropertySource
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
	 * Initializes the super admin user for the test suite.
	 *
	 * <p>This method performs the following operations:
	 * <ul>
	 *   <li>Removes all existing users from the user repository.</li>
	 *   <li>Creates a new super admin user with the email {@code "superadmin@test.com"}, password {@code "password"},
	 *       and role {@code Role.SUPERADMIN}.</li>
	 *   <li>Generates a JWT token for the newly created super admin and stores it in {@code superAdminToken}.</li>
	 * </ul>
	 * </p>
	 *
	 * <p>This setup is executed once before all tests to ensure that a consistent super admin is available throughout the test suite.</p>
	 */
	@BeforeAll
	public void initializeSuperAdmin() {
		// Limpieza inicial
		userRepository.getAllUsers().forEach(u -> userRepository.deleteUser(u.id));

		// Creación superadmin
		User superAdmin = new User();
		superAdmin.email = "superadmin@test.com";
		superAdmin.password = "password";
		superAdmin.role = Role.SUPERADMIN;
		userRepository.createUser(superAdmin);

		// Generar token
		superAdminToken = jwtService.getToken(superAdmin);
	}

	/**
	 * Resets the database to a clean state before each test.
	 *
	 * <p>This method performs the following actions:
	 * <ul>
	 *   <li>Deletes all administrator records by invoking {@code administratorRepository.deleteAll()}.</li>
	 *   <li>Retrieves all user records from the database, filters out the user with the email
	 *       {@code "superadmin@test.com"}, and deletes the remaining users using {@code userRepository.deleteUser(u.id)}.</li>
	 * </ul>
	 * </p>
	 *
	 * <p>The {@code @BeforeEach} annotation ensures that this method is executed before each test,
	 * preventing interference from leftover data.</p>
	 */
	@BeforeEach
	public void resetDatabase() {
		administratorRepository.deleteAll();

		userRepository.getAllUsers().stream()
				.filter(u -> !u.email.equals("superadmin@test.com"))
				.forEach(u -> userRepository.deleteUser(u.id));
	}

	/**
	 * Tests that the GET endpoint for fetching all administrators returns a 404 Not Found status
	 * when there are no administrators registered in the system.
	 * <p>
	 * This test sends an HTTP GET request to "/api/superadmin/admins" with a valid super admin token
	 * and expects the response status to be 404 Not Found along with the message
	 * "No hay administradores registrados en el sistema." in the response body.
	 * </p>
	 *
	 * @throws Exception if an error occurs when launching the HTTP request with mockMvc.
	 */
	@Test
	public void testGetAllAdministratorsWhenEmptyFailure_thenReturnsNotFound() throws Exception {
		// Ejecutar y Verificar
		mockMvc.perform(get("/api/superadmin/admins")
						.header("Authorization", "Bearer " + superAdminToken))
				.andExpect(status().isNotFound())
				.andExpect(content().string("No hay administradores registrados en el sistema."));
	}

	/**
	 * Tests that retrieving all administrators without authentication returns a Forbidden status.
	 *
	 * @throws Exception if an error occurs during the request
	 */
	@Test
	public void testGetAllAdministratorsFailureWithoutAuthentication() throws Exception {
		// Execute and verify
		mockMvc.perform(get("/api/superadmin/admins"))
				.andExpect(status().isForbidden());
	}

	/**
	 * Tests that retrieving all administrators returns the expected list when administrators exist.
	 *
	 * <p>This test creates two administrator users, then verifies that a GET request to the admins endpoint
	 * returns both administrators with their corresponding names.</p>
	 *
	 * @throws Exception if an error occurs during the request
	 */
	@Test
	public void testGetAllAdministratorsWhenNotEmpty() throws Exception {
		// Insert two administrators
		User user1 = new User();
		user1.setEmail("adminuser@example.com");
		user1.setPassword("securePassword");
		user1.setRole(Role.ADMIN);

		User user2 = new User();
		user2.setEmail("adminuser2@example.com");
		user2.setPassword("securePassword");
		user2.setRole(Role.ADMIN);

		long idUser1 = userRepository.createUser(user1);
		long idUser2 = userRepository.createUser(user2);

		Administrator admin1 = new Administrator();
		admin1.setUser_id(idUser1);
		admin1.setName("Admin One");

		Administrator admin2 = new Administrator();
		admin2.setUser_id(idUser2);
		admin2.setName("Admin Two");

		administratorRepository.createAdministrator(admin1);
		administratorRepository.createAdministrator(admin2);

		// Execute and verify
		mockMvc.perform(get("/api/superadmin/admins")
						.header("Authorization", "Bearer " + superAdminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].name").value("Admin One"))
				.andExpect(jsonPath("$[1].name").value("Admin Two"));
	}

	/**
	 * Tests that retrieving an administrator by user ID returns the correct administrator when found.
	 *
	 * <p>This test creates a user and its corresponding administrator, then performs a GET request using the
	 * user's ID. It verifies that the response contains the correct administrator details.</p>
	 *
	 * @throws Exception if an error occurs during the request
	 */
	@Test
	public void testGetAdministratorByUserIdFound() throws Exception {
		// Create user and administrator
		User user = new User();
		user.setEmail("adminuser@example.com");
		user.setPassword("securePassword");
		user.setRole(Role.ADMIN);
		long userId = userRepository.createUser(user); // generated user ID

		Administrator admin = new Administrator();
		admin.setUser_id(userId);
		admin.setName("Admin User");
		administratorRepository.createAdministrator(admin); // Create admin

		// Execute and verify
		mockMvc.perform(get("/api/superadmin/admin/userId/{userId}", userId)
						.header("Authorization", "Bearer " + superAdminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Admin User"))
				.andExpect(jsonPath("$.user_id").value(userId)); // Validate with the real ID
	}

	/**
	 * Tests that retrieving an administrator by user ID without authentication is forbidden.
	 *
	 * @throws Exception if an error occurs during the request
	 */
	@Test
	public void testGetAdministratorByUserIdFailureWithoutAuthentication() throws Exception {
		// Execute and verify
		mockMvc.perform(get("/api/superadmin/admin/userId/{userId}", 1L))
				.andExpect(status().isForbidden());
	}

	/**
	 * Tests that retrieving an administrator by a non-existent user ID returns a Not Found status.
	 *
	 * <p>This test performs a GET request with a user ID that does not correspond to any administrator,
	 * expecting a 404 response with a message indicating that the administrator does not exist.</p>
	 *
	 * @throws Exception if an error occurs during the request
	 */
	@Test
	public void testGetAdministratorByUserIdFailureNotFound() throws Exception {
		// Execute and verify
		mockMvc.perform(get("/api/superadmin/admin/userId/{userId}", 999L)
						.header("Authorization", "Bearer " + superAdminToken))
				.andExpect(status().isNotFound())
				.andExpect(content().string("El administrador solicitado no existe."));
	}

	/**
	 * Tests that retrieving an administrator by admin ID returns the correct administrator when found.
	 *
	 * <p>This test creates a user and an administrator, retrieves the administrator by user ID to get
	 * the generated admin ID, and then performs a GET request using that admin ID to verify the administrator's details.</p>
	 *
	 * @throws Exception if an error occurs during the request
	 */
	@Test
	public void testGetAdministratorByIdFound() throws Exception {
		// Create user and administrator
		User user = new User();
		user.setEmail("adminuser@example.com");
		user.setPassword("securePassword");
		user.setRole(Role.ADMIN);
		long userId = userRepository.createUser(user);

		Administrator admin = new Administrator();
		admin.setUser_id(userId);
		admin.setName("Admin By Id");
		administratorRepository.createAdministrator(admin);

		// Retrieve the administrator's ID using the user ID
		Optional<Administrator> createdAdmin = administratorRepository.getAdministratorByUserId(userId);
		assertThat(createdAdmin).isPresent();
		long adminId = createdAdmin.get().id;

		// Execute and verify
		mockMvc.perform(get("/api/superadmin/admin/id/{id}", adminId)
						.header("Authorization", "Bearer " + superAdminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Admin By Id"))
				.andExpect(jsonPath("$.id").value(adminId));
	}

	/**
	 * Tests that retrieving an administrator by admin ID without authentication is forbidden.
	 *
	 * @throws Exception if an error occurs during the request
	 */
	@Test
	public void testGetAdministratorByIdFailureWithoutAuthentication() throws Exception {
		// Execute and verify
		mockMvc.perform(get("/api/superadmin/admin/id/{id}", 1L))
				.andExpect(status().isForbidden());
	}

	/**
	 * Tests that retrieving an administrator by a non-existent admin ID returns a Not Found status.
	 *
	 * <p>This test sends a GET request with an admin ID that does not exist, expecting a 404 response
	 * with an appropriate error message.</p>
	 *
	 * @throws Exception if an error occurs during the request
	 */
	@Test
	public void testGetAdministratorByIdNotFound() throws Exception {
		// Execute and verify
		mockMvc.perform(get("/api/superadmin/admin/id/{id}", 999L)
						.header("Authorization", "Bearer " + superAdminToken))
				.andExpect(status().isNotFound())
				.andExpect(content().string("El administrador solicitado no existe."));
	}

	/**
	 * Tests that updating an administrator's details is successful when valid data is provided.
	 *
	 * <p>This test creates a user and an administrator, then sends a PUT request with updated administrator
	 * information. It verifies that the update is successful and that the administrator's details are updated in the repository.</p>
	 *
	 * @throws Exception if an error occurs during the request
	 */
	@Test
	public void testUpdateAdministratorSuccess() throws Exception {
		// Create user and administrator
		User user = new User();
		user.setEmail("adminuser@example.com");
		user.setPassword("securePassword");
		user.setRole(Role.ADMIN);
		long userId = userRepository.createUser(user);

		Administrator admin = new Administrator();
		admin.setUser_id(userId);
		admin.setName("Original Name");
		administratorRepository.createAdministrator(admin);

		// Retrieve the current administrator using the user ID
		Optional<Administrator> existingAdmin = administratorRepository.getAdministratorByUserId(userId);
		assertThat(existingAdmin).isPresent();

		// Update using the real user ID
		Administrator updatedAdmin = new Administrator();
		updatedAdmin.setName("Updated Name");
		String jsonContent = objectMapper.writeValueAsString(updatedAdmin);

		// Execute update
		mockMvc.perform(put("/api/superadmin/admin/{userId}", userId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonContent)
						.header("Authorization", "Bearer " + superAdminToken))
				.andExpect(status().isOk())
				.andExpect(content().string("La actualización se ha hecho correctamente"));

		// Verify update in the repository
		Optional<Administrator> adminAfterUpdate = administratorRepository.getAdministratorByUserId(userId);
		assertThat(adminAfterUpdate).isPresent();
		assertThat(adminAfterUpdate.get().name).isEqualTo("Updated Name");
	}

	/**
	 * Tests that updating an administrator fails with a Bad Request status when the administrator does not exist.
	 *
	 * <p>This test attempts to update a non-existing administrator and expects a 400 Bad Request response with an error message.</p>
	 *
	 * @throws Exception if an error occurs during the request
	 */
	@Test
	public void testUpdateAdministratorFailureBadRequest() throws Exception {
		// No administrator is inserted
		Administrator updatedAdmin = new Administrator();
		updatedAdmin.setName("Any Name");
		String jsonContent = objectMapper.writeValueAsString(updatedAdmin);

		// Execute and verify update failure
		mockMvc.perform(put("/api/superadmin/admin/{userId}", 500L)
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonContent)
						.header("Authorization", "Bearer " + superAdminToken))
				.andExpect(status().isBadRequest())
				.andExpect(content().string("No se ha podido actualizar."));
	}

	/**
	 * Tests that updating an administrator without authentication is forbidden.
	 *
	 * @throws Exception if an error occurs during the request
	 */
	@Test
	public void testUpdateAdministratorFailureWithoutAuthentication() throws Exception {
		// Execute and verify
		mockMvc.perform(put("/api/superadmin/admin/{userId}", 1L))
				.andExpect(status().isForbidden());
	}

	/**
	 * Tests that deleting an administrator is successful when the administrator exists.
	 *
	 * <p>This test creates a user and an administrator, then sends a DELETE request to remove the administrator.
	 * It verifies that the response confirms successful deletion and that the administrator no longer exists in the repository.</p>
	 *
	 * @throws Exception if an error occurs during the request
	 */
	@Test
	public void testDeleteAdministratorSuccess() throws Exception {
		// Create user and administrator
		User user = new User();
		user.setEmail("deleteadmin@example.com");
		user.setPassword("password");
		user.setRole(Role.ADMIN);
		long userId = userRepository.createUser(user);

		Administrator admin = new Administrator();
		admin.setUser_id(userId);
		admin.setName("Admin Delete");
		administratorRepository.createAdministrator(admin);

		// Retrieve the administrator's ID
		Optional<Administrator> createdAdmin = administratorRepository.getAdministratorByUserId(userId);
		assertThat(createdAdmin).isPresent();
		long adminId = createdAdmin.get().id;

		// Execute delete
		mockMvc.perform(delete("/api/superadmin/admin/{id}", adminId)
						.header("Authorization", "Bearer " + superAdminToken))
				.andExpect(status().isOk())
				.andExpect(content().string("El administrador con id: " + adminId +
						" se ha eliminado correctamente"));

		// Verify deletion
		boolean exists = administratorRepository.getAdministratorById(adminId).isPresent();
		assertThat(exists).isFalse();
	}

	/**
	 * Tests that deleting an administrator without authentication is forbidden.
	 *
	 * @throws Exception if an error occurs during the request
	 */
	@Test
	public void testDeleteAdministratorFailureWithoutAuthentication() throws Exception {
		// Execute and verify
		mockMvc.perform(delete("/api/superadmin/admin/{id}", 1L))
				.andExpect(status().isForbidden());
	}

	/**
	 * Tests that attempting to delete a non-existent administrator returns a Not Found status.
	 *
	 * <p>This test sends a DELETE request for an administrator ID that does not exist,
	 * expecting a 404 response with an appropriate error message.</p>
	 *
	 * @throws Exception if an error occurs during the request
	 */
	@Test
	public void testDeleteAdministratorFailure() throws Exception {
		// Execute and verify
		mockMvc.perform(delete("/api/superadmin/admin/{id}", 999L)
						.header("Authorization", "Bearer " + superAdminToken))
				.andExpect(status().isNotFound())
				.andExpect(content().string("No existe el administrador que quieres eliminar"));
	}
}