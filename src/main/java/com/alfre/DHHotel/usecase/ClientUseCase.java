package com.alfre.DHHotel.usecase;

import com.alfre.DHHotel.adapter.web.dto.ClientDTO;
import com.alfre.DHHotel.adapter.web.dto.UpdateProfileRequest;
import com.alfre.DHHotel.domain.model.Client;
import com.alfre.DHHotel.domain.model.User;
import com.alfre.DHHotel.domain.repository.ClientRepository;
import com.alfre.DHHotel.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service class that handles business logic for client-related operations.
 * It interacts with the {@link ClientRepository} and {@link UserRepository}
 * to manage client data and user accounts.
 * <p>
 * This class provides methods for retrieving, updating, and deleting clients,
 * ensuring proper validation and transaction management.
 * </p>
 *
 * @author Alfredo Sobrados González
 */
@Service
@RequiredArgsConstructor
public class ClientUseCase {
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;

    /**
     * Retrieves a list of all clients.
     *
     * @return a {@link List} of {@link Client} objects.
     */
    public List<Client> getAllClients() {
        return clientRepository.getAllClients();
    }

    /**
     * Retrieves a client by its ID.
     *
     * @param id the ID of the client.
     * @return an {@link Optional} containing the {@link Client}, if found.
     */
    public Optional<Client> getClientById(long id) {
        return clientRepository.getClientById(id);
    }

    /**
     * Updates the profile information of a client.
     *
     * @param user    the authenticated user.
     * @param request the request containing updated profile details.
     * @return a {@link ClientDTO} with the updated client information.
     * @throws IllegalArgumentException if required fields are missing or have invalid values.
     * @throws RuntimeException if the client is not found.
     */
    @Transactional
    public ClientDTO updateClientProfile(User user, UpdateProfileRequest request) {
        validateProfileUpdateRequest(request);

        // Fetch the client associated with the user
        Client client = clientRepository.getClientByUserId(user.id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        // Update the client's details
        client.setFirst_name(request.getFirstName());
        client.setLast_name(request.getLastName());
        client.setPhone(request.getPhone());

        Client updatedClient = clientRepository.updateClient(client);

        return ClientDTO.builder()
                .email(user.email)
                .firstName(updatedClient.first_name)
                .lastName(updatedClient.last_name)
                .phone(updatedClient.phone)
                .build();
    }

    /**
     * Deletes a client and its associated user account.
     *
     * @param id the ID of the client to delete.
     * @return the number of affected rows in the database.
     * @throws RuntimeException if the client does not exist.
     */
    public int deleteClient(long id) {
        Client client = clientRepository.getClientById(id)
                .orElseThrow(() -> new RuntimeException("No existe el cliente que quieres eliminar"));

        return userRepository.deleteUser(client.user_id);
    }

    /**
     * Validates the profile update request.
     *
     * @param request the request containing profile update details.
     * @throws IllegalArgumentException if required fields are missing or have invalid values.
     */
    private void validateProfileUpdateRequest(UpdateProfileRequest request) {
        if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()
                || request.getLastName() == null || request.getLastName().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre y el apellido son obligatorios");
        }

        if (request.getPhone() == null || !request.getPhone().matches(RegexConstants.PHONE_REGEX)) {
            throw new IllegalArgumentException("Formato de teléfono inválido u obligatorio");
        }
    }
}