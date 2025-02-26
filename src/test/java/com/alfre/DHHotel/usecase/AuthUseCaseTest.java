package com.alfre.DHHotel.usecase;

import com.alfre.DHHotel.adapter.security.jwt.JwtService;
import com.alfre.DHHotel.adapter.web.dto.*;
import com.alfre.DHHotel.domain.model.Administrator;
import com.alfre.DHHotel.domain.model.Client;
import com.alfre.DHHotel.domain.model.Role;
import com.alfre.DHHotel.domain.model.User;
import com.alfre.DHHotel.domain.repository.AdministratorRepository;
import com.alfre.DHHotel.domain.repository.ClientRepository;
import com.alfre.DHHotel.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * This class contains the attributes and methods for realize the unit tests
 * of the authentication and authorization operations business logic.
 *
 * @author Alfredo Sobrados González
 */
@ExtendWith(MockitoExtension.class)
public class AuthUseCaseTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private AdministratorRepository adminRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    private AuthUseCase authUseCase;

    /**
     * Configures the AuthUseCase instance with all its dependencies before each test.
     */
    @BeforeEach
    public void setup() {
        // Inject mocks into the AuthUseCase instance.
        authUseCase = new AuthUseCase(userRepository, clientRepository, adminRepository, jwtService, passwordEncoder,
                authenticationManager);
    }

    /**
     * Tests the login functionality when provided with valid credentials.
     * <p>
     * The test simulates a successful authentication by stubbing the authentication manager to return a dummy
     * Authentication object.
     * It also stubs the user repository and JWT service to return a dummy user and token respectively.
     * Finally, it asserts that the returned AuthResponse contains the expected token.
     * </p>
     */
    @Test
    public void loginTest_success() {
        // Arrange: Prepare input data and mock behaviors.
        LoginRequest request = new LoginRequest();
        request.setUsername("user@example.com");
        request.setPassword("password");

        // Simulate successful authentication by returning a dummy Authentication object.
        Authentication dummyAuth = new UsernamePasswordAuthenticationToken("user@example.com",
                "password");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(dummyAuth);

        // Create a dummy user that will be returned when querying by email.
        User dummyUser = User.builder()
                .id(1L)
                .email("user@example.com")
                .password("encodedPassword")
                .role(Role.CLIENT)
                .build();
        when(userRepository.getUserByEmail("user@example.com")).thenReturn(Optional.of(dummyUser));

        // Stub the JWT service to return a token.
        when(jwtService.getToken(dummyUser)).thenReturn("dummyToken");

        // Act: Execute the login method.
        AuthResponse response = authUseCase.login(request);

        // Assert: Verify that the response is not null and contains the expected token.
        assertNotNull(response);
        assertEquals("dummyToken", response.getToken());

        // Verify that the relevant methods were called.
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).getUserByEmail("user@example.com");
        verify(jwtService).getToken(dummyUser);
    }

    /**
     * Tests the login functionality when an incorrect password is provided.
     * <p>
     * The authentication manager is stubbed to throw a RuntimeException to simulate authentication failure.
     * The test asserts that the exception message matches the expected error.
     * </p>
     */
    @Test
    public void loginTest_failure_becauseWrongPassword() {
        // Arrange: Create a LoginRequest with a wrong password.
        LoginRequest request = new LoginRequest();
        request.setUsername("user@example.com");
        request.setPassword("wrongPassword");

        // Simulate authentication failure.
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new RuntimeException("Authentication failed"));

        // Act & Assert: Verify that the expected exception is thrown.
        Exception exception = assertThrows(RuntimeException.class, () ->
                authUseCase.login(request)
        );
        assertEquals("Authentication failed", exception.getMessage());
    }

    /**
     * Tests the login functionality when an incorrect username is provided.
     * <p>
     * The authentication manager is stubbed to throw an exception to simulate failure, and the test verifies the
     * exception message.
     * </p>
     */
    @Test
    public void loginTest_failure_becauseWrongUsername() {
        // Arrange: Create a LoginRequest with a wrong username.
        LoginRequest request = new LoginRequest();
        request.setUsername("wronguser@example.com");
        request.setPassword("securePassword");

        // Simulate authentication failure.
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new RuntimeException("Authentication failed"));

        // Act & Assert: Verify that the expected exception is thrown.
        Exception exception = assertThrows(RuntimeException.class, () ->
                authUseCase.login(request)
        );
        assertEquals("Authentication failed", exception.getMessage());
    }

    /**
     * Tests that login fails when a required field (username) is null.
     * <p>
     * The test expects an IllegalArgumentException to be thrown with a specific message.
     * </p>
     */
    @Test
    void login_nullField_shouldThrowException() {
        // Arrange: Create a LoginRequest with a null username.
        LoginRequest request = new LoginRequest();
        request.setUsername(null);
        request.setPassword("securePassword");

        // Act & Assert: Verify that the expected exception is thrown.
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                authUseCase.login(request)
        );
        assertEquals("Los campos obligatorios no pueden ser nulos", exception.getMessage());
    }

    /**
     * Tests that registering a client with valid data returns a token.
     * <p>
     * The test stubs the user repository to simulate user creation and the JWT service to return a dummy token.
     * It then asserts that the AuthResponse contains the expected token and that the repositories were invoked with
     * correct parameters.
     * </p>
     */
    @Test
    public void registerClient_success() {
        // Arrange: Prepare a valid client registration request.
        RegisterRequest request = new RegisterRequest();
        request.setUsername("client@example.com");
        request.setPassword("encodedPassword");
        request.setFirst_name("Bob");
        request.setLast_name("Smith");
        request.setRole(Role.CLIENT);
        request.setPhone("+34 618 34 54 56");

        // Stub user creation.
        when(userRepository.createUser(any(User.class))).thenReturn(1L);
        // Stub JWT token generation.
        when(jwtService.getToken(any(User.class))).thenReturn("dummyToken");

        // Act: Execute client registration.
        AuthResponse response = authUseCase.registerClient(request);

        // Assert: Validate the response and verify that the mocks were invoked with the expected values.
        assertNotNull(response);
        assertEquals("dummyToken", response.getToken());
        verify(userRepository).createUser(argThat(user ->
                "client@example.com".equals(user.email) &&
                        Role.CLIENT.equals(user.role)
        ));
        verify(clientRepository).createClient(argThat(client ->
                "Bob".equals(client.first_name) &&
                        "Smith".equals(client.last_name) &&
                        "+34 618 34 54 56".equals(client.phone)
        ));
        verify(jwtService).getToken(any(User.class));
    }

    /**
     * Tests that registering a client with an incorrectly formatted phone number throws an IllegalArgumentException.
     */
    @Test
    public void registerClient_withWrongFormatPhone() {
        // Arrange: Prepare a RegisterRequest with an invalid phone format.
        RegisterRequest request = new RegisterRequest();
        request.setUsername("client@example.com");
        request.setPassword("encodedPassword");
        request.setFirst_name("Bob");
        request.setLast_name("Smith");
        request.setRole(Role.CLIENT);
        request.setPhone("414-3445");

        // Act & Assert: Expect an exception with the correct message.
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                authUseCase.registerClient(request)
        );
        assertEquals("Formato de teléfono inválido", exception.getMessage());
    }

    /**
     * Tests that registering a client with an incorrectly formatted email throws an IllegalArgumentException.
     */
    @Test
    public void registerClient_withWrongFormatEmail() {
        // Arrange: Prepare a RegisterRequest with an invalid email format.
        RegisterRequest request = new RegisterRequest();
        request.setUsername("client_example");
        request.setPassword("encodedPassword");
        request.setFirst_name("Bob");
        request.setLast_name("Smith");
        request.setRole(Role.CLIENT);
        request.setPhone("+34 618 34 54 56");

        // Act & Assert: Expect an exception with the correct error message.
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                authUseCase.registerClient(request)
        );
        assertEquals("Formato de correo electrónico inválido", exception.getMessage());
    }

    /**
     * Tests that registering a client with an invalid role (ADMIN instead of CLIENT) throws an
     * IllegalArgumentException.
     */
    @Test
    public void registerClient_withWrongRole() {
        // Arrange: Prepare a RegisterRequest with an invalid role.
        RegisterRequest request = new RegisterRequest();
        request.setUsername("client@example.com");
        request.setPassword("encodedPassword");
        request.setFirst_name("Bob");
        request.setLast_name("Smith");
        request.setRole(Role.ADMIN);
        request.setPhone("+34 618 34 54 56");

        // Act & Assert: Expect an exception with the message "Role inválido".
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                authUseCase.registerClient(request)
        );
        assertEquals("Role inválido", exception.getMessage());
    }

    /**
     * Tests that registering a client with null mandatory fields throws an IllegalArgumentException.
     */
    @Test
    public void registerClient_nullField_shouldThrowException() {
        // Arrange: Prepare a RegisterRequest with null email and phone.
        RegisterRequest request = new RegisterRequest();
        request.setUsername(null);
        request.setPassword("securePassword");
        request.setFirst_name("Alice");
        request.setLast_name("Doe");
        request.setRole(Role.CLIENT);
        request.setPhone(null);

        // Act & Assert: Expect an exception with the correct error message.
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                authUseCase.registerClient(request)
        );
        assertEquals("Los campos obligatorios no pueden ser nulos", exception.getMessage());
    }

    /**
     * Tests that registering an administrator with valid data returns a token.
     * <p>
     * The test simulates user creation and administrator creation, and stubs the JWT service to return a dummy token.
     * It verifies that the response contains the expected token and that the mocks are invoked correctly.
     * </p>
     */
    @Test
    public void registerAdministrator_success() {
        // Arrange: Prepare a valid admin registration request.
        RegisterRequest request = new RegisterRequest();
        request.setUsername("admin@example.com");
        request.setPassword("encodedPassword");
        request.setName("Jack Johnson");
        request.setRole(Role.ADMIN);

        // Stub user creation.
        when(userRepository.createUser(any(User.class))).thenReturn(1L);
        // Stub JWT token generation.
        when(jwtService.getToken(any(User.class))).thenReturn("dummyToken");

        // Act: Execute the admin registration method.
        AuthResponse response = authUseCase.registerAdministrator(request);

        // Assert: Validate the response and verify that the repository and JWT service were called with correct
        // arguments.
        assertNotNull(response);
        assertEquals("dummyToken", response.getToken());
        verify(userRepository).createUser(argThat(user ->
                "admin@example.com".equals(user.email) &&
                        Role.ADMIN.equals(user.role)
        ));
        verify(adminRepository).createAdministrator(argThat(admin ->
                "Jack Johnson".equals(admin.name)
        ));
        verify(jwtService).getToken(any(User.class));
    }

    /**
     * Tests that registering an administrator with null mandatory fields throws an IllegalArgumentException.
     */
    @Test
    public void registerAdministrator_nullField_shouldThrowException() {
        // Arrange: Prepare a RegisterRequest with a null username.
        RegisterRequest request = new RegisterRequest();
        request.setUsername(null);
        request.setPassword("encodedPassword");
        request.setName("Jack Johnson");
        request.setRole(Role.ADMIN);

        // Act & Assert: Expect an exception with the correct error message.
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                authUseCase.registerAdministrator(request)
        );
        assertEquals("Los campos obligatorios no pueden ser nulos", exception.getMessage());
    }

    /**
     * Tests that registering an administrator with an invalid role (CLIENT instead of ADMIN) throws an IllegalArgumentException.
     */
    @Test
    public void registerAdministrator_withWrongRole() {
        // Arrange: Prepare a RegisterRequest with an invalid role.
        RegisterRequest request = new RegisterRequest();
        request.setUsername("admin@example.com");
        request.setPassword("encodedPassword");
        request.setName("Jack Johnson");
        request.setRole(Role.CLIENT);

        // Act & Assert: Expect an exception with the message "Role inválido".
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                authUseCase.registerAdministrator(request)
        );
        assertEquals("Role inválido", exception.getMessage());
    }

    /**
     * Tests that registering an administrator with an invalid email format throws an IllegalArgumentException.
     */
    @Test
    public void registerAdministrator_withWrongFormatEmail() {
        // Arrange: Prepare a RegisterRequest with an invalid email.
        RegisterRequest request = new RegisterRequest();
        request.setUsername("admin_example");
        request.setPassword("encodedPassword");
        request.setName("Jack Johnson");
        request.setRole(Role.ADMIN);

        // Act & Assert: Expect an exception with the appropriate error message.
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                authUseCase.registerAdministrator(request)
        );
        assertEquals("Formato de correo electrónico inválido", exception.getMessage());
    }

    /**
     * Tests that retrieving client information for a valid client user returns the correct ClientDTO.
     * <p>
     * The test stubs the client repository to return a client and then asserts that the ClientDTO returned by getInfoClient
     * matches the expected values.
     * </p>
     */
    @Test
    public void getInfoClient_success() {
        // Arrange: Prepare a client user and a corresponding client record.
        User user = new User();
        user.setId(1L);
        user.setEmail("client@example.com");
        user.setPassword("encodedPassword");
        user.setRole(Role.CLIENT);

        Client client = new Client();
        client.setId(1L);
        client.setUser_id(1L);
        client.setFirst_name("Bob");
        client.setLast_name("Cooper");
        client.setPhone("+34 645 86 58 67");

        // Stub the client repository to return the client.
        when(clientRepository.getClientByUserId(user.id)).thenReturn(Optional.of(client));

        // Act: Execute getInfoClient.
        ClientDTO response = authUseCase.getInfoClient(user);

        // Assert: Validate that the returned ClientDTO matches the expected values.
        ClientDTO responseExpected = ClientDTO.builder()
                .firstName(client.first_name)
                .lastName(client.last_name)
                .email(user.email)
                .phone(client.phone)
                .build();
        assertNotNull(response);
        assertEquals(response, responseExpected);

        verify(clientRepository).getClientByUserId(1L);
    }

    /**
     * Tests that retrieving client information fails when the client is not found,
     * causing a RuntimeException.
     */
    @Test
    public void getInfoClient_failure() {
        // Arrange: Prepare a client user.
        User user = new User();
        user.setId(4L);
        user.setEmail("client@example.com");
        user.setPassword("encodedPassword");
        user.setRole(Role.CLIENT);

        // Stub the client repository to throw an exception.
        when(clientRepository.getClientByUserId(user.id)).thenThrow(new RuntimeException("Cliente no encontrado"));

        // Act & Assert: Expect a RuntimeException with the message "Cliente no encontrado".
        Exception exception = assertThrows(RuntimeException.class, () ->
                authUseCase.getInfoClient(user)
        );
        assertEquals("Cliente no encontrado", exception.getMessage());

        verify(clientRepository).getClientByUserId(4L);
    }

    /**
     * Tests that retrieving administrator information for a valid admin user returns the correct AdminDTO.
     * <p>
     * The test stubs the admin repository to return an administrator and verifies that getInfoAdmin returns an AdminDTO
     * with the expected values.
     * </p>
     */
    @Test
    public void getInfoAdmin_success() {
        // Arrange: Prepare an admin user and a corresponding administrator record.
        User user = new User();
        user.setId(1L);
        user.setEmail("admin@example.com");
        user.setPassword("encodedPassword");
        user.setRole(Role.ADMIN);

        Administrator administrator = new Administrator();
        administrator.setId(1L);
        administrator.setUser_id(1L);
        administrator.setName("Jennifer Collins");

        // Stub the admin repository.
        when(adminRepository.getAdministratorByUserId(user.id)).thenReturn(Optional.of(administrator));

        // Act: Execute getInfoAdmin.
        AdminDTO response = authUseCase.getInfoAdmin(user);

        // Assert: Validate that the returned AdminDTO matches the expected values.
        AdminDTO responseExpected = AdminDTO.builder()
                .email(user.email)
                .role(user.role)
                .name(administrator.name)
                .build();
        assertNotNull(response);
        assertEquals(response, responseExpected);

        verify(adminRepository).getAdministratorByUserId(1L);
    }

    /**
     * Tests that retrieving administrator information fails when the administrator is not found,
     * and a RuntimeException is thrown.
     */
    @Test
    public void getInfoAdmin_failure() {
        // Arrange: Prepare an admin user.
        User user = new User();
        user.setId(4L);
        user.setEmail("admin@example.com");
        user.setPassword("encodedPassword");
        user.setRole(Role.ADMIN);

        // Stub the admin repository to throw an exception.
        when(adminRepository.getAdministratorByUserId(user.id)).thenThrow(new RuntimeException("Admin no encontrado"));

        // Act & Assert: Expect a RuntimeException with the message "Admin no encontrado".
        Exception exception = assertThrows(RuntimeException.class, () ->
                authUseCase.getInfoAdmin(user)
        );
        assertEquals("Admin no encontrado", exception.getMessage());

        verify(adminRepository).getAdministratorByUserId(4L);
    }

    /**
     * Tests that changing the password with valid data returns a success message.
     * <p>
     * The test stubs the password encoder and user repository to simulate a successful password change,
     * and verifies that the response message indicates success.
     * </p>
     */
    @Test
    public void changePassword_success() {
        // Arrange: Prepare a user and new password.
        User user = new User();
        user.setEmail("client@example.com");
        user.setPassword("oldEncodedPassword");

        String newPassword = "newPassword";
        String newEncodedPassword = "newEncodedPassword";

        // Stub the password encoder.
        when(passwordEncoder.encode(newPassword)).thenReturn(newEncodedPassword);
        when(passwordEncoder.matches(newPassword, newEncodedPassword)).thenReturn(true);

        // Stub the user repository to return the updated user.
        User updatedUser = new User();
        updatedUser.setEmail("client@example.com");
        updatedUser.setPassword(newEncodedPassword);
        when(userRepository.getUserByEmail("client@example.com")).thenReturn(Optional.of(updatedUser));

        // Act: Execute changePassword.
        String result = authUseCase.changePassword(user, newPassword);

        // Assert: Verify the success message.
        assertNotNull(result);
        assertEquals("Cambio de contraseña CORRECTO.", result);

        verify(userRepository).updateUser(user);
    }

    /**
     * Tests that changing the password fails when the user is not found.
     * <p>
     * The test stubs the user repository to throw an exception when retrieving the user,
     * and verifies that the appropriate exception is thrown.
     * </p>
     */
    @Test
    public void changePassword_failureUserNotFound() {
        // Arrange: Prepare a user.
        User user = new User();
        user.setEmail("client@example.com");
        user.setPassword("oldEncodedPassword");

        String newPassword = "newPassword";

        // Stub the user repository to throw an exception.
        when(userRepository.getUserByEmail("client@example.com"))
                .thenThrow(new RuntimeException("Usuario no encontrado"));

        // Act & Assert: Expect a RuntimeException with the correct message.
        Exception exception = assertThrows(RuntimeException.class, () ->
                authUseCase.changePassword(user, newPassword)
        );
        assertEquals("Usuario no encontrado", exception.getMessage());

        verify(userRepository).getUserByEmail("client@example.com");
    }

    /**
     * Tests that changing the password fails when the new password change is incorrect.
     * <p>
     * The test stubs the password encoder to simulate an unsuccessful password match after encoding,
     * and verifies that the response message indicates failure.
     * </p>
     */
    @Test
    public void changePassword_failureIncorrectChange() {
        // Arrange: Prepare a user and new password.
        User user = new User();
        user.setEmail("client@example.com");
        user.setPassword("oldEncodedPassword");

        String newPassword = "newPassword";
        String newWrongEncodedPassword = "newWrongEncodedPassword";

        // Stub the password encoder for an incorrect match.
        when(passwordEncoder.encode(newPassword)).thenReturn(newWrongEncodedPassword);
        when(passwordEncoder.matches(newPassword, newWrongEncodedPassword)).thenReturn(false);

        // Stub the user repository.
        User updatedUser = new User();
        updatedUser.setEmail("client@example.com");
        updatedUser.setPassword(newWrongEncodedPassword);
        when(userRepository.getUserByEmail("client@example.com")).thenReturn(Optional.of(updatedUser));

        // Act: Execute changePassword.
        String result = authUseCase.changePassword(user, newPassword);

        // Assert: Verify that the failure message is returned.
        assertNotNull(result);
        assertEquals("Cambio de contraseña INCORRECTO. Vuelva a intentarlo.", result);
    }

    /**
     * Tests that changing the email with valid data returns a success message.
     * <p>
     * The test simulates the user repository returning an updated user with the new email,
     * and verifies that the changeEmail method returns the correct success message.
     * </p>
     */
    @Test
    public void changeEmail_success() {
        // Arrange: Prepare a user and a new email.
        User user = new User();
        user.setId(1L);
        user.setEmail("oldEmail@example.com");
        user.setPassword("encodedPassword");
        user.setRole(Role.ADMIN);

        String newEmail = "newEmail@example.com";

        // Stub: Simulate repository returning the updated user.
        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setEmail(newEmail);
        updatedUser.setPassword("encodedPassword");
        updatedUser.setRole(Role.ADMIN);
        when(userRepository.getUserByEmail(newEmail)).thenReturn(Optional.of(updatedUser));

        // Act: Execute changeEmail.
        String result = authUseCase.changeEmail(user, newEmail);

        // Assert: Verify that the success message is returned.
        assertNotNull(result);
        assertEquals("Cambio de email CORRECTO.", result);

        verify(userRepository).updateUser(user);
    }

    /**
     * Tests that changing the email fails when the update does not result in the expected new email.
     * <p>
     * The test simulates the repository returning a user with an incorrect email after update,
     * and verifies that the changeEmail method returns a failure message.
     * </p>
     */
    @Test
    public void changeEmail_failure_incorrectChange() {
        // Arrange: Prepare a user and a new email.
        User user = new User();
        user.setId(1L);
        user.setEmail("oldEmail@example.com");
        user.setPassword("encodedPassword");
        user.setRole(Role.ADMIN);

        String newEmail = "newEmail@example.com";

        // Simulate that the updated user in the repository has an incorrect email.
        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setEmail("incorrectEmail@example.com");
        updatedUser.setPassword("encodedPassword");
        updatedUser.setRole(Role.ADMIN);
        when(userRepository.getUserByEmail(newEmail)).thenReturn(Optional.of(updatedUser));

        // Act: Execute changeEmail.
        String result = authUseCase.changeEmail(user, newEmail);

        // Assert: Verify that the failure message is returned.
        assertNotNull(result);
        assertEquals("Cambio de email INCORRECTO. Vuelva a intentarlo.", result);

        verify(userRepository).updateUser(user);
    }

    /**
     * Tests that changing the email with an invalid email format throws an exception.
     */
    @Test
    public void changeEmail_failure_invalidFormatEmail() {
        // Arrange: Prepare a user and an invalid new email.
        User user = new User();
        user.setId(1L);
        user.setEmail("oldEmail@example.com");
        user.setPassword("encodedPassword");
        user.setRole(Role.ADMIN);

        String newEmail = "newEmail_example";

        // Act & Assert: Expect an IllegalArgumentException with the specified message.
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                authUseCase.changeEmail(user, newEmail)
        );
        assertEquals("Correo electrónico obligatorio o inválido", exception.getMessage());
    }

    /**
     * Tests that changing the email with a null new email field throws an exception.
     */
    @Test
    public void changeEmail_nullField_shouldThrowException() {
        // Arrange: Prepare a user.
        User user = new User();
        user.setId(1L);
        user.setEmail("oldEmail@example.com");
        user.setPassword("encodedPassword");
        user.setRole(Role.ADMIN);

        // Act & Assert: Expect an IllegalArgumentException when new email is null.
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                authUseCase.changeEmail(user, null)
        );
        assertEquals("Correo electrónico obligatorio o inválido", exception.getMessage());
    }

    /**
     * Tests that changing the email fails when the repository cannot find the user.
     * <p>
     * The test simulates that getUserByEmail throws an exception and verifies that the changeEmail method
     * propagates the error.
     * </p>
     */
    @Test
    public void changeEmail_failureUserNotFound() {
        // Arrange: Prepare a user and new email.
        User user = new User();
        user.setId(1L);
        user.setEmail("oldEmail@example.com");
        user.setPassword("encodedPassword");
        user.setRole(Role.ADMIN);

        String newEmail = "newEmail@example.com";

        // Simulate repository behavior for user not found.
        when(userRepository.getUserByEmail("newEmail@example.com"))
                .thenThrow(new RuntimeException("Usuario no encontrado"));

        // Act & Assert: Verify that the expected exception is thrown.
        Exception exception = assertThrows(RuntimeException.class, () ->
                authUseCase.changeEmail(user, newEmail)
        );
        assertEquals("Usuario no encontrado", exception.getMessage());
        verify(userRepository).getUserByEmail("newEmail@example.com");
    }
}