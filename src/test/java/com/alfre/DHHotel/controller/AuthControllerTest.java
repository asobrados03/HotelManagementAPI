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

    @BeforeEach
    void setup() {
        // Configuramos MockMVC en modo standalone
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Test
    public void whenLogin_withValidCredentials_thenReturnsToken() throws Exception {
        // Preparar
        LoginRequest loginRequest = new LoginRequest("user", "password");
        when(authUseCase.login(any(LoginRequest.class)))
                .thenReturn(new AuthResponse("token123"));

        // Ejecutar
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token123"));

        // Verificar
        verify(authUseCase).login(any(LoginRequest.class));
    }

    @Test
    public void whenLogin_withInvalidCredentials_thenReturnsNotFound() throws Exception {
        // Preparar
        LoginRequest loginRequest = new LoginRequest("user", "wrongpassword");

        when(authUseCase.login(any(LoginRequest.class)))
                .thenThrow(new RuntimeException("Credenciales inválidas"));

        // Ejecutar y verificar
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isNotFound()) // HTTP 404 Not Found
                .andExpect(jsonPath("$.error").value("Credenciales inválidas"));

        // Verificar que se llamó al método login
        verify(authUseCase).login(any(LoginRequest.class));
    }

    @Test
    public void whenLogin_withNullCredentials_thenReturnsNotFound() throws Exception {
        // Preparar
        LoginRequest loginRequest = new LoginRequest(null, null);

        when(authUseCase.login(any(LoginRequest.class)))
                .thenThrow(new IllegalArgumentException("Los campos obligatorios no pueden ser nulos"));

        // Ejecutar y verificar
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest()) // HTTP 400 Bad Request
                .andExpect(jsonPath("$.error")
                        .value("Los campos obligatorios no pueden ser nulos"));

        // Verificar que se llamó al método login
        verify(authUseCase).login(any(LoginRequest.class));
    }

    @Test
    public void whenRegisterClient_withValidFields_thenReturnsToken() throws Exception {
        // Preparar
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("username@example.com");
        registerRequest.setPassword("securePassword");
        registerRequest.setRole(Role.CLIENT);
        registerRequest.setFirst_name("Melissa");
        registerRequest.setLast_name("Ericsson");
        registerRequest.setPhone("+34 655 456 767");

        when(authUseCase.registerClient(any(RegisterRequest.class)))
                .thenReturn(new AuthResponse("token123"));

        // Ejecutar
        mockMvc.perform(post("/api/auth/register/client")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token123"));

        // Verificar
        verify(authUseCase).registerClient(any(RegisterRequest.class));
    }

    @Test
    public void whenRegisterClient_withInvalidFields_thenThrowsException() throws Exception {
        // Preparar: Creamos un RegisterRequest con datos inválidos (por ejemplo, un email mal formado)
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("invalid-email"); // email inválido
        registerRequest.setPassword("password");
        registerRequest.setRole(Role.CLIENT);
        registerRequest.setFirst_name("Melissa");
        registerRequest.setLast_name("Ericsson");
        registerRequest.setPhone("+34 655 456 767");

        // Stub: Simulamos que al intentar registrar se lanza una excepción
        when(authUseCase.registerClient(any(RegisterRequest.class)))
                .thenThrow(new IllegalArgumentException("Formato de correo electrónico inválido"));

        // Ejecutar & Verificar: Se realiza la petición y se verifica que se devuelva un BAD_REQUEST con el mensaje esperado
        mockMvc.perform(post("/api/auth/register/client")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Formato de correo electrónico inválido"));

        verify(authUseCase).registerClient(any(RegisterRequest.class));
    }

    @Test
    public void whenRegisterAdmin_withValidData_thenReturnsToken() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("admin@example.com");
        registerRequest.setPassword("password");
        registerRequest.setRole(Role.ADMIN);
        registerRequest.setName("Joe Roman");

        AuthResponse response = new AuthResponse("adminToken123");
        when(authUseCase.registerAdministrator(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register/admin")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("adminToken123"));

        verify(authUseCase).registerAdministrator(any(RegisterRequest.class));
    }

    @Test
    public void whenRegisterAdmin_withInvalidData_thenReturnsBadRequest() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("invalid-email");
        registerRequest.setPassword("password");
        registerRequest.setRole(Role.ADMIN);
        registerRequest.setName("Joe Roman");

        when(authUseCase.registerAdministrator(any(RegisterRequest.class)))
                .thenThrow(new RuntimeException("Datos inválidos para administrador"));

        mockMvc.perform(post("/api/auth/register/admin")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Datos inválidos para administrador"));

        verify(authUseCase).registerAdministrator(any(RegisterRequest.class));
    }

    @Test
    @WithMockUser
    public void whenGetInfoClient_withValidUser_thenReturnsClientInfo() throws Exception {
        // Creamos un objeto ClientDTO con los datos esperados
        ClientDTO clientDTO = ClientDTO.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .phone("123456789")
                .build();

        // Stub: authUseCase.getInfoClient debe retornar el DTO
        when(authUseCase.getInfoClient(any(User.class))).thenReturn(clientDTO);

        // Realizamos la petición e imprimimos la respuesta para depuración (andDo(print()))
        mockMvc.perform(get("/api/auth/me/client")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.phone").value("123456789"));

        // Verificamos que se invoca el método en authUseCase
        verify(authUseCase).getInfoClient(any(User.class));
    }

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

    @Test
    @WithMockUser
    public void whenChangePassword_success_thenReturnsSuccessMessage() throws Exception {
        // Suponemos que UserDTO tiene un campo newPassword
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

    @Test
    @WithMockUser
    public void whenChangeEmail_illegalArgument_thenReturnsBadRequest() throws Exception {
        UserDTO request = new UserDTO();
        request.newEmail = "invalid_email";

        when(authUseCase.changeEmail(any(com.alfre.DHHotel.domain.model.User.class), eq("invalid_email")))
                .thenThrow(new IllegalArgumentException("Formato de correo electrónico inválido"));

        mockMvc.perform(put("/api/auth/change-email")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Formato de correo electrónico inválido"));

        verify(authUseCase).changeEmail(any(User.class), eq("invalid_email"));
    }

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