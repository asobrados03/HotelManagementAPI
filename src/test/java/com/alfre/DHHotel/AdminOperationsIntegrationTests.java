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

	@DynamicPropertySource
	static void databaseProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", () -> mariaDB.getJdbcUrl());
		registry.add("spring.datasource.username", mariaDB::getUsername);
		registry.add("spring.datasource.password", mariaDB::getPassword);
	}

	@BeforeAll
	void initializeSuperAdmin() {
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

	@BeforeEach
	public void resetDatabase() {
		administratorRepository.deleteAll();

		userRepository.getAllUsers().stream()
				.filter(u -> !u.email.equals("superadmin@test.com"))
				.forEach(u -> userRepository.deleteUser(u.id));
	}

	@Test
	public void testGetAllAdministratorsWhenEmptyFailure_thenReturnsNotFound() throws Exception {
		// Ejecutar y Verificar
		mockMvc.perform(get("/api/superadmin/admins")
						.header("Authorization", "Bearer " + superAdminToken))
				.andExpect(status().isNotFound())
				.andExpect(content().string("No hay administradores registrados en el sistema."));
	}

	@Test
	public void testGetAllAdministratorsFailureWithoutAuthentication() throws Exception {
		// Ejecutar y Verificar
		mockMvc.perform(get("/api/superadmin/admins"))
				.andExpect(status().isForbidden());
	}

	@Test
	public void testGetAllAdministratorsWhenNotEmpty() throws Exception {
		// Insertar dos administradores
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

		// Ejecutar y Verificar
		mockMvc.perform(get("/api/superadmin/admins")
						.header("Authorization", "Bearer " + superAdminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].name").value("Admin One"))
				.andExpect(jsonPath("$[1].name").value("Admin Two"));
	}

	@Test
	public void testGetAdministratorByUserIdFound() throws Exception {
		// Crear usuario y administrador
		User user = new User();
		user.setEmail("adminuser@example.com");
		user.setPassword("securePassword");
		user.setRole(Role.ADMIN);
		long userId = userRepository.createUser(user); // ID del usuario generado

		Administrator admin = new Administrator();
		admin.setUser_id(userId);
		admin.setName("Admin User");
		administratorRepository.createAdministrator(admin); // Crear admin

		// Ejecutar y Verificar
		mockMvc.perform(get("/api/superadmin/admin/userId/{userId}", userId)
						.header("Authorization", "Bearer " + superAdminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Admin User"))
				.andExpect(jsonPath("$.user_id").value(userId)); // Validar con el ID real
	}

	@Test
	public void testGetAdministratorByUserIdFailureWithoutAuthentication() throws Exception {
		// Ejecutar y Verificar
		mockMvc.perform(get("/api/superadmin/admin/userId/{userId}", 1L))
				.andExpect(status().isForbidden());
	}

	@Test
	public void testGetAdministratorByUserIdFailureNotFound() throws Exception {
		// Ejecutar y Verificar
		mockMvc.perform(get("/api/superadmin/admin/userId/{userId}", 999L)
						.header("Authorization", "Bearer " + superAdminToken))
				.andExpect(status().isNotFound())
				.andExpect(content().string("El administrador solicitado no existe."));
	}

	@Test
	public void testGetAdministratorByIdFound() throws Exception {
		// Crear usuario y administrador
		User user = new User();
		user.setEmail("adminuser@example.com");
		user.setPassword("securePassword");
		user.setRole(Role.ADMIN);
		long userId = userRepository.createUser(user);

		Administrator admin = new Administrator();
		admin.setUser_id(userId);
		admin.setName("Admin By Id");
		administratorRepository.createAdministrator(admin);

		// Obtener el ID del administrador usando el user_id
		Optional<Administrator> createdAdmin = administratorRepository.getAdministratorByUserId(userId);
		assertThat(createdAdmin).isPresent();
		long adminId = createdAdmin.get().id;

		// Ejecutar y Verificar
		mockMvc.perform(get("/api/superadmin/admin/id/{id}", adminId)
						.header("Authorization", "Bearer " + superAdminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Admin By Id"))
				.andExpect(jsonPath("$.id").value(adminId));
	}

	@Test
	public void testGetAdministratorByIdFailureWithoutAuthentication() throws Exception {
		// Ejecutar y Verificar
		mockMvc.perform(get("/api/superadmin/admin/id/{id}", 1L))
				.andExpect(status().isForbidden());
	}

	@Test
	public void testGetAdministratorByIdNotFound() throws Exception {
		// Ejecutar y Verificar
		mockMvc.perform(get("/api/superadmin/admin/id/{id}", 999L)
						.header("Authorization", "Bearer " + superAdminToken))
				.andExpect(status().isNotFound())
				.andExpect(content().string("El administrador solicitado no existe."));
	}

	@Test
	public void testUpdateAdministratorSuccess() throws Exception {
		// Crear usuario y administrador
		User user = new User();
		user.setEmail("adminuser@example.com");
		user.setPassword("securePassword");
		user.setRole(Role.ADMIN);
		long userId = userRepository.createUser(user);

		Administrator admin = new Administrator();
		admin.setUser_id(userId);
		admin.setName("Original Name");
		administratorRepository.createAdministrator(admin);

		// Obtener el user_id dinámico
		Optional<Administrator> existingAdmin = administratorRepository.getAdministratorByUserId(userId);
		assertThat(existingAdmin).isPresent();

		// Actualizar usando el user_id real
		Administrator updatedAdmin = new Administrator();
		updatedAdmin.setName("Updated Name");
		String jsonContent = objectMapper.writeValueAsString(updatedAdmin);

		// Ejecutar
		mockMvc.perform(put("/api/superadmin/admin/{userId}", userId)
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonContent)
						.header("Authorization", "Bearer " + superAdminToken))
				.andExpect(status().isOk())
				.andExpect(content().string("La actualización se ha hecho correctamente"));

		// Verificar en la base de datos
		Optional<Administrator> adminAfterUpdate = administratorRepository.getAdministratorByUserId(userId);
		assertThat(adminAfterUpdate).isPresent();
		assertThat(adminAfterUpdate.get().name).isEqualTo("Updated Name");
	}

	@Test
	public void testUpdateAdministratorFailureBadRequest() throws Exception {
		// No se inserta ningún administrador
		Administrator updatedAdmin = new Administrator();
		updatedAdmin.setName("Any Name");
		String jsonContent = objectMapper.writeValueAsString(updatedAdmin);

		// Ejecutar y Verificar
		mockMvc.perform(put("/api/superadmin/admin/{userId}", 500L)
						.contentType(MediaType.APPLICATION_JSON)
						.content(jsonContent)
						.header("Authorization", "Bearer " + superAdminToken))
				.andExpect(status().isBadRequest())
				.andExpect(content().string("No se ha podido actualizar."));
	}

	@Test
	public void testUpdateAdministratorFailureWithoutAuthentication() throws Exception {
		// Ejecutar y Verificar
		mockMvc.perform(put("/api/superadmin/admin/{userId}", 1L))
				.andExpect(status().isForbidden());
	}

	@Test
	public void testDeleteAdministratorSuccess() throws Exception {
		// Crear usuario y administrador
		User user = new User();
		user.setEmail("deleteadmin@example.com");
		user.setPassword("password");
		user.setRole(Role.ADMIN);
		long userId = userRepository.createUser(user);

		Administrator admin = new Administrator();
		admin.setUser_id(userId);
		admin.setName("Admin Delete");
		administratorRepository.createAdministrator(admin);

		// Obtener el ID del administrador
		Optional<Administrator> createdAdmin = administratorRepository.getAdministratorByUserId(userId);
		assertThat(createdAdmin).isPresent();
		long adminId = createdAdmin.get().id;

		// Ejecutar
		mockMvc.perform(delete("/api/superadmin/admin/{id}", adminId)
						.header("Authorization", "Bearer " + superAdminToken))
				.andExpect(status().isOk())
				.andExpect(content().string("El administrador con id: " + adminId +
						" se ha eliminado correctamente"));

		// Verificar eliminación
		boolean exists = administratorRepository.getAdministratorById(adminId).isPresent();
		assertThat(exists).isFalse();
	}

	@Test
	public void testDeleteAdministratorFailureWithoutAuthentication() throws Exception {
		// Ejecutar y Verificar
		mockMvc.perform(delete("/api/superadmin/admin/{id}", 1L))
				.andExpect(status().isForbidden());
	}

	@Test
	public void testDeleteAdministratorFailure() throws Exception {
		// Ejecutar y Verificar
		mockMvc.perform(delete("/api/superadmin/admin/{id}", 999L)
						.header("Authorization", "Bearer " + superAdminToken))
				.andExpect(status().isNotFound())
				.andExpect(content().string("No existe el administrador que quieres eliminar"));
	}
}