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

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthUseCase authUseCase;
    private final ClientUseCase clientUseCase;
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    public AuthController(AuthUseCase authUseCase, ClientUseCase clientUseCase) {
        this.authUseCase = authUseCase;
        this.clientUseCase = clientUseCase;
    }

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

    @PostMapping("/register/client")
    public ResponseEntity<?> registerClient(@RequestBody RegisterRequest request) {
        try {
            return ResponseEntity.ok(authUseCase.registerClient(request));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/register/admin")
    public ResponseEntity<?> registerAdministrator(@RequestBody RegisterRequest request) {
        try {
            return ResponseEntity.ok(authUseCase.registerAdministrator(request));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('CLIENT')")
    @GetMapping("/me/client")
    public ResponseEntity<?> getInfoClient(@AuthenticationPrincipal User user){
        try {
            return ResponseEntity.ok(authUseCase.getInfoClient(user));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Autenticación no válida o error en el servicio"));
        }
    }

    @GetMapping("/me/admin")
    public ResponseEntity<?> getInfoAdmin(@AuthenticationPrincipal User user){
        try {
            return ResponseEntity.ok(authUseCase.getInfoAdmin(user));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Autenticación no válida o error en el servicio"));
        }
    }

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
            // Log detallado para diagnóstico
            logger.error("Cliente no encontrado para userId: {}", user.id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Cliente no registrado"));
        }
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@AuthenticationPrincipal User user, @RequestBody UserDTO request) {
        try {
            return ResponseEntity.ok(authUseCase.changePassword(user, request.newPassword));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Autenticación no válida o error en el servicio"));
        }
    }

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