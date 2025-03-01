package com.alfre.DHHotel.controller;

import com.alfre.DHHotel.adapter.web.controller.ClientController;
import com.alfre.DHHotel.domain.model.Client;
import com.alfre.DHHotel.usecase.ClientUseCase;
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
 * of the customers operations controller.
 *
 * @author Alfredo Sobrados González
 */
@ExtendWith(MockitoExtension.class)
public class ClientControllerTest {
    @Mock
    private ClientUseCase clientUseCase;

    @InjectMocks
    private ClientController clientController;

    private MockMvc mockMvc;

    /**
     * Configures MockMvc in standalone mode using the ClientController before each test.
     */
    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(clientController).build();
    }

    /**
     * Tests that when retrieving all clients successfully, the endpoint returns a list of clients.
     * <p>
     * The test prepares a list with two clients, stubs the clientUseCase to return that list,
     * performs a GET request to "/api/admin/clients", and asserts that the response contains both clients
     * with the expected properties.
     * </p>
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    public void whenGetAllClients_success_thenReturnsClientList() throws Exception {
        // Prepare test data
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

        // Stub the use case to return the client list.
        when(clientUseCase.getAllClients()).thenReturn(clientList);

        // Execute GET request and verify response.
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

        // Verify that getAllClients was called once.
        verify(clientUseCase, times(1)).getAllClients();
    }

    /**
     * Tests that when no clients are found, the endpoint returns a 404 Not Found with an appropriate message.
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    public void whenGetAllClients_failure_thenReturnsNotFound() throws Exception {
        // Prepare an empty client list.
        List<Client> clientList = new ArrayList<>();

        when(clientUseCase.getAllClients()).thenReturn(clientList);

        // Execute GET request and verify 404 response.
        mockMvc.perform(get("/api/admin/clients")
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No hay clientes registrados en el sistema."));

        verify(clientUseCase).getAllClients();
    }

    /**
     * Tests that when retrieving a client by a valid ID, the endpoint returns the corresponding client.
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    public void whenGetClientById_success_thenReturnsClient() throws Exception {
        // Prepare test data for a client with ID 1.
        long clientId = 1L;
        Client client = new Client();
        client.setId(clientId);
        client.setFirst_name("Juan");
        client.setLast_name("Pérez");

        // Stub the use case to return the client.
        when(clientUseCase.getClientById(clientId)).thenReturn(Optional.of(client));

        // Execute GET request and verify the JSON response.
        mockMvc.perform(get("/api/admin/client/{id}", clientId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(clientId))
                .andExpect(jsonPath("$.first_name").value("Juan"))
                .andExpect(jsonPath("$.last_name").value("Pérez"))
                .andDo(print());

        verify(clientUseCase).getClientById(clientId);
    }

    /**
     * Tests that when deleting a client successfully, the endpoint returns the correct success message.
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    public void whenDeleteClient_success_thenReturnsCorrectMessage() throws Exception {
        // Prepare test scenario.
        long clientId = 1L;
        when(clientUseCase.deleteClient(clientId)).thenReturn(1);

        // Execute DELETE request and verify response.
        mockMvc.perform(delete("/api/admin/client/{id}", clientId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("Cliente con id: " + clientId + " eliminado correctamente."))
                .andDo(print());

        verify(clientUseCase).deleteClient(clientId);
    }

    /**
     * Tests that when retrieving a client by a non-existent ID, the endpoint returns a 404 Not Found.
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    public void whenGetClientById_notFound_thenReturns404() throws Exception {
        // Prepare scenario with non-existent client ID.
        long nonExistentId = 999L;
        when(clientUseCase.getClientById(nonExistentId)).thenReturn(Optional.empty());

        // Execute GET request and verify response.
        mockMvc.perform(get("/api/admin/client/{id}", nonExistentId)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(content().string("El cliente solicitado no existe"))
                .andDo(print());
    }

    /**
     * Tests that when deleting a client that is not found, the endpoint returns a 404 Not Found with a message.
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    public void whenDeleteClient_notFound_thenReturnsNotFound() throws Exception {
        // Prepare scenario where delete returns 0 (failure).
        long nonExistentId = 999L;
        when(clientUseCase.deleteClient(nonExistentId)).thenReturn(0);

        // Execute DELETE request and verify response.
        mockMvc.perform(delete("/api/admin/client/{id}", nonExistentId)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(content().string("El cliente no ha sido eliminado"))
                .andDo(print());
    }

    /**
     * Tests that when deleting a client that does not exist (throws exception),
     * the endpoint returns a 404 Not Found with the appropriate error message.
     *
     * @throws Exception if an error occurs during the HTTP request
     */
    @Test
    public void whenDeleteClient_clientDoesNotExist_thenReturnsNotFound() throws Exception {
        // Prepare scenario: simulate deletion throwing an exception.
        long nonExistentId = 999L;
        when(clientUseCase.deleteClient(nonExistentId))
                .thenThrow(new RuntimeException("No existe el cliente que quieres eliminar"));

        // Execute DELETE request and verify response.
        mockMvc.perform(delete("/api/admin/client/{id}", nonExistentId)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(content().string("No existe el cliente que quieres eliminar"))
                .andDo(print());
    }
}