package com.alfre.DHHotel.domain.repository;

import com.alfre.DHHotel.domain.model.Client;

import java.util.List;
import java.util.Optional;

public interface ClientRepository {
    List<Client> getAllClients();
    Optional<Client> getClientById(long id);
    Optional<Client> getClientByUserId(long userId);
    long createClient(Client newClient);
    Client updateClient(Client client);
    void deleteAll();
}
