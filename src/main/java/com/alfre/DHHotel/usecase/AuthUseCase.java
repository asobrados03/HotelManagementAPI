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

    public AuthResponse login(LoginRequest request) {
        // Validar que los campos obligatorios no sean nulos
        if (request.getUsername() == null || request.getPassword() == null) {
            throw new IllegalArgumentException("Los campos obligatorios no pueden ser nulos");
        }

        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        UserDetails user = userRepository.getUserByEmail(request.getUsername())
                .orElseThrow(() -> new RuntimeException("El usuario no existe."));
        String token = jwtService.getToken(user);
        return AuthResponse.builder()
                .token(token)
                .build();
    }

    public AuthResponse registerClient(RegisterRequest request) {
        // Validar que los campos obligatorios no sean nulos
        if (request.getUsername() == null || request.getPassword() == null || request.getRole() == null ||
                request.getFirst_name() == null || request.getLast_name() == null || request.getPhone() == null) {
            throw new IllegalArgumentException("Los campos obligatorios no pueden ser nulos");
        }

        // Validar formato del email
        if(!request.getUsername().matches(EMAIL_REGEX)) {
            throw new IllegalArgumentException("Formato de correo electrónico inválido");
        }

        // Validar role
        if(!(request.getRole() == Role.CLIENT)) {
            throw new IllegalArgumentException("Role inválido");
        }

        User user = User.builder()
                .email(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();

        long userId = userRepository.createUser(user);

        user.setId(userId);

        // Validar formato del teléfono
        if (!request.getPhone().matches(RegexConstants.PHONE_REGEX)) {
            throw new IllegalArgumentException("Formato de teléfono inválido");
        }

        Client client = Client.builder()
                .user_id(userId)
                .first_name(request.getFirst_name())
                .last_name(request.getLast_name())
                .phone(request.getPhone())
                .build();

        clientRepository.createClient(client);

        return AuthResponse.builder()
                .token(jwtService.getToken(user))
                .build();
    }

    public AuthResponse registerAdministrator(RegisterRequest request) {
        // Validar que los campos obligatorios no sean nulos
        if (request.getUsername() == null || request.getPassword() == null || request.getName() == null
                || request.getRole() == null) {
            throw new IllegalArgumentException("Los campos obligatorios no pueden ser nulos");
        }

        // Validar formato del email
        if(!request.getUsername().matches(EMAIL_REGEX)) {
            throw new IllegalArgumentException("Formato de correo electrónico inválido");
        }

        // Validar role
        if(request.getRole() == Role.CLIENT) {
            throw new IllegalArgumentException("Role inválido");
        }

        User user = User.builder()
                .email(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();

        long userId = userRepository.createUser(user);

        user.setId(userId);

        Administrator admin = Administrator.builder()
                .user_id(userId)
                .name(request.getName())
                .build();

        adminRepository.createAdministrator(admin);

        return AuthResponse.builder()
                .token(jwtService.getToken(user))
                .build();
    }

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

    public AdminDTO getInfoAdmin(User user) {
        Administrator administrator = adminRepository.getAdministratorByUserId(user.id)
                .orElseThrow(() -> new RuntimeException("Admin no encontrado"));

        return AdminDTO.builder()
                .email(user.email)
                .role(user.role)
                .name(administrator.name)
                .build();
    }

    public String changePassword(User user, String newPassword) {
        String newPasswordEncoded = passwordEncoder.encode(newPassword);

        user.setPassword(newPasswordEncoded);
        userRepository.updateUser(user);

        User updatedUser = userRepository.getUserByEmail(user.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if(passwordEncoder.matches(newPassword, updatedUser.getPassword())) {
            return "Cambio de contraseña CORRECTO.";
        } else {
            return "Cambio de contraseña INCORRECTO. Vuelva a intentarlo.";
        }
    }

    public String changeEmail(User user, String newEmail) {
        // Limpieza de la entrada
        if(newEmail != null){
            newEmail = newEmail.trim();
        }

        // Validar formato del email
        if(newEmail == null || !newEmail.matches(EMAIL_REGEX)) {
            throw new IllegalArgumentException("Correo electrónico obligatorio o inválido");
        }

        user.setEmail(newEmail);
        userRepository.updateUser(user);

        User updatedUser = userRepository.getUserByEmail(user.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Verificar que el email actualizado en la base de datos coincida con newEmail
        if(updatedUser.email.equals(newEmail)) {
            return "Cambio de email CORRECTO.";
        } else {
            return "Cambio de email INCORRECTO. Vuelva a intentarlo.";
        }
    }
}