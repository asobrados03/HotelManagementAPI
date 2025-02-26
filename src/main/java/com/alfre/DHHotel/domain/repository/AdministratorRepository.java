package com.alfre.DHHotel.domain.repository;

import com.alfre.DHHotel.domain.model.Administrator;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing administrator data.
 * Defines CRUD operations for administrators in the system.
 *
 * <p>This interface should be implemented by a class that interacts with the database.</p>
 *
 * @author Alfredo Sobrados González
 */
public interface AdministratorRepository {

    /**
     * Retrieves a list of all administrators in the system.
     *
     * @return a list of {@link Administrator} objects.
     */
    List<Administrator> getAllAdministrators();

    /**
     * Retrieves an administrator based on the provided user ID.
     *
     * @param userId the user ID associated with the administrator.
     * @return an {@code Optional} containing the administrator if found, otherwise empty.
     */
    Optional<Administrator> getAdministratorByUserId(String userId);

    /**
     * Retrieves an administrator based on their unique ID.
     *
     * @param id the unique identifier of the administrator.
     * @return an {@code Optional} containing the administrator if found, otherwise empty.
     */
    Optional<Administrator> getAdministratorById(long id);

    /**
     * Creates a new administrator in the system.
     *
     * @param newAdministrator the {@link Administrator} object to be added.
     */
    void createAdministrator(Administrator newAdministrator);

    /**
     * Updates the details of an existing administrator.
     *
     * @param administrator the administrator object containing updated information.
     * @param userId the user ID associated with the administrator to be updated.
     * @return the number of rows affected in the database.
     */
    int updateAdministrator(Administrator administrator, long userId);

    /**
     * Retrieves an administrator based on their user ID.
     *
     * <p>Note: This method is a duplicate of {@link #getAdministratorByUserId(String)}</p>
     *
     * @param userId the unique identifier of the user.
     * @return an {@code Optional} containing the administrator if found, otherwise empty.
     */
    Optional<Administrator> getAdministratorByUserId(long userId);

    /**
     * Deletes all administrator records from the system.
     * <p><b>Warning:</b> This action is irreversible.</p>
     */
    void deleteAll();
}
