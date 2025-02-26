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

/**
 * This class contains the attributes and methods for realize the unit tests
 * of the customers operations business logic.
 *
 * @author Alfredo Sobrados González
 */
@ExtendWith(MockitoExtension.class)
public class ClientUseCaseTest {
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private UserRepository userRepository;

    private ClientUseCase clientUseCase;

    /**
     * Tests for the ClientUseCase functionality.
     * <p>
     * This test class verifies the behavior of methods in the ClientUseCase class by stubbing the underlying
     * repository calls.
     * </p>
     */
    @BeforeEach
    public void setup() {
        // Instantiate the ClientUseCase with the required repositories.
        clientUseCase = new ClientUseCase(clientRepository, userRepository);
    }

    /**
     * Tests that getAllClients() returns the expected list of clients.
     * <p>
     * The test arranges a list containing one client, stubs the repository to return that list,
     * then asserts that the result from getAllClients() matches the expected list.
     * </p>
     */
    @Test
    public void getAllClients_success() {
        // Arrange: Prepare a list of clients.
        List<Client> clientList = new ArrayList<>();
        Client client = new Client();
        // Optionally configure properties of the client.
        clientList.add(client);
        when(clientRepository.getAllClients()).thenReturn(clientList);

        // Act: Execute getAllClients().
        List<Client> result = clientUseCase.getAllClients();

        // Assert: Verify that the result matches the expected list.
        assertEquals(clientList, result);
        verify(clientRepository).getAllClients();
    }

    /**
     * Tests that getClientById(long) returns the expected client when found.
     * <p>
     * The test stubs the client repository to return an Optional containing a client for a given id,
     * then asserts that the client is correctly returned.
     * </p>
     */
    @Test
    public void getAdministratorById_success() {
        // Arrange: Create a client and stub its retrieval for the specified id.
        Client client = new Client();
        when(clientRepository.getClientById(10L)).thenReturn(Optional.of(client));

        // Act: Execute getClientById().
        Optional<Client> result = clientUseCase.getClientById(10L);

        // Assert: Verify that the client is present and equals the expected client.
        assertTrue(result.isPresent());
        assertEquals(client, result.get());
        verify(clientRepository).getClientById(10L);
    }

    /**
     * Tests that updateClientProfile() successfully updates a client's profile.
     * <p>
     * The test arranges a User and an UpdateProfileRequest, stubs the repository to return an updated client,
     * then asserts that the ClientDTO returned from updateClientProfile() matches the expected values.
     * </p>
     */
    @Test
    public void updateClientProfile_success() {
        // Arrange: Create a user and an update profile request.
        User user = new User();
        user.setId(1L);
        user.setEmail("client@example.com");

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Ron");
        request.setLastName("Smith");
        request.setPhone("+34 612 345 677");

        Client client = new Client();
        // Stub the repository call to retrieve the client's record.
        when(clientRepository.getClientByUserId(user.id)).thenReturn(Optional.of(client));
        // Stub the repository update call to return the updated client.
        Client clientUpdated = new Client();
        when(clientRepository.updateClient(client)).thenReturn(clientUpdated);

        // Act: Execute updateClientProfile().
        ClientDTO response = clientUseCase.updateClientProfile(user, request);

        // Assert: Construct the expected ClientDTO and verify that it matches the response.
        ClientDTO responseExpected = ClientDTO.builder()
                .email(user.email)
                .firstName(clientUpdated.first_name)
                .lastName(clientUpdated.last_name)
                .phone(clientUpdated.phone)
                .build();

        assertNotNull(response);
        assertEquals(response, responseExpected);
    }

    /**
     * Tests that updateClientProfile() throws an IllegalArgumentException when the last name is null.
     * <p>
     * The test arranges a valid first name but a null last name in the update profile request,
     * then asserts that the appropriate exception is thrown.
     * </p>
     */
    @Test
    public void updateClientProfile_nullFirstAndLastName_shouldThrowException() {
        // Arrange: Create a user and an update profile request with null last name.
        User user = new User();
        user.setId(1L);
        user.setEmail("client@example.com");

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Ron");
        request.setLastName(null);
        request.setPhone("+34 612 345 677");

        // Act & Assert: Expect IllegalArgumentException with the specified message.
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                clientUseCase.updateClientProfile(user, request)
        );
        assertEquals("El nombre y el apellido son obligatorios", exception.getMessage());
    }

    /**
     * Tests that updateClientProfile() throws an IllegalArgumentException when the phone field is null.
     * <p>
     * The test arranges a valid first and last name but a null phone in the update request,
     * then asserts that the proper exception is thrown.
     * </p>
     */
    @Test
    public void updateClientProfile_nullPhone_shouldThrowException() {
        // Arrange: Create a user and an update profile request with null phone.
        User user = new User();
        user.setId(1L);
        user.setEmail("client@example.com");

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Ron");
        request.setLastName("Smith");
        request.setPhone(null);

        // Act & Assert: Verify that an exception is thrown with the correct message.
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                clientUseCase.updateClientProfile(user, request)
        );
        assertEquals("Formato de teléfono inválido u obligatorio", exception.getMessage());
    }

    /**
     * Tests that updateClientProfile() throws an IllegalArgumentException when the phone number format is invalid.
     * <p>
     * The test arranges an update request with an incorrectly formatted phone number,
     * and asserts that the expected exception is thrown.
     * </p>
     */
    @Test
    public void updateClientProfile_invalidFormatPhone_shouldThrowException() {
        // Arrange: Create a user and an update profile request with an invalid phone format.
        User user = new User();
        user.setId(1L);
        user.setEmail("client@example.com");

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Ron");
        request.setLastName("Smith");
        request.setPhone("444-3345");

        // Act & Assert: Expect IllegalArgumentException with the proper message.
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                clientUseCase.updateClientProfile(user, request)
        );
        assertEquals("Formato de teléfono inválido u obligatorio", exception.getMessage());
    }

    /**
     * Tests that updateClientProfile() throws a RuntimeException when no client is found for the user.
     * <p>
     * The test stubs the repository to return an empty Optional and asserts that a RuntimeException is thrown.
     * </p>
     */
    @Test
    public void updateClientProfile_userRelatedClientNotFound_shouldThrowException() {
        // Arrange: Create a user and a valid update profile request.
        User user = new User();
        user.setId(1L);
        user.setEmail("client@example.com");

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Ron");
        request.setLastName("Smith");
        request.setPhone("+34 612 345 677");

        when(clientRepository.getClientByUserId(user.id)).thenReturn(Optional.empty());

        // Act & Assert: Verify that a RuntimeException is thrown with the expected message.
        Exception exception = assertThrows(RuntimeException.class, () ->
                clientUseCase.updateClientProfile(user, request)
        );
        assertEquals("Cliente no encontrado", exception.getMessage());
    }

    /**
     * Tests that deleteClient() successfully deletes a client and returns 1.
     * <p>
     * The test stubs the userRepository.deleteUser() to return 1 and the clientRepository.getClientById() to return
     * the client.
     * It then asserts that deleteClient() returns 1.
     * </p>
     */
    @Test
    public void deleteClient_success() {
        // Arrange: Prepare a client with a specific user_id.
        Client client = new Client();
        client.setUser_id(5L);

        // Stub the delete method.
        when(userRepository.deleteUser(client.user_id)).thenReturn(1);
        when(clientRepository.getClientById(5L)).thenReturn(Optional.of(client));

        // Act: Execute deleteClient().
        int result  = clientUseCase.deleteClient(5L);

        // Assert: Verify the result and that the deleteUser method was called.
        assertEquals(1, result);
        verify(userRepository).deleteUser(5L);
    }

    /**
     * Tests that deleteClient() throws a RuntimeException when the client to delete is not found.
     * <p>
     * The test stubs the clientRepository.getClientById() to return an empty Optional and asserts that a
     * RuntimeException with the expected message is thrown.
     * </p>
     */
    @Test
    public void deleteClient_failure_shouldThrowException() {
        // Arrange: Simulate a scenario where the client is not found.
        when(clientRepository.getClientById(7L)).thenReturn(Optional.empty());

        // Act & Assert: Verify that a RuntimeException is thrown with the proper error message.
        Exception exception = assertThrows(RuntimeException.class, () ->
                clientUseCase.deleteClient(7L)
        );
        assertEquals("No existe el cliente que quieres eliminar", exception.getMessage());
        verify(clientRepository).getClientById(7L);
    }
}
