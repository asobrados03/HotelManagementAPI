package com.alfre.DHHotel.controller;

import com.alfre.DHHotel.adapter.web.controller.AuthController;
import com.alfre.DHHotel.adapter.web.dto.*;
import com.alfre.DHHotel.domain.model.Role;
import com.alfre.DHHotel.domain.model.User;
import com.alfre.DHHotel.usecase.AuthUseCase;
import com.alfre.DHHotel.usecase.ClientUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * This class contains the attributes and methods for realize the unit tests
 * of the authentication and authorization operations controller.
 *
 * @author Alfredo Sobrados González
 */
@ExtendWith(MockitoExtension.class)
public class AuthControllerTest {
    @Mock
    private AuthUseCase authUseCase;

    @Mock
    private ClientUseCase clientUseCase;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Configures MockMvc in standalone mode with the AuthController.
     */
    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    /**
     * Tests that when a login request is made with valid credentials, the endpoint returns a token.
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    public void whenLogin_withValidCredentials_thenReturnsToken() throws Exception {
        // Arrange: create a valid login request and stub the use case to return a token.
        LoginRequest loginRequest = new LoginRequest("user", "password");
        when(authUseCase.login(any(LoginRequest.class)))
                .thenReturn(new AuthResponse("token123"));

        // Act & Assert: perform the POST request and expect a 200 OK with a token in the response.
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token123"));

        // Verify that the login method was called.
        verify(authUseCase).login(any(LoginRequest.class));
    }

    /**
     * Tests that when a login request is made with invalid credentials, the endpoint returns a 404 Not Found with an error.
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    public void whenLogin_withInvalidCredentials_thenReturnsNotFound() throws Exception {
        // Arrange: create a login request with an incorrect password.
        LoginRequest loginRequest = new LoginRequest("user", "wrongpassword");
        when(authUseCase.login(any(LoginRequest.class)))
                .thenThrow(new RuntimeException("Credenciales inválidas"));

        // Act & Assert: perform the POST request and expect a 404 Not Found with the specified error message.
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Credenciales inválidas"));

        // Verify that the login method was invoked.
        verify(authUseCase).login(any(LoginRequest.class));
    }

    /**
     * Tests that when a login request is made with null credentials, the endpoint returns a 400 Bad Request.
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    public void whenLogin_withNullCredentials_thenReturnsNotFound() throws Exception {
        // Arrange: create a login request with null values.
        LoginRequest loginRequest = new LoginRequest(null, null);
        when(authUseCase.login(any(LoginRequest.class)))
                .thenThrow(new IllegalArgumentException("Los campos obligatorios no pueden ser nulos"));

        // Act & Assert: perform the POST request and expect a 400 Bad Request with the appropriate error message.
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Los campos obligatorios no pueden ser nulos"));

        // Verify that the login method was called.
        verify(authUseCase).login(any(LoginRequest.class));
    }

    /**
     * Tests that registering a client with valid fields returns a token.
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    public void whenRegisterClient_withValidFields_thenReturnsToken() throws Exception {
        // Arrange: prepare a valid client registration request.
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("username@example.com");
        registerRequest.setPassword("securePassword");
        registerRequest.setRole(Role.CLIENT);
        registerRequest.setFirst_name("Melissa");
        registerRequest.setLast_name("Ericsson");
        registerRequest.setPhone("+34 655 456 767");

        when(authUseCase.registerClient(any(RegisterRequest.class)))
                .thenReturn(new AuthResponse("token123"));

        // Act & Assert: perform the POST request and verify a token is returned.
        mockMvc.perform(post("/api/auth/register/client")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token123"));

        verify(authUseCase).registerClient(any(RegisterRequest.class));
    }

    /**
     * Tests that registering a client with invalid fields (e.g. malformed email) returns a 400 Bad Request.
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    public void whenRegisterClient_withInvalidFields_thenThrowsException() throws Exception {
        // Arrange: prepare a registration request with an invalid email.
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("invalid-email"); // Invalid email format
        registerRequest.setPassword("password");
        registerRequest.setRole(Role.CLIENT);
        registerRequest.setFirst_name("Melissa");
        registerRequest.setLast_name("Ericsson");
        registerRequest.setPhone("+34 655 456 767");

        // Stub: simulate that registration fails due to invalid email.
        when(authUseCase.registerClient(any(RegisterRequest.class)))
                .thenThrow(new IllegalArgumentException("Formato de correo electrónico inválido"));

        // Act & Assert: perform the POST request and verify a 400 Bad Request is returned with the error message.
        mockMvc.perform(post("/api/auth/register/client")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Formato de correo electrónico inválido"));

        verify(authUseCase).registerClient(any(RegisterRequest.class));
    }

    /**
     * Tests that registering an administrator with valid data returns a token.
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    public void whenRegisterAdmin_withValidData_thenReturnsToken() throws Exception {
        // Arrange: prepare a valid admin registration request.
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("admin@example.com");
        registerRequest.setPassword("password");
        registerRequest.setRole(Role.ADMIN);
        registerRequest.setName("Joe Roman");

        AuthResponse response = new AuthResponse("adminToken123");
        when(authUseCase.registerAdministrator(any(RegisterRequest.class))).thenReturn(response);

        // Act & Assert: perform the POST request and verify the token is returned.
        mockMvc.perform(post("/api/auth/register/admin")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("adminToken123"));

        verify(authUseCase).registerAdministrator(any(RegisterRequest.class));
    }

    /**
     * Tests that registering an administrator with invalid data returns a 400 Bad Request.
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    public void whenRegisterAdmin_withInvalidData_thenReturnsBadRequest() throws Exception {
        // Arrange: prepare an admin registration request with an invalid email.
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("invalid-email");
        registerRequest.setPassword("password");
        registerRequest.setRole(Role.ADMIN);
        registerRequest.setName("Joe Roman");

        when(authUseCase.registerAdministrator(any(RegisterRequest.class)))
                .thenThrow(new RuntimeException("Datos inválidos para administrador"));

        // Act & Assert: perform the POST request and expect a 400 Bad Request with the error message.
        mockMvc.perform(post("/api/auth/register/admin")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Datos inválidos para administrador"));

        verify(authUseCase).registerAdministrator(any(RegisterRequest.class));
    }

    /**
     * Tests that when retrieving client information with a valid authenticated user, the endpoint returns
     * the correct client info.
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    @WithMockUser
    public void whenGetInfoClient_withValidUser_thenReturnsClientInfo() throws Exception {
        // Arrange: build the expected ClientDTO.
        ClientDTO clientDTO = ClientDTO.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .phone("123456789")
                .build();

        when(authUseCase.getInfoClient(any(User.class))).thenReturn(clientDTO);

        // Act & Assert: perform GET request and verify the JSON response.
        mockMvc.perform(get("/api/auth/me/client")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.phone").value("123456789"));

        verify(authUseCase).getInfoClient(any(User.class));
    }

    /**
     * Tests that when an error occurs while retrieving client information, the endpoint returns a 500 Internal Server Error.
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    @WithMockUser
    public void whenGetInfoClient_withError_thenReturnsInternalServerError() throws Exception {
        when(authUseCase.getInfoClient(any(User.class)))
                .thenThrow(new RuntimeException("Error interno"));

        mockMvc.perform(get("/api/auth/me/client")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error")
                        .value("Autenticación no válida o error en el servicio"));

        verify(authUseCase).getInfoClient(any(User.class));
    }

    /**
     * Tests that when retrieving administrator information with a valid authenticated admin user,
     * the endpoint returns the correct admin info.
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    @WithMockUser(username = "admin", roles = {"ROLE_ADMIN"})
    public void whenGetInfoAdmin_withValidUser_thenReturnsAdminInfo() throws Exception {
        AdminDTO adminDTO = AdminDTO.builder()
                .email("admin@example.com")
                .role(Role.ADMIN)
                .name("Admin Name")
                .build();
        when(authUseCase.getInfoAdmin(any(User.class))).thenReturn(adminDTO);

        mockMvc.perform(get("/api/auth/me/admin")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@example.com"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.name").value("Admin Name"));

        verify(authUseCase).getInfoAdmin(any(User.class));
    }

    /**
     * Tests that when an error occurs while retrieving administrator information,
     * the endpoint returns a 500 Internal Server Error.
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    @WithMockUser
    public void whenGetInfoAdmin_withError_thenReturnsInternalServerError() throws Exception {
        when(authUseCase.getInfoAdmin(any(User.class)))
                .thenThrow(new RuntimeException("Error interno"));

        mockMvc.perform(get("/api/auth/me/admin")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error")
                        .value("Autenticación no válida o error en el servicio"));

        verify(authUseCase).getInfoAdmin(any(User.class));
    }

    /**
     * Tests that updating a client's profile with valid data returns the updated profile.
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    @WithMockUser
    public void whenUpdateProfile_success_thenReturnsUpdatedProfile() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("NewFirst");
        request.setLastName("NewLast");

        ClientDTO updatedProfile = ClientDTO.builder()
                .firstName("NewFirst")
                .lastName("NewLast")
                .email("client@example.com")
                .phone("123456789")
                .build();

        when(clientUseCase.updateClientProfile(any(User.class), any(UpdateProfileRequest.class)))
                .thenReturn(updatedProfile);

        mockMvc.perform(put("/api/auth/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("NewFirst"))
                .andExpect(jsonPath("$.lastName").value("NewLast"));

        verify(clientUseCase).updateClientProfile(any(User.class), any(UpdateProfileRequest.class));
    }

    /**
     * Tests that if updating the client profile results in an IllegalArgumentException,
     * the endpoint returns a 400 Bad Request with the corresponding error message.
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    @WithMockUser
    public void whenUpdateProfile_illegalArgument_thenReturnsBadRequest() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("NewFirst");
        request.setLastName("NewLast");

        when(clientUseCase.updateClientProfile(any(User.class), any(UpdateProfileRequest.class)))
                .thenThrow(new IllegalArgumentException("Datos de perfil inválidos"));

        mockMvc.perform(put("/api/auth/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Datos de perfil inválidos"));

        verify(clientUseCase).updateClientProfile(any(User.class), any(UpdateProfileRequest.class));
    }

    /**
     * Tests that if updating the client profile throws a RuntimeException indicating the client is not registered,
     * the endpoint returns a 404 Not Found with the appropriate error message.
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    @WithMockUser
    public void whenUpdateProfile_runtimeException_thenReturnsNotFound() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("NewFirst");
        request.setLastName("NewLast");

        when(clientUseCase.updateClientProfile(any(User.class), any(UpdateProfileRequest.class)))
                .thenThrow(new RuntimeException("Not registered"));

        mockMvc.perform(put("/api/auth/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Cliente no registrado"));

        verify(clientUseCase).updateClientProfile(any(User.class), any(UpdateProfileRequest.class));
    }

    /**
     * Tests that when changing the password with valid data, the endpoint returns a success message.
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    @WithMockUser
    public void whenChangePassword_success_thenReturnsSuccessMessage() throws Exception {
        // Assume that UserDTO has a field newPassword.
        UserDTO request = new UserDTO();
        request.newPassword = "newPassword123";

        when(authUseCase.changePassword(any(User.class), eq("newPassword123")))
                .thenReturn("Cambio de contraseña CORRECTO.");

        mockMvc.perform(put("/api/auth/change-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Cambio de contraseña CORRECTO."));

        verify(authUseCase).changePassword(any(User.class), eq("newPassword123"));
    }

    /**
     * Tests that when changing the password fails, the endpoint returns a 500 Internal Server Error with the proper error message.
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    @WithMockUser
    public void whenChangePassword_failure_thenReturnsInternalServerError() throws Exception {
        UserDTO request = new UserDTO();
        request.newPassword = "newPassword123";

        when(authUseCase.changePassword(any(User.class), eq("newPassword123")))
                .thenThrow(new RuntimeException("Error al cambiar contraseña"));

        mockMvc.perform(put("/api/auth/change-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error")
                        .value("Autenticación no válida o error en el servicio"));

        verify(authUseCase).changePassword(any(User.class), eq("newPassword123"));
    }

    /**
     * Tests that when changing the email with valid data, the endpoint returns a success message.
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    @WithMockUser
    public void whenChangeEmail_success_thenReturnsSuccessMessage() throws Exception {
        UserDTO request = new UserDTO();
        request.newEmail = "newemail@example.com";

        when(authUseCase.changeEmail(any(User.class), eq("newemail@example.com")))
                .thenReturn("Cambio de email CORRECTO.");

        mockMvc.perform(put("/api/auth/change-email")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Cambio de email CORRECTO."));

        verify(authUseCase).changeEmail(any(User.class), eq("newemail@example.com"));
    }

    /**
     * Tests that when changing the email with an invalid email format, the endpoint returns a 400 Bad Request.
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    @WithMockUser
    public void whenChangeEmail_illegalArgument_thenReturnsBadRequest() throws Exception {
        UserDTO request = new UserDTO();
        request.newEmail = "invalid_email";

        when(authUseCase.changeEmail(any(User.class), eq("invalid_email")))
                .thenThrow(new IllegalArgumentException("Formato de correo electrónico inválido"));

        mockMvc.perform(put("/api/auth/change-email")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Formato de correo electrónico inválido"));

        verify(authUseCase).changeEmail(any(User.class), eq("invalid_email"));
    }

    /**
     * Tests that when changing the email results in a runtime exception, the endpoint returns a 500 Internal Server Error.
     *
     * @throws Exception if an error occurs during the request
     */
    @Test
    @WithMockUser
    public void whenChangeEmail_runtimeException_thenReturnsInternalServerError() throws Exception {
        UserDTO request = new UserDTO();
        request.newEmail = "newemail@example.com";

        when(authUseCase.changeEmail(any(User.class), eq("newemail@example.com")))
                .thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(put("/api/auth/change-email")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error")
                        .value("Autenticación no válida o error en el servicio"));

        verify(authUseCase).changeEmail(any(User.class), eq("newemail@example.com"));
    }
}