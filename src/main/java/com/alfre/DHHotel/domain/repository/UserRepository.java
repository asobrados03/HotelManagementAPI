package com.alfre.DHHotel.domain.repository;

import com.alfre.DHHotel.domain.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing user data.
 * Defines CRUD operations for users in the system.
 *
 * <p>This interface should be implemented by a class that interacts with the database.</p>
 *
 * @author Alfredo
 */
public interface UserRepository {

    /**
     * Retrieves a list of all users in the system.
     *
     * @return a list of {@link User} objects.
     */
    List<User> getAllUsers();

    /**
     * Retrieves a user based on their email.
     *
     * @param email the email of the user.
     * @return an {@code Optional} containing the user if found, otherwise empty.
     */
    Optional<User> getUserByEmail(String email);

    /**
     * Creates a new user in the system.
     *
     * @param newUser the {@link User} object to be added.
     * @return the generated unique identifier of the newly created user.
     */
    long createUser(User newUser);

    /**
     * Updates the details of an existing user.
     *
     * @param user the user object containing updated information.
     */
    void updateUser(User user);

    /**
     * Deletes a user from the system based on their unique ID.
     *
     * @param id the unique identifier of the user to be deleted.
     * @return the number of rows affected in the database.
     */
    int deleteUser(long id);

    /**
     * Deletes all user records from the system.
     * <p><b>Warning:</b> This action is irreversible.</p>
     */
    void deleteAll();

    /**
     * Retrieves a user based on their unique ID.
     *
     * @param id the unique identifier of the user.
     * @return an {@code Optional} containing the user if found, otherwise empty.
     */
    Optional<User> getUserById(long id);
}