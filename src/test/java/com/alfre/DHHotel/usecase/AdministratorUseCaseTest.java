package com.alfre.DHHotel.usecase;

import com.alfre.DHHotel.domain.model.Administrator;
import com.alfre.DHHotel.domain.repository.AdministratorRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * This class contains the attributes and methods for realize the unit tests
 * of the system administrators business logic.
 *
 * @author Alfredo Sobrados González
 */
@ExtendWith(MockitoExtension.class)
public class AdministratorUseCaseTest {
    @Mock
    private AdministratorRepository administratorRepository;
    @Mock
    private UserRepository userRepository;

    private AdministratorUseCase administratorUseCase;

    /**
     * Sets up the AdministratorUseCase instance before each test.
     * <p>
     * This method instantiates the AdministratorUseCase with its required dependencies,
     * ensuring that each test starts with a fresh use case instance.
     * </p>
     */
    @BeforeEach
    public void setup() {
        administratorUseCase = new AdministratorUseCase(administratorRepository, userRepository);
    }

    /**
     * Tests that {@link AdministratorUseCase#getAllAdministrators()} returns the expected list of administrators.
     * <p>
     * The test arranges a list of administrators, stubs the repository to return that list,
     * invokes the use case method, and verifies that the returned list matches the expected one.
     * </p>
     */
    @Test
    public void getAllAdministrators_success() {
        // Arrange: Prepare a list of administrators
        List<Administrator> adminList = new ArrayList<>();
        Administrator admin = new Administrator();
        // Configure admin properties if needed
        adminList.add(admin);
        when(administratorRepository.getAllAdministrators()).thenReturn(adminList);

        // Act: Execute the method
        List<Administrator> result = administratorUseCase.getAllAdministrators();

        // Assert: Verify that the result is as expected and the repository method was called
        assertEquals(adminList, result);
        verify(administratorRepository).getAllAdministrators();
    }

    /**
     * Tests that {@link AdministratorUseCase#getAdministratorByUserId(long)} returns the correct administrator.
     * <p>
     * The test stubs the repository to return an Optional containing an administrator for a given user ID,
     * then verifies that the use case method returns the expected administrator.
     * </p>
     */
    @Test
    public void getAdministratorByUserId_success() {
        // Arrange: Create an administrator and simulate its return by the repository
        Administrator admin = new Administrator();
        // Configure admin properties if needed
        when(administratorRepository.getAdministratorByUserId(1L)).thenReturn(Optional.of(admin));

        // Act: Execute the method
        Optional<Administrator> result = administratorUseCase.getAdministratorByUserId(1L);

        // Assert: Verify that the result is present and equals the expected administrator
        assertTrue(result.isPresent());
        assertEquals(admin, result.get());
        verify(administratorRepository).getAdministratorByUserId(1L);
    }

    /**
     * Tests that {@link AdministratorUseCase#getAdministratorById(long)} returns the correct administrator.
     * <p>
     * The test simulates the repository returning an administrator for a given ID and verifies that
     * the use case method returns an Optional containing that administrator.
     * </p>
     */
    @Test
    public void getAdministratorById_success() {
        // Arrange: Create an administrator and simulate its return for the given id
        Administrator admin = new Administrator();
        when(administratorRepository.getAdministratorById(10L)).thenReturn(Optional.of(admin));

        // Act: Execute the method
        Optional<Administrator> result = administratorUseCase.getAdministratorById(10L);

        // Assert: Verify that the result is present and equals the expected administrator
        assertTrue(result.isPresent());
        assertEquals(admin, result.get());
        verify(administratorRepository).getAdministratorById(10L);
    }

    /**
     * Tests that {@link AdministratorUseCase#updateAdministrator(Administrator, long)} successfully updates an administrator.
     * <p>
     * The test simulates the repository returning 1 (indicating one record updated) when updating the administrator,
     * then verifies that the use case method returns 1.
     * </p>
     */
    @Test
    public void updateAdministrator_success() {
        // Arrange: Create an administrator and simulate the update return value
        Administrator admin = new Administrator();
        // Configure admin properties if needed
        when(administratorRepository.updateAdministrator(admin, 1L)).thenReturn(1);

        // Act: Execute the update method
        int result = administratorUseCase.updateAdministrator(admin, 1L);

        // Assert: Verify that the result is 1 and the repository update method was called with the correct parameters
        assertEquals(1, result);
        verify(administratorRepository).updateAdministrator(admin, 1L);
    }

    /**
     * Tests that {@link AdministratorUseCase#deleteAdministrator(long)} successfully deletes an administrator.
     * <p>
     * The test simulates the repository returning an administrator for a given user ID,
     * stubs the user repository to return 1 when deleting the user, and verifies that the use case returns 1.
     * </p>
     */
    @Test
    public void deleteAdministrator_success() {
        // Arrange: Create an administrator and simulate the repository behavior for deletion
        Administrator admin = new Administrator();
        admin.setUser_id(5L);

        // Stubs: Simulate successful deletion of the user and retrieval of the administrator
        when(userRepository.deleteUser(admin.user_id)).thenReturn(1);
        when(administratorRepository.getAdministratorById(5L)).thenReturn(Optional.of(admin));

        // Act: Execute the delete method
        int result = administratorUseCase.deleteAdministrator(5L);

        // Assert: Verify that the result is 1 and that the user deletion method was invoked
        assertEquals(1, result);
        verify(userRepository).deleteUser(5L);
    }

    /**
     * Tests that {@link AdministratorUseCase#deleteAdministrator(long)} throws an exception when the administrator does not exist.
     * <p>
     * The test simulates the repository returning an empty Optional for a non-existent administrator ID,
     * and verifies that the use case method throws a RuntimeException with the expected message.
     * </p>
     */
    @Test
    public void deleteAdministrator_failure_shouldThrowException() {
        // Arrange: Configure the repository to simulate that the administrator does not exist
        when(administratorRepository.getAdministratorById(7L)).thenReturn(Optional.empty());

        // Act: Attempt to delete the administrator and capture the thrown exception
        Exception exception = assertThrows(RuntimeException.class, () ->
                administratorUseCase.deleteAdministrator(7L)
        );

        // Assert: Verify that the exception message is as expected and that the repository method was called
        assertEquals("No existe el administrador que quieres eliminar", exception.getMessage());
        verify(administratorRepository).getAdministratorById(7L);
    }
}