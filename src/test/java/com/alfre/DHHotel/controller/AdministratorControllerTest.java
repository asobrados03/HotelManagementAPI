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

@ExtendWith(MockitoExtension.class)
public class AdministratorControllerTest {
    @Mock
    private AdministratorUseCase administratorUseCase;

    @InjectMocks
    private AdministratorController administratorController;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setup() {
        // Configuramos MockMVC en modo standalone
        mockMvc = MockMvcBuilders.standaloneSetup(administratorController).build();
    }

    @Test
    public void whenGetAllAdministrators_success_thenReturnsListAdministrators() throws Exception {
        // Preparar
        List<Administrator> administratorList = new ArrayList<>();
        Administrator administrator = new Administrator();
        administrator.setId(1L);
        administrator.setName("Admin Test");
        administratorList.add(administrator);

        when(administratorUseCase.getAllAdministrators()).thenReturn(administratorList);

        // Ejecutar y Verificar
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

    @Test
    public void whenGetAllAdministrators_failure_thenReturnsNotFound() throws Exception {
        // Preparar
        List<Administrator> administratorList = new ArrayList<>();

        when(administratorUseCase.getAllAdministrators()).thenReturn(administratorList);

        // Ejecutar y Verificar
        mockMvc.perform(get("/api/superadmin/admins")
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No hay administradores registrados en el sistema."));

        verify(administratorUseCase).getAllAdministrators();
    }

    @Test
    public void whenGetAdministratorByUserId_success_thenReturnsAdministrator() throws Exception {
        // Preparación de datos
        long userId = 4L;
        Administrator administrator = new Administrator();
        administrator.setId(2L);
        administrator.setUser_id(userId);
        administrator.setName("Alice Parker");

        // Configuración del mock
        when(administratorUseCase.getAdministratorByUserId(userId)).thenReturn(Optional.of(administrator));

        // Ejecutar y Verificar
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

    @Test
    public void whenGetAdministratorById_success_thenReturnsAdministrator() throws Exception {
        // Preparación: Creamos un administrador de prueba
        long adminId = 1L;
        Administrator administrator = new Administrator();
        administrator.setId(adminId);
        administrator.setName("John Doe");
        administrator.setUser_id(123L);

        // Configuramos el comportamiento esperado del caso de uso
        when(administratorUseCase.getAdministratorById(adminId)).thenReturn(Optional.of(administrator));

        // Ejecutamos la prueba y verificamos
        mockMvc.perform(get("/api/superadmin/admin/id/{id}", adminId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(adminId))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.user_id").value(123))
                .andDo(print());

        // Verificamos que el método del caso de uso fue llamado
        verify(administratorUseCase).getAdministratorById(adminId);
    }

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

    @Test
    public void whenUpdateAdministrator_success_thenReturnsOk() throws Exception {
        // Preparación
        long userId = 1L;
        Administrator administrator = new Administrator();
        administrator.setName("John Updated");
        administrator.setUser_id(userId);

        // Configuramos el caso de éxito (1 fila afectada)
        when(administratorUseCase.updateAdministrator(any(Administrator.class), eq(userId)))
                .thenReturn(1);

        // Ejecutamos y verificamos
        mockMvc.perform(put("/api/superadmin/admin/{userId}", userId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(administrator)))
                .andExpect(status().isOk())
                .andExpect(content().string("La actualización se ha hecho correctamente"))
                .andDo(print());
    }

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
