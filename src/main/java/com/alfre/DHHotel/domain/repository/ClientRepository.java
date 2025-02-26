package com.alfre.DHHotel.domain.repository;

import com.alfre.DHHotel.domain.model.Client;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing client data.
 * Defines CRUD operations for clients in the system.
 *
 * <p>This interface should be implemented by a class that interacts with the database.</p>
 *
 * @author Alfredo
 */
public interface ClientRepository {

    /**
     * Retrieves a list of all clients in the system.
     *
     * @return a list of {@link Client} objects.
     */
    List<Client> getAllClients();

    /**
     * Retrieves a client based on their unique ID.
     *
     * @param id the unique identifier of the client.
     * @return an {@code Optional} containing the client if found, otherwise empty.
     */
    Optional<Client> getClientById(long id);

    /**
     * Retrieves a client based on their associated user ID.
     *
     * @param userId the unique identifier of the user associated with the client.
     * @return an {@code Optional} containing the client if found, otherwise empty.
     */
    Optional<Client> getClientByUserId(long userId);

    /**
     * Creates a new client in the system.
     *
     * @param newClient the {@link Client} object to be added.
     * @return the generated unique identifier of the newly created client.
     */
    long createClient(Client newClient);

    /**
     * Updates the details of an existing client.
     *
     * @param client the client object containing updated information.
     * @return the updated {@link Client} object.
     */
    Client updateClient(Client client);

    /**
     * Deletes all client records from the system.
     * <p><b>Warning:</b> This action is irreversible.</p>
     */
    void deleteAll();
}
