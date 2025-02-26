package com.alfre.DHHotel.adapter.web.controller;

import com.alfre.DHHotel.usecase.AuthUseCase;
import com.alfre.DHHotel.adapter.web.dto.LoginRequest;
import com.alfre.DHHotel.adapter.web.dto.RegisterRequest;
import com.alfre.DHHotel.adapter.web.dto.UpdateProfileRequest;
import com.alfre.DHHotel.adapter.web.dto.UserDTO;
import com.alfre.DHHotel.domain.model.User;
import com.alfre.DHHotel.usecase.ClientUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * This class handles authentication-related HTTP requests, such as login, registration,
 * and profile updates.
 * It delegates business logic to the AuthUseCase
 * and ClientUseCase components.
 *
 * @author Alfredo Sobrados González
 */
@RestController
@RequestMapping("/api")
public class AuthController {
    private final AuthUseCase authUseCase;
    private final ClientUseCase clientUseCase;
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    /**
     * Constructs an AuthController with the specified use case components.
     *
     * @param authUseCase   the authentication use case that handles login, registration, etc.
     * @param clientUseCase the client use case that manages client-specific operations
     */
    public AuthController(AuthUseCase authUseCase, ClientUseCase clientUseCase) {
        this.authUseCase = authUseCase;
        this.clientUseCase = clientUseCase;
    }

    /**
     * Handles user login requests.
     * <p>
     * Expects a LoginRequest in the request body and returns a JWT or error message.
     * </p>
     *
     * @param request the LoginRequest containing user credentials
     * @return a ResponseEntity containing the JWT on success or an error message on failure
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(authUseCase.login(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Credenciales inválidas"));
        }
    }

    /**
     * Handles registration requests for new clients.
     *
     * @param request the RegisterRequest containing registration details for the client
     * @return a ResponseEntity containing the newly registered client details or an error message
     */
    @PostMapping("/register/client")
    public ResponseEntity<?> registerClient(@RequestBody RegisterRequest request) {
        try {
            return ResponseEntity.ok(authUseCase.registerClient(request));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * Handles registration requests for new administrators.
     *
     * @param request the RegisterRequest containing registration details for the administrator
     * @return a ResponseEntity containing the newly registered administrator details or an error message
     */
    @PostMapping("/register/admin")
    public ResponseEntity<?> registerAdministrator(@RequestBody RegisterRequest request) {
        try {
            return ResponseEntity.ok(authUseCase.registerAdministrator(request));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * Retrieves the client profile information for the currently authenticated user.
     * <p>
     * Only users with the 'CLIENT' role are authorized to access this endpoint.
     * </p>
     *
     * @param user the authenticated User object
     * @return a ResponseEntity containing the client's profile information or an error message
     */
    @PreAuthorize("hasRole('CLIENT')")
    @GetMapping("/me/client")
    public ResponseEntity<?> getInfoClient(@AuthenticationPrincipal User user) {
        try {
            return ResponseEntity.ok(authUseCase.getInfoClient(user));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Autenticación no válida o error en el servicio"));
        }
    }

    /**
     * Retrieves the administrator profile information for the currently authenticated user.
     *
     * @param user the authenticated User object
     * @return a ResponseEntity containing the administrator's profile information or an error message
     */
    @GetMapping("/me/admin")
    public ResponseEntity<?> getInfoAdmin(@AuthenticationPrincipal User user) {
        try {
            return ResponseEntity.ok(authUseCase.getInfoAdmin(user));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Autenticación no válida o error en el servicio"));
        }
    }

    /**
     * Updates the profile information for the currently authenticated client.
     * <p>
     * Only users with the 'CLIENT' role are allowed to update their profile.
     * </p>
     *
     * @param request the UpdateProfileRequest containing the new profile information
     * @param user the authenticated User object
     * @return a ResponseEntity containing the updated client profile or an error message
     */
    @PreAuthorize("hasRole('CLIENT')")
    @PutMapping("/me")
    public ResponseEntity<?> updateProfile(
            @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal User user
    ) {
        try {
            return ResponseEntity.ok(clientUseCase.updateClientProfile(user, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            // Detailed logging for diagnostic purposes
            logger.error("Cliente no encontrado para userId: {}", user.id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Cliente no registrado"));
        }
    }

    /**
     * Changes the password for the currently authenticated user.
     *
     * @param user the authenticated User object
     * @param request a UserDTO containing the new password
     * @return a ResponseEntity containing the result of the password change or an error message
     */
    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@AuthenticationPrincipal User user, @RequestBody UserDTO request) {
        try {
            return ResponseEntity.ok(authUseCase.changePassword(user, request.newPassword));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Autenticación no válida o error en el servicio"));
        }
    }

    /**
     * Changes the email for the currently authenticated user.
     *
     * @param user the authenticated User object
     * @param request a UserDTO containing the new email address
     * @return a ResponseEntity containing the result of the email change or an error message
     */
    @PutMapping("/change-email")
    public ResponseEntity<?> changeEmail(@AuthenticationPrincipal User user, @RequestBody UserDTO request) {
        try {
            return ResponseEntity.ok(authUseCase.changeEmail(user, request.newEmail));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Autenticación no válida o error en el servicio"));
        }
    }
}