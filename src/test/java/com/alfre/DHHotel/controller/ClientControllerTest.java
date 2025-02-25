package com.alfre.DHHotel.controller;

import com.alfre.DHHotel.adapter.web.controller.ClientController;
import com.alfre.DHHotel.domain.model.Administrator;
import com.alfre.DHHotel.domain.model.Client;
import com.alfre.DHHotel.usecase.ClientUseCase;
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
public class ClientControllerTest {
    @Mock
    private ClientUseCase clientUseCase;

    @InjectMocks
    private ClientController clientController;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        // Configuramos MockMVC en modo standalone
        mockMvc = MockMvcBuilders.standaloneSetup(clientController).build();
    }

    @Test
    public void whenGetAllClients_success_thenReturnsClientList() throws Exception {
        // Preparamos datos de prueba con una estructura clara
        List<Client> clientList = new ArrayList<>();

        Client client1 = new Client();
        client1.setId(1L);
        client1.setFirst_name("Juan");
        client1.setLast_name("Pérez");

        Client client2 = new Client();
        client2.setId(2L);
        client2.setFirst_name("María");
        client2.setLast_name("García");

        clientList.add(client1);
        clientList.add(client2);

        // Configuramos el comportamiento esperado del caso de uso
        when(clientUseCase.getAllClients()).thenReturn(clientList);

        // Ejecutamos la petición y verificamos la respuesta
        mockMvc.perform(get("/api/admin/clients").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].first_name").value("Juan"))
                .andExpect(jsonPath("$[0].last_name").value("Pérez"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].first_name").value("María"))
                .andDo(print());

        // Verificamos que el caso de uso fue llamado exactamente una vez
        verify(clientUseCase, times(1)).getAllClients();
    }

    @Test
    public void whenGetAllClients_failure_thenReturnsNotFound() throws Exception {
        // Preparar
        List<Client> clientList = new ArrayList<>();

        when(clientUseCase.getAllClients()).thenReturn(clientList);

        // Ejecutar y Verificar
        mockMvc.perform(get("/api/admin/clients")
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No hay clientes registrados en el sistema."));

        verify(clientUseCase).getAllClients();
    }

    @Test
    public void whenGetClientById_success_thenReturnsClient() throws Exception {
        // Preparamos un escenario de prueba específico
        long clientId = 1L;
        Client client = new Client();
        client.setId(clientId);
        client.setFirst_name("Juan");
        client.setLast_name("Pérez");

        // Configuramos el comportamiento esperado
        when(clientUseCase.getClientById(clientId)).thenReturn(Optional.of(client));

        // Ejecutamos la petición
        mockMvc.perform(get("/api/admin/client/{id}", clientId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(clientId))
                .andExpect(jsonPath("$.first_name").value("Juan"))
                .andExpect(jsonPath("$.last_name").value("Pérez"))
                .andDo(print());

        // Verificamos la interacción con el caso de uso
        verify(clientUseCase).getClientById(clientId);
    }

    @Test
    public void whenDeleteClient_success_thenReturnsCorrectMessage() throws Exception {
        // Preparamos el escenario
        long clientId = 1L;
        when(clientUseCase.deleteClient(clientId)).thenReturn(1);

        // Ejecutamos y verificamos
        mockMvc.perform(delete("/api/admin/client/{id}", clientId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Cliente con id: " + clientId + "eliminado correctamente."))
                .andDo(print());

        // Verificamos la llamada al caso de uso
        verify(clientUseCase).deleteClient(clientId);
    }

    // Casos de error
    @Test
    public void whenGetClientById_notFound_thenReturns404() throws Exception {
        // Preparamos un escenario de cliente no encontrado
        long nonExistentId = 999L;
        when(clientUseCase.getClientById(nonExistentId)).thenReturn(Optional.empty());

        // Ejecutamos y verificamos
        mockMvc.perform(get("/api/admin/client/{id}", nonExistentId)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(content().string("El cliente solicitado no existe"))
                .andDo(print());
    }

    @Test
    public void whenDeleteClient_notFound_thenReturnsNotFound() throws Exception {
        // Preparamos un escenario de eliminación fallida
        long nonExistentId = 999L;
        when(clientUseCase.deleteClient(nonExistentId)).thenReturn(0);

        // Ejecutamos y verificamos
        mockMvc.perform(delete("/api/admin/client/{id}", nonExistentId)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(content().string("El cliente no ha sido eliminado"))
                .andDo(print());
    }

    @Test
    public void whenDeleteClient_clientDoesNotExist_thenReturnsNotFound() throws Exception {
        // Preparamos un escenario de eliminación fallida
        long nonExistentId = 999L;
        when(clientUseCase.deleteClient(nonExistentId))
                .thenThrow(new RuntimeException("No existe el cliente que quieres eliminar"));

        // Ejecutamos y verificamos
        mockMvc.perform(delete("/api/admin/client/{id}", nonExistentId)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No existe el cliente que quieres eliminar"))
                .andDo(print());
    }
}