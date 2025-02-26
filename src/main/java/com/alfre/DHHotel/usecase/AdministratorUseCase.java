package com.alfre.DHHotel.usecase;

import com.alfre.DHHotel.domain.model.Administrator;
import com.alfre.DHHotel.domain.repository.AdministratorRepository;
import com.alfre.DHHotel.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service class that handles business logic for administrator-related operations.
 * Delegates data access to the {@link AdministratorRepository} and {@link UserRepository}.
 *
 * <p>This class provides methods to manage administrators in the system, including retrieval,
 * updates, and deletion.</p>
 *
 * @author Alfredo Sobrados González
 */
@Service
public class AdministratorUseCase {
    private final AdministratorRepository administratorRepository;
    private final UserRepository userRepository;

    /**
     * Constructs an instance of {@code AdministratorUseCase} with the required dependencies.
     *
     * @param administratorRepository the repository for administrator data access.
     * @param userRepository the repository for user data access.
     */
    public AdministratorUseCase(AdministratorRepository administratorRepository, UserRepository userRepository) {
        this.administratorRepository = administratorRepository;
        this.userRepository = userRepository;
    }

    /**
     * Retrieves a list of all administrators in the system.
     *
     * @return a list of {@link Administrator} objects.
     */
    public List<Administrator> getAllAdministrators() {
        return administratorRepository.getAllAdministrators();
    }

    /**
     * Retrieves an administrator by their associated user ID.
     *
     * @param userId the unique identifier of the user linked to the administrator.
     * @return an {@code Optional} containing the administrator if found, otherwise empty.
     */
    public Optional<Administrator> getAdministratorByUserId(long userId) {
        return administratorRepository.getAdministratorByUserId(userId);
    }

    /**
     * Retrieves an administrator by their unique ID.
     *
     * @param id the unique identifier of the administrator.
     * @return an {@code Optional} containing the administrator if found, otherwise empty.
     */
    public Optional<Administrator> getAdministratorById(long id) {
        return administratorRepository.getAdministratorById(id);
    }

    /**
     * Updates an administrator's details.
     *
     * @param administrator the updated administrator details.
     * @param userId the unique identifier of the user linked to the administrator.
     * @return the number of rows affected in the database.
     */
    public int updateAdministrator(Administrator administrator, long userId) {
        return administratorRepository.updateAdministrator(administrator, userId);
    }

    /**
     * Deletes an administrator from the system.
     * <p>This method first verifies if the administrator exists before deleting the associated user record.</p>
     *
     * @param id the unique identifier of the administrator to be deleted.
     * @return the number of rows affected in the database.
     * @throws RuntimeException if the administrator does not exist.
     */
    public int deleteAdministrator(long id) {
        Administrator admin = administratorRepository.getAdministratorById(id)
                .orElseThrow(() -> new RuntimeException("No existe el administrador que quieres eliminar"));

        return userRepository.deleteUser(admin.user_id);
    }
}