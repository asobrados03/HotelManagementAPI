package com.alfre.DHHotel.controller;

import com.alfre.DHHotel.adapter.web.controller.AdministratorController;
import com.alfre.DHHotel.domain.model.Administrator;
import com.alfre.DHHotel.usecase.AdministratorUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * This class contains the attributes and methods for realize the unit tests
 * of the system administrators controller.
 *
 * @author Alfredo Sobrados González
 */
@ExtendWith(MockitoExtension.class)
public class AdministratorControllerTest {
    @Mock
    private AdministratorUseCase administratorUseCase;

    @InjectMocks
    private AdministratorController administratorController;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Configures MockMvc in standalone mode before each test.
     * <p>
     * This setup method initializes the MockMvc instance with the AdministratorController
     * so that HTTP requests can be simulated during testing.
     * </p>
     */
    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(administratorController).build();
    }

    /**
     * Tests that when all administrators are retrieved successfully,
     * the endpoint returns a JSON list of administrators.
     * <p>
     * This test stubs the AdministratorUseCase to return a list containing one administrator,
     * then performs a GET request to "/api/superadmin/admins" and asserts that the response status,
     * content type, and JSON content are as expected.
     * </p>
     *
     * @throws Exception if any error occurs during the request
     */
    @Test
    public void whenGetAllAdministrators_success_thenReturnsListAdministrators() throws Exception {
        // Arrange: Prepare a list of administrators
        List<Administrator> administratorList = new ArrayList<>();
        Administrator administrator = new Administrator();
        administrator.setId(1L);
        administrator.setName("Admin Test");
        administratorList.add(administrator);

        when(administratorUseCase.getAllAdministrators()).thenReturn(administratorList);

        // Act & Assert: Perform GET request and verify response
        mockMvc.perform(get("/api/superadmin/admins")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Admin Test"))
                .andDo(print());

        verify(administratorUseCase).getAllAdministrators();
    }

    /**
     * Tests that when no administrators are found,
     * the endpoint returns a 404 Not Found with an appropriate error message.
     * <p>
     * The test stubs the AdministratorUseCase to return an empty list and asserts that the response
     * status is 404 with the expected message.
     * </p>
     *
     * @throws Exception if any error occurs during the request
     */
    @Test
    public void whenGetAllAdministrators_failure_thenReturnsNotFound() throws Exception {
        // Arrange: Prepare an empty administrator list
        List<Administrator> administratorList = new ArrayList<>();

        when(administratorUseCase.getAllAdministrators()).thenReturn(administratorList);

        // Act & Assert: Perform GET request and verify that a 404 is returned with the expected message.
        mockMvc.perform(get("/api/superadmin/admins")
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No hay administradores registrados en el sistema."));

        verify(administratorUseCase).getAllAdministrators();
    }

    /**
     * Tests that retrieving an administrator by a valid user ID returns the corresponding administrator.
     * <p>
     * The test stubs the AdministratorUseCase to return an Optional containing an administrator,
     * performs a GET request to "/api/superadmin/admin/userId/{userId}", and verifies the JSON response.
     * </p>
     *
     * @throws Exception if any error occurs during the request
     */
    @Test
    public void whenGetAdministratorByUserId_success_thenReturnsAdministrator() throws Exception {
        // Arrange: Prepare test data
        long userId = 4L;
        Administrator administrator = new Administrator();
        administrator.setId(2L);
        administrator.setUser_id(userId);
        administrator.setName("Alice Parker");

        // Stub the use case to return the administrator for the given userId
        when(administratorUseCase.getAdministratorByUserId(userId)).thenReturn(Optional.of(administrator));

        // Act & Assert: Perform GET request and verify the returned JSON object
        mockMvc.perform(get("/api/superadmin/admin/userId/{userId}", userId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.user_id").value(4))
                .andExpect(jsonPath("$.name").value("Alice Parker"))
                .andDo(print());

        verify(administratorUseCase).getAdministratorByUserId(userId);
    }

    /**
     * Tests that retrieving an administrator by a valid administrator ID returns the corresponding administrator.
     * <p>
     * The test stubs the AdministratorUseCase to return an Optional containing an administrator,
     * performs a GET request to "/api/superadmin/admin/id/{id}", and verifies that the response contains the expected data.
     * </p>
     *
     * @throws Exception if any error occurs during the request
     */
    @Test
    public void whenGetAdministratorById_success_thenReturnsAdministrator() throws Exception {
        // Arrange: Create a sample administrator with the given id
        long adminId = 1L;
        Administrator administrator = new Administrator();
        administrator.setId(adminId);
        administrator.setName("John Doe");
        administrator.setUser_id(123L);

        // Stub the use case to return the administrator for the given id
        when(administratorUseCase.getAdministratorById(adminId)).thenReturn(Optional.of(administrator));

        // Act & Assert: Perform GET request and verify JSON response
        mockMvc.perform(get("/api/superadmin/admin/id/{id}", adminId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(adminId))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.user_id").value(123))
                .andDo(print());

        verify(administratorUseCase).getAdministratorById(adminId);
    }

    /**
     * Tests that retrieving an administrator by an ID that does not exist returns a 404 Not Found.
     * <p>
     * The test stubs the AdministratorUseCase to return an empty Optional, performs a GET request,
     * and verifies that the response status is 404 with the appropriate error message.
     * </p>
     *
     * @throws Exception if any error occurs during the request
     */
    @Test
    public void whenGetAdministratorById_notFound_thenReturns404() throws Exception {
        long adminId = 999L;

        when(administratorUseCase.getAdministratorById(adminId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/superadmin/admin/id/{id}", adminId)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(content().string("El administrador solicitado no existe."))
                .andDo(print());
    }

    /**
     * Tests that updating an administrator is successful when the update operation affects a row.
     * <p>
     * The test stubs the AdministratorUseCase to return 1 (indicating success),
     * sends a PUT request with the updated administrator data, and verifies that the response message is as expected.
     * </p>
     *
     * @throws Exception if any error occurs during the request
     */
    @Test
    public void whenUpdateAdministrator_success_thenReturnsOk() throws Exception {
        // Arrange: Prepare the updated administrator data
        long userId = 1L;
        Administrator administrator = new Administrator();
        administrator.setName("John Updated");
        administrator.setUser_id(userId);

        // Stub the update method to return 1 (one row updated)
        when(administratorUseCase.updateAdministrator(any(Administrator.class), eq(userId)))
                .thenReturn(1);

        // Act & Assert: Perform PUT request and verify response message
        mockMvc.perform(put("/api/superadmin/admin/{userId}", userId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(administrator)))
                .andExpect(status().isOk())
                .andExpect(content().string("La actualización se ha hecho correctamente"))
                .andDo(print());
    }

    /**
     * Tests that when updating an administrator fails (no rows updated),
     * the endpoint returns a 400 Bad Request with an appropriate error message.
     * <p>
     * The test stubs the AdministratorUseCase to return 0 and verifies the response message.
     * </p>
     *
     * @throws Exception if any error occurs during the request
     */
    @Test
    public void whenUpdateAdministrator_fails_thenReturnsBadRequest() throws Exception {
        long userId = 1L;
        Administrator administrator = new Administrator();
        administrator.setName("John Updated");

        when(administratorUseCase.updateAdministrator(any(Administrator.class), eq(userId)))
                .thenReturn(0);

        mockMvc.perform(put("/api/superadmin/admin/{userId}", userId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(administrator)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("No se ha podido actualizar."));
    }

    /**
     * Tests that deleting an administrator is successful when the deletion operation returns success.
     * <p>
     * The test stubs the AdministratorUseCase to return 1 for a successful deletion,
     * sends a DELETE request, and verifies that the response contains the expected success message.
     * </p>
     *
     * @throws Exception if any error occurs during the request
     */
    @Test
    public void whenDeleteAdministrator_success_thenReturnsOk() throws Exception {
        long adminId = 1L;

        when(administratorUseCase.deleteAdministrator(adminId))
                .thenReturn(1);

        mockMvc.perform(delete("/api/superadmin/admin/{id}", adminId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("El administrador con id: " + adminId + " se ha eliminado correctamente"))
                .andDo(print());

        verify(administratorUseCase).deleteAdministrator(adminId);
    }

    /**
     * Tests that when deleting an administrator fails (returns 0),
     * the endpoint returns a 400 Bad Request with an appropriate error message.
     * <p>
     * The test stubs the AdministratorUseCase to return 0 and verifies that the response message is as expected.
     * </p>
     *
     * @throws Exception if any error occurs during the request
     */
    @Test
    public void whenDeleteAdministrator_fails_thenReturnsBadRequest() throws Exception {
        long adminId = 999L;

        when(administratorUseCase.deleteAdministrator(adminId)).thenReturn(0);

        mockMvc.perform(delete("/api/superadmin/admin/{id}", adminId)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("No se ha podido eliminar."))
                .andDo(print());
    }

    /**
     * Tests that when attempting to delete an administrator that does not exist,
     * the endpoint returns a 404 Not Found with the appropriate error message.
     * <p>
     * The test stubs the AdministratorUseCase to throw a RuntimeException for a non-existent administrator,
     * then verifies that the response status and message match the expected values.
     * </p>
     *
     * @throws Exception if any error occurs during the request
     */
    @Test
    public void whenDeleteAdministrator_adminDoesNotExist_thenReturnsNotFound() throws Exception {
        long adminId = 999L;

        when(administratorUseCase.deleteAdministrator(adminId))
                .thenThrow(new RuntimeException("No existe el administrador que quieres eliminar"));

        mockMvc.perform(delete("/api/superadmin/admin/{id}", adminId)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No existe el administrador que quieres eliminar"))
                .andDo(print());
    }
}
