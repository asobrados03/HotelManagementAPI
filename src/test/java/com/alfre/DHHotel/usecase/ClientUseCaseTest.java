package com.alfre.DHHotel.usecase;

import com.alfre.DHHotel.adapter.web.dto.ClientDTO;
import com.alfre.DHHotel.adapter.web.dto.UpdateProfileRequest;
import com.alfre.DHHotel.domain.model.Client;
import com.alfre.DHHotel.domain.model.User;
import com.alfre.DHHotel.domain.repository.ClientRepository;
import com.alfre.DHHotel.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ClientUseCaseTest {
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private UserRepository userRepository;

    private ClientUseCase clientUseCase;

    @BeforeEach
    public void setup() {
        clientUseCase = new ClientUseCase(clientRepository, userRepository);
    }

    @Test
    public void getAllClients_success() {
        // Arrange: Preparamos una lista de clientes
        List<Client> clientList = new ArrayList<>();
        Client client = new Client();
        // Configuramos las propiedades de client si es necesario
        clientList.add(client);
        when(clientRepository.getAllClients()).thenReturn(clientList);

        // Act: Ejecutamos el método
        List<Client> result = clientUseCase.getAllClients();

        // Assert: Verificamos que el resultado sea el esperado y se llamó al método del repository
        assertEquals(clientList, result);
        verify(clientRepository).getAllClients();
    }

    @Test
    public void getAdministratorById_success() {
        // Arrange: Creamos un cliente y simulamos su retorno para el id dado
        Client client = new Client();
        when(clientRepository.getClientById(10L)).thenReturn(Optional.of(client));

        // Act
        Optional<Client> result = clientUseCase.getClientById(10L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(client, result.get());
        verify(clientRepository).getClientById(10L);
    }

    @Test
    public void updateClientProfile_success() {
        // Arrange: Creamos un usuario y un update profile request
        User user = new User();
        user.setId(1L);
        user.setEmail("client@example.com");

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Ron");
        request.setLastName("Smith");
        request.setPhone("+34 612 345 677");

        Client client = new Client();
        Client clientUpdated = new Client();

        when(clientRepository.getClientByUserId(user.id)).thenReturn(Optional.of(client));
        when(clientRepository.updateClient(client)).thenReturn(clientUpdated);

        // Act
        ClientDTO response = clientUseCase.updateClientProfile(user, request);

        // Assert
        ClientDTO responseExpected = ClientDTO.builder()
                .email(user.email)
                .firstName(clientUpdated.first_name)
                .lastName(clientUpdated.last_name)
                .phone(clientUpdated.phone)
                .build();

        assertNotNull(response);
        assertEquals(response, responseExpected);
    }

    @Test
    public void updateClientProfile_nullFirstAndLastName_shouldThrowException() {
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setEmail("client@example.com");

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Ron");
        request.setLastName(null);
        request.setPhone("+34 612 345 677");

        // Act & Assert: Verificamos que se lanza la excepción esperada
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            clientUseCase.updateClientProfile(user, request)
        );

        // Validamos el mensaje de la excepción
        assertEquals("El nombre y el apellido son obligatorios", exception.getMessage());
    }

    @Test
    public void updateClientProfile_nullPhone_shouldThrowException() {
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setEmail("client@example.com");

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Ron");
        request.setLastName("Smith");
        request.setPhone(null);

        // Act & Assert: Verificamos que se lanza la excepción esperada
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                clientUseCase.updateClientProfile(user, request)
        );

        // Validamos el mensaje de la excepción
        assertEquals("Formato de teléfono inválido u obligatorio", exception.getMessage());
    }

    @Test
    public void updateClientProfile_invalidFormatPhone_shouldThrowException() {
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setEmail("client@example.com");

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Ron");
        request.setLastName("Smith");
        request.setPhone("444-3345");

        // Act & Assert: Verificamos que se lanza la excepción esperada
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                clientUseCase.updateClientProfile(user, request)
        );

        // Validamos el mensaje de la excepción
        assertEquals("Formato de teléfono inválido u obligatorio", exception.getMessage());
    }

    @Test
    public void updateClientProfile_userRelatedClientNotFound_shouldThrowException() {
        User user = new User();
        user.setId(1L);
        user.setEmail("client@example.com");

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Ron");
        request.setLastName("Smith");
        request.setPhone("+34 612 345 677");

        when(clientRepository.getClientByUserId(user.id)).thenReturn(Optional.empty());

        // Act & Assert: Verificamos que se lanza la excepción esperada
        Exception exception = assertThrows(RuntimeException.class, () ->
                clientUseCase.updateClientProfile(user, request)
        );

        // Validamos el mensaje de la excepción
        assertEquals("Cliente no encontrado", exception.getMessage());
    }

    @Test
    public void deleteClient_success() {
        // Arrange: Creamos un cliente y simulamos el retorno del delete
        Client client = new Client();
        client.setUser_id(5L);

        // Stubs
        when(userRepository.deleteUser(client.user_id)).thenReturn(1);
        when(clientRepository.getClientById(5L)).thenReturn(Optional.of(client));

        // Act
        int result  = clientUseCase.deleteClient(5L);

        // Assert
        assertEquals(1, result);
        verify(userRepository).deleteUser(5L);
    }

    @Test
    public void deleteClient_failure_shouldThrowException() {
        // Arrange: Configuramos el comportamiento del repository para simular que no existe el administrador.
        when(clientRepository.getClientById(7L)).thenReturn(Optional.empty());

        // Act
        Exception exception = assertThrows(RuntimeException.class, () ->
                clientUseCase.deleteClient(7L)
        );

        // Assert
        assertEquals("No existe el cliente que quieres eliminar", exception.getMessage());
        verify(clientRepository).getClientById(7L);
    }
}
