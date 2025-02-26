package com.alfre.DHHotel.usecase;

import com.alfre.DHHotel.adapter.web.dto.AdminDTO;
import com.alfre.DHHotel.adapter.web.dto.ClientDTO;
import com.alfre.DHHotel.adapter.web.dto.LoginRequest;
import com.alfre.DHHotel.adapter.web.dto.RegisterRequest;
import com.alfre.DHHotel.adapter.web.dto.AuthResponse;
import com.alfre.DHHotel.adapter.security.jwt.JwtService;
import com.alfre.DHHotel.domain.model.Administrator;
import com.alfre.DHHotel.domain.model.Client;
import com.alfre.DHHotel.domain.model.Role;
import com.alfre.DHHotel.domain.model.User;
import com.alfre.DHHotel.domain.repository.AdministratorRepository;
import com.alfre.DHHotel.domain.repository.ClientRepository;
import com.alfre.DHHotel.domain.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/**
 * This service class handles authentication and authorization use cases,
 * including user login, registration, profile retrieval, and credential updates.
 * It interacts with repositories to manage user, client, and administrator data
 * and generates JWT tokens for authenticated sessions.
 *
 * <p>Functionality includes:
 * <ul>
 *     <li>User authentication and JWT token generation</li>
 *     <li>Client and administrator registration</li>
 *     <li>Retrieving authenticated user details</li>
 *     <li>Password and email updates</li>
 * </ul>
 *
 * <p>Uses {@link JwtService} for token handling and {@link AuthenticationManager}
 * for Spring Security authentication.
 *
 * @author Alfredo Sobrados González
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AuthUseCase {
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final AdministratorRepository adminRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    private static final String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";

    /**
     * Authenticates a user and generates a JWT token.
     *
     * @param request the login request containing username (email) and password.
     * @return an {@link AuthResponse} containing the generated JWT token.
     * @throws IllegalArgumentException if required fields are null.
     * @throws RuntimeException if the user does not exist.
     */
    public AuthResponse login(LoginRequest request) {
        if (request.getUsername() == null || request.getPassword() == null) {
            throw new IllegalArgumentException("Los campos obligatorios no pueden ser nulos");
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserDetails user = userRepository.getUserByEmail(request.getUsername())
                .orElseThrow(() -> new RuntimeException("El usuario no existe."));

        return AuthResponse.builder().token(jwtService.getToken(user)).build();
    }

    /**
     * Registers a new client and returns a JWT token.
     *
     * @param request the registration request containing client details.
     * @return an {@link AuthResponse} containing the generated JWT token.
     * @throws IllegalArgumentException if required fields are null, email format is invalid, or role is incorrect.
     */
    public AuthResponse registerClient(RegisterRequest request) {
        validateClientRequest(request);

        User user = createUser(request.getUsername(), request.getPassword(), Role.CLIENT);
        long userId = userRepository.createUser(user);
        user.setId(userId);

        validatePhoneNumber(request.getPhone());

        Client client = Client.builder()
                .user_id(userId)
                .first_name(request.getFirst_name())
                .last_name(request.getLast_name())
                .phone(request.getPhone())
                .build();

        clientRepository.createClient(client);

        return AuthResponse.builder().token(jwtService.getToken(user)).build();
    }

    /**
     * Registers a new administrator and returns a JWT token.
     *
     * @param request the registration request containing administrator details.
     * @return an {@link AuthResponse} containing the generated JWT token.
     * @throws IllegalArgumentException if required fields are null, email format is invalid, or role is incorrect.
     */
    public AuthResponse registerAdministrator(RegisterRequest request) {
        validateAdminRequest(request);

        User user = createUser(request.getUsername(), request.getPassword(), request.getRole());
        long userId = userRepository.createUser(user);
        user.setId(userId);

        Administrator admin = Administrator.builder()
                .user_id(userId)
                .name(request.getName())
                .build();

        adminRepository.createAdministrator(admin);

        return AuthResponse.builder().token(jwtService.getToken(user)).build();
    }

    /**
     * Retrieves client details based on the authenticated user.
     *
     * @param user the authenticated user.
     * @return a {@link ClientDTO} containing client information.
     * @throws RuntimeException if the client is not found.
     */
    public ClientDTO getInfoClient(User user) {
        Client client = clientRepository.getClientByUserId(user.id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        return ClientDTO.builder()
                .firstName(client.first_name)
                .lastName(client.last_name)
                .email(user.email)
                .phone(client.phone)
                .build();
    }

    /**
     * Retrieves administrator details based on the authenticated user.
     *
     * @param user the authenticated user.
     * @return an {@link AdminDTO} containing administrator information.
     * @throws RuntimeException if the administrator is not found.
     */
    public AdminDTO getInfoAdmin(User user) {
        Administrator administrator = adminRepository.getAdministratorByUserId(user.id)
                .orElseThrow(() -> new RuntimeException("Admin no encontrado"));

        return AdminDTO.builder()
                .email(user.email)
                .role(user.role)
                .name(administrator.name)
                .build();
    }

    /**
     * Changes the user's password.
     *
     * @param user        the authenticated user.
     * @param newPassword the new password to be set.
     * @return a success or failure message.
     */
    public String changePassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.updateUser(user);

        User updatedUser = userRepository.getUserByEmail(user.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return passwordEncoder.matches(newPassword, updatedUser.getPassword())
                ? "Cambio de contraseña CORRECTO."
                : "Cambio de contraseña INCORRECTO. Vuelva a intentarlo.";
    }

    /**
     * Changes the user's email.
     *
     * @param user     the authenticated user.
     * @param newEmail the new email to be set.
     * @return a success or failure message.
     * @throws IllegalArgumentException if the email is null or invalid.
     */
    public String changeEmail(User user, String newEmail) {
        if (newEmail == null || !newEmail.trim().matches(EMAIL_REGEX)) {
            throw new IllegalArgumentException("Correo electrónico obligatorio o inválido");
        }

        user.setEmail(newEmail.trim());
        userRepository.updateUser(user);

        User updatedUser = userRepository.getUserByEmail(user.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return updatedUser.email.equals(newEmail)
                ? "Cambio de email CORRECTO."
                : "Cambio de email INCORRECTO. Vuelva a intentarlo.";
    }

    // --------------------------- PRIVATE METHODS --------------------------- //

    /**
     * Validates the request for client registration.
     *
     * @param request the registration request.
     * @throws IllegalArgumentException if required fields are missing or invalid.
     */
    private void validateClientRequest(RegisterRequest request) {
        if (request.getUsername() == null || request.getPassword() == null || request.getRole() == null ||
                request.getFirst_name() == null || request.getLast_name() == null || request.getPhone() == null) {
            throw new IllegalArgumentException("Los campos obligatorios no pueden ser nulos");
        }
        if (!request.getUsername().matches(EMAIL_REGEX)) {
            throw new IllegalArgumentException("Formato de correo electrónico inválido");
        }
        if (request.getRole() != Role.CLIENT) {
            throw new IllegalArgumentException("Role inválido");
        }
    }

    /**
     * Validates the request for administrator registration.
     *
     * @param request the registration request.
     * @throws IllegalArgumentException if required fields are missing or invalid.
     */
    private void validateAdminRequest(RegisterRequest request) {
        if (request.getUsername() == null || request.getPassword() == null || request.getName() == null
                || request.getRole() == null) {
            throw new IllegalArgumentException("Los campos obligatorios no pueden ser nulos");
        }
        if (!request.getUsername().matches(EMAIL_REGEX)) {
            throw new IllegalArgumentException("Formato de correo electrónico inválido");
        }
        if (request.getRole() == Role.CLIENT) {
            throw new IllegalArgumentException("Role inválido");
        }
    }

    /**
     * Creates a new {@link User} instance.
     *
     * @param email    the user's email.
     * @param password the user's password.
     * @param role     the user's role.
     * @return a new {@link User} object.
     */
    private User createUser(String email, String password, Role role) {
        return User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(role)
                .build();
    }

    /**
     * Validates the phone number format.
     *
     * @param phone the phone number to validate.
     * @throws IllegalArgumentException if the phone format is invalid.
     */
    private void validatePhoneNumber(String phone) {
        if (!phone.matches(RegexConstants.PHONE_REGEX)) {
            throw new IllegalArgumentException("Formato de teléfono inválido");
        }
    }
}