package com.alfre.DHHotel.usecase;

import com.alfre.DHHotel.adapter.web.dto.UpdateProfileRequest;
import com.alfre.DHHotel.adapter.web.dto.ClientDTO;
import com.alfre.DHHotel.domain.model.Client;
import com.alfre.DHHotel.domain.model.User;
import com.alfre.DHHotel.domain.repository.ClientRepository;
import com.alfre.DHHotel.domain.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ClientUseCase {
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;


    public ClientUseCase(ClientRepository clientRepository, UserRepository userRepository) {
        this.clientRepository = clientRepository;
        this.userRepository = userRepository;
    }

    public List<Client> getAllClients() {
        return clientRepository.getAllClients();
    }

    public Optional<Client> getClientById(long id) {
        return clientRepository.getClientById(id);
    }

    @Transactional
    public ClientDTO updateClientProfile(User user, UpdateProfileRequest request) {
        // Validar campos requeridos
        if ((request.getFirstName() == null || request.getFirstName().trim().isEmpty()) ||
                (request.getLastName() == null || request.getLastName().trim().isEmpty())) {
            throw new IllegalArgumentException("El nombre y el apellido son obligatorios");
        }

        // Validar formato del teléfono
        if (request.getPhone() == null || !request.getPhone().matches(RegexConstants.PHONE_REGEX)) {
            throw new IllegalArgumentException("Formato de teléfono inválido u obligatorio");
        }

        // Obtener el cliente asociado al usuario
        Client client = clientRepository.getClientByUserId(user.id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        // Actualizar campos permitidos
        client.setFirst_name(request.getFirstName());
        client.setLast_name(request.getLastName());
        client.setPhone(request.getPhone());

        Client clientUpdated =  clientRepository.updateClient(client);

        return ClientDTO.builder()
                .email(user.email)
                .firstName(clientUpdated.first_name)
                .lastName(clientUpdated.last_name)
                .phone(clientUpdated.phone)
                .build();
    }

    public int deleteClient(long id) {
        Client client = clientRepository.getClientById(id)
                .orElseThrow(() -> new RuntimeException("No existe el cliente que quieres eliminar"));

        return userRepository.deleteUser(client.user_id);
    }
}