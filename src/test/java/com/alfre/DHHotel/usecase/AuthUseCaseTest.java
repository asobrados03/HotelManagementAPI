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

    @BeforeEach
    public void setup() {
        // Inyectamos los mocks en la clase que vamos a testear
        authUseCase = new AuthUseCase(userRepository, clientRepository, adminRepository, jwtService, passwordEncoder, authenticationManager);
    }

    @Test
    public void loginTest_success() {
        // Arrange: Preparamos los datos de entrada y definimos el comportamiento de los mocks

        // Creamos un LoginRequest con usuario y contraseña
        LoginRequest request = new LoginRequest();
        request.setUsername("user@example.com");
        request.setPassword("password");

        // Simulamos que la autenticación es exitosa.
        // Crea una instancia dummy de Authentication, puede ser un UsernamePasswordAuthenticationToken
        Authentication dummyAuth = new UsernamePasswordAuthenticationToken("user@example.com", "password");

        // Cuando se invoque el método authenticate con cualquier UsernamePasswordAuthenticationToken, se retornará dummyAuth
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(dummyAuth);

        // Creamos un usuario ficticio (dummy) que se devolverá cuando se consulte el repositorio
        User dummyUser = User.builder()
                .id(1L)
                .email("user@example.com")
                .password("encodedPassword")
                .role(Role.CLIENT)
                .build();
        when(userRepository.getUserByEmail("user@example.com")).thenReturn(Optional.of(dummyUser));

        // Simulamos que el servicio JWT retorna un token
        when(jwtService.getToken(dummyUser)).thenReturn("dummyToken");

        // Act: Ejecutamos el método que se está testeando
        AuthResponse response = authUseCase.login(request);

        // Assert: Verificamos que la respuesta es la esperada
        assertNotNull(response);
        assertEquals("dummyToken", response.getToken());

        // Además, podemos verificar que se hayan invocado los métodos de los mocks
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(userRepository).getUserByEmail("user@example.com");
        verify(jwtService).getToken(dummyUser);
    }

    @Test
    public void loginTest_failure_becauseWrongPassword() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername("user@example.com");
        request.setPassword("wrongPassword");

        // Simula que el método authenticate lanza una excepción, indicando que la autenticación falla
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new RuntimeException("Authentication failed"));

        // Act & Assert: Verifica que se lanza la excepción esperada
        Exception exception = assertThrows(RuntimeException.class, () ->
            authUseCase.login(request)
        );

        assertEquals("Authentication failed", exception.getMessage());
    }

    @Test
    public void loginTest_failure_becauseWrongUsername() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername("wronguser@example.com");
        request.setPassword("securePassword");

        // Simula que el método authenticate lanza una excepción, indicando que la autenticación falla
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new RuntimeException("Authentication failed"));

        // Act & Assert: Verifica que se lanza la excepción esperada
        Exception exception = assertThrows(RuntimeException.class, () ->
            authUseCase.login(request)
        );

        assertEquals("Authentication failed", exception.getMessage());
    }

    @Test
    void login_nullField_shouldThrowException() {
        // Arrange: Creamos una solicitud con un campo nulo
        LoginRequest request = new LoginRequest();
        request.setUsername(null); // El email es nulo
        request.setPassword("securePassword");

        // Act & Assert: Verificamos que se lanza la excepción esperada
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            authUseCase.login(request)
        );

        // Validamos el mensaje de la excepción
        assertEquals("Los campos obligatorios no pueden ser nulos", exception.getMessage());
    }

    @Test
    public void registerClient_success() {
        // Arrange: Preparamos los datos de entrada y definimos el comportamiento de los mocks
        RegisterRequest request = new RegisterRequest();
        request.setUsername("client@example.com");
        request.setPassword("encodedPassword");
        request.setFirst_name("Bob");
        request.setLast_name("Smith");
        request.setRole(Role.CLIENT);
        request.setPhone("+34 618 34 54 56");

        // Stub para la creación del usuario, usando un matcher
        when(userRepository.createUser(any(User.class))).thenReturn(1L);

        // Simulamos que el servicio JWT retorna un token
        when(jwtService.getToken(any(User.class))).thenReturn("dummyToken");

        // Act: Ejecutamos el método a testear
        AuthResponse response = authUseCase.registerClient(request);

        // Assert: Validamos la respuesta
        assertNotNull(response);
        assertEquals("dummyToken", response.getToken());

        // Verificamos que se hayan invocado los métodos de los mocks con un argumento que cumpla ciertas condiciones
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

    @Test
    public void registerClient_withWrongFormatPhone() {
        // Arrange: Preparamos los datos de entrada
        RegisterRequest request = new RegisterRequest();
        request.setUsername("client@example.com");
        request.setPassword("encodedPassword");
        request.setFirst_name("Bob");
        request.setLast_name("Smith");
        request.setRole(Role.CLIENT);
        request.setPhone("414-3445");

        // Act & Assert: Se espera que al ejecutar registerClient se lance una IllegalArgumentException
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            authUseCase.registerClient(request)
        );

        // Verificamos que el mensaje de la excepción sea el esperado
        assertEquals("Formato de teléfono inválido", exception.getMessage());
    }

    @Test
    public void registerClient_withWrongFormatEmail() {
        // Arrange: Preparamos los datos de entrada
        RegisterRequest request = new RegisterRequest();
        request.setUsername("client_example");
        request.setPassword("encodedPassword");
        request.setFirst_name("Bob");
        request.setLast_name("Smith");
        request.setRole(Role.CLIENT);
        request.setPhone("+34 618 34 54 56");

        // Act & Assert: Se espera que al ejecutar registerClient se lance una IllegalArgumentException
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            authUseCase.registerClient(request)
        );

        // Verificamos que el mensaje de la excepción sea el esperado
        assertEquals("Formato de correo electrónico inválido", exception.getMessage());
    }

    @Test
    public void registerClient_withWrongRole() {
        // Arrange: Preparamos los datos de entrada
        RegisterRequest request = new RegisterRequest();
        request.setUsername("client@example.com");
        request.setPassword("encodedPassword");
        request.setFirst_name("Bob");
        request.setLast_name("Smith");
        request.setRole(Role.ADMIN);
        request.setPhone("+34 618 34 54 56");

        // Act & Assert: Se espera que al ejecutar registerClient se lance una IllegalArgumentException
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                authUseCase.registerClient(request)
        );

        // Verificamos que el mensaje de la excepción sea el esperado
        assertEquals("Role inválido", exception.getMessage());
    }

    @Test
    public void registerClient_nullField_shouldThrowException() {
        // Arrange: Creamos una solicitud con un campo nulo
        RegisterRequest request = new RegisterRequest();
        request.setUsername(null); // El email es nulo
        request.setPassword("securePassword");
        request.setFirst_name("Alice");
        request.setLast_name("Doe");
        request.setRole(Role.CLIENT);
        request.setPhone(null);

        // Act & Assert: Verificamos que se lanza la excepción esperada
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
            authUseCase.registerClient(request)
        );

        // Validamos el mensaje de la excepción
        assertEquals("Los campos obligatorios no pueden ser nulos", exception.getMessage());
    }

    @Test
    public void registerAdministrator_success() {
        // Arrange: Preparamos los datos de entrada y definimos el comportamiento de los mocks
        RegisterRequest request = new RegisterRequest();
        request.setUsername("admin@example.com");
        request.setPassword("encodedPassword");
        request.setName("Jack Johnson");
        request.setRole(Role.ADMIN);

        // Stub para la creación del usuario, usando un matcher
        when(userRepository.createUser(any(User.class))).thenReturn(1L);

        // Simulamos que el servicio JWT retorna un token
        when(jwtService.getToken(any(User.class))).thenReturn("dummyToken");

        // Act: Ejecutamos el método a testear
        AuthResponse response = authUseCase.registerAdministrator(request);

        // Assert: Validamos la respuesta
        assertNotNull(response);
        assertEquals("dummyToken", response.getToken());

        // Verificamos que se hayan invocado los métodos de los mocks con un argumento que cumpla ciertas condiciones
        verify(userRepository).createUser(argThat(user ->
                "admin@example.com".equals(user.email) &&
                        Role.ADMIN.equals(user.role)
        ));
        verify(adminRepository).createAdministrator(argThat(admin ->
                "Jack Johnson".equals(admin.name)
        ));
        verify(jwtService).getToken(any(User.class));
    }

    @Test
    public void registerAdministrator_nullField_shouldThrowException() {
        // Arrange: Preparamos los datos de entrada
        RegisterRequest request = new RegisterRequest();
        request.setUsername(null);
        request.setPassword("encodedPassword");
        request.setName("Jack Johnson");
        request.setRole(Role.ADMIN);

        // Act & Assert: Verificamos que se lanza la excepción esperada
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                authUseCase.registerAdministrator(request)
        );

        // Validamos el mensaje de la excepción
        assertEquals("Los campos obligatorios no pueden ser nulos", exception.getMessage());
    }

    @Test
    public void registerAdministrator_withWrongRole() {
        // Arrange: Preparamos los datos de entrada
        RegisterRequest request = new RegisterRequest();
        request.setUsername("admin@example.com");
        request.setPassword("encodedPassword");
        request.setName("Jack Johnson");
        request.setRole(Role.CLIENT);

        // Act & Assert: Verificamos que se lanza la excepción esperada
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                authUseCase.registerAdministrator(request)
        );

        // Validamos el mensaje de la excepción
        assertEquals("Role inválido", exception.getMessage());
    }

    @Test
    public void registerAdministrator_withWrongFormatEmail() {
        // Arrange: Preparamos los datos de entrada
        RegisterRequest request = new RegisterRequest();
        request.setUsername("admin_example");
        request.setPassword("encodedPassword");
        request.setName("Jack Johnson");
        request.setRole(Role.ADMIN);

        // Act & Assert: Verificamos que se lanza la excepción esperada
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                authUseCase.registerAdministrator(request)
        );

        // Validamos el mensaje de la excepción
        assertEquals("Formato de correo electrónico inválido", exception.getMessage());
    }

    @Test
    public void getInfoClient_success() {
        // Arrange: Preparamos los datos de entrada
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

        // Stub para conseguir el cliente, usando un matcher
        when(clientRepository.getClientByUserId(user.id)).thenReturn(Optional.of(client));

        // Act: Ejecutamos el método a testear
        ClientDTO response = authUseCase.getInfoClient(user);

        // Assert: Validamos la respuesta
        ClientDTO responseExpected = ClientDTO.builder()
                .firstName(client.first_name)
                .lastName(client.last_name)
                .email(user.email)
                .phone(client.phone)
                .build();

        assertNotNull(response);
        assertEquals(response, responseExpected);

        // Verificamos que se haya invocado el método de el mock con un argumento que cumpla ciertas condiciones
        verify(clientRepository).getClientByUserId(1L);
    }

    @Test
    public void getInfoClient_failure() {
        // Arrange: Preparamos los datos de entrada
        User user = new User();
        user.setId(4L);
        user.setEmail("client@example.com");
        user.setPassword("encodedPassword");
        user.setRole(Role.CLIENT);

        // Stub para conseguir el cliente, usando un matcher
        when(clientRepository.getClientByUserId(user.id)).thenThrow(new RuntimeException("Cliente no encontrado"));

        // Act & Assert: Verificamos que se lanza la excepción esperada
        Exception exception = assertThrows(RuntimeException.class, () ->
                authUseCase.getInfoClient(user)
        );

        // Validamos el mensaje de la excepción
        assertEquals("Cliente no encontrado", exception.getMessage());

        // Verificamos que se haya invocado el método de el mock con un argumento que cumpla ciertas condiciones
        verify(clientRepository).getClientByUserId(4L);
    }

    @Test
    public void getInfoAdmin_success() {
        // Arrange: Preparamos los datos de entrada
        User user = new User();
        user.setId(1L);
        user.setEmail("admin@example.com");
        user.setPassword("encodedPassword");
        user.setRole(Role.ADMIN);

        Administrator administrator = new Administrator();
        administrator.setId(1L);
        administrator.setUser_id(1L);
        administrator.setName("Jennifer Collins");

        // Stub para conseguir el cliente, usando un matcher
        when(adminRepository.getAdministratorByUserId(user.id)).thenReturn(Optional.of(administrator));

        // Act: Ejecutamos el método a testear
        AdminDTO response = authUseCase.getInfoAdmin(user);

        // Assert: Validamos la respuesta
        AdminDTO responseExpected = AdminDTO.builder()
                .email(user.email)
                .role(user.role)
                .name(administrator.name)
                .build();

        assertNotNull(response);
        assertEquals(response, responseExpected);

        // Verificamos que se haya invocado el método de el mock con un argumento que cumpla ciertas condiciones
        verify(adminRepository).getAdministratorByUserId(1L);
    }

    @Test
    public void getInfoAdmin_failure() {
        // Arrange: Preparamos los datos de entrada
        User user = new User();
        user.setId(4L);
        user.setEmail("admin@example.com");
        user.setPassword("encodedPassword");
        user.setRole(Role.ADMIN);

        // Stub para conseguir el cliente, usando un matcher
        when(adminRepository.getAdministratorByUserId(user.id)).thenThrow(new RuntimeException("Admin no encontrado"));

        // Act & Assert: Verificamos que se lanza la excepción esperada
        Exception exception = assertThrows(RuntimeException.class, () ->
                authUseCase.getInfoAdmin(user)
        );

        // Validamos el mensaje de la excepción
        assertEquals("Admin no encontrado", exception.getMessage());

        // Verificamos que se haya invocado el método de el mock con un argumento que cumpla ciertas condiciones
        verify(adminRepository).getAdministratorByUserId(4L);
    }

    @Test
    public void changePassword_success() {
        // Arrange: Preparamos los datos de entrada
        User user = new User();
        user.setEmail("client@example.com");
        user.setPassword("oldEncodedPassword");

        String newPassword = "newPassword";
        String newEncodedPassword = "newEncodedPassword";

        // Configuramos el behavior del passwordEncoder
        when(passwordEncoder.encode(newPassword)).thenReturn(newEncodedPassword);
        when(passwordEncoder.matches(newPassword, newEncodedPassword)).thenReturn(true);

        // Simulamos que, tras la actualización, el repositorio devuelve un usuario con la nueva contraseña codificada
        User updatedUser = new User();
        updatedUser.setEmail("client@example.com");
        updatedUser.setPassword(newEncodedPassword);
        when(userRepository.getUserByEmail("client@example.com")).thenReturn(Optional.of(updatedUser));

        // Act: Ejecutamos changePassword
        String result = authUseCase.changePassword(user, newPassword);

        // Assert: Se espera que el método retorne el mensaje de éxito
        assertNotNull(result);
        assertEquals("Cambio de contraseña CORRECTO.", result);

        // Además, verificamos que se llamó a updateUser con el usuario modificado
        verify(userRepository).updateUser(user);
    }

    @Test
    public void changePassword_failureUserNotFound() {
        // Arrange: Preparamos los datos de entrada
        User user = new User();
        user.setEmail("client@example.com");
        user.setPassword("oldEncodedPassword");

        String newPassword = "newPassword";

        // Simulamos que, tras la actualización, el repositorio no encuentra el usuario
        when(userRepository.getUserByEmail("client@example.com"))
                .thenThrow(new RuntimeException("Usuario no encontrado"));

        // Act & Assert: Ejecutamos changePassword
        Exception exception = assertThrows(RuntimeException.class, () ->
            authUseCase.changePassword(user, newPassword)
        );

        // Validamos el mensaje de la excepción
        assertEquals("Usuario no encontrado", exception.getMessage());

        // Verificamos que se haya invocado el método de el mock con un argumento que cumpla ciertas condiciones
        verify(userRepository).getUserByEmail("client@example.com");
    }

    @Test
    public void changePassword_failureIncorrectChange() {
        // Arrange: Preparamos los datos de entrada
        User user = new User();
        user.setEmail("client@example.com");
        user.setPassword("oldEncodedPassword");

        String newPassword = "newPassword";
        String newWrongEncodedPassword = "newWrongEncodedPassword";

        // Configuramos el behavior del passwordEncoder
        when(passwordEncoder.encode(newPassword)).thenReturn(newWrongEncodedPassword);
        when(passwordEncoder.matches(newPassword, newWrongEncodedPassword)).thenReturn(false);

        // Simulamos que, tras la actualización, el repositorio devuelve un usuario con la nueva contraseña codificada
        User updatedUser = new User();
        updatedUser.setEmail("client@example.com");
        updatedUser.setPassword(newWrongEncodedPassword);
        when(userRepository.getUserByEmail("client@example.com")).thenReturn(Optional.of(updatedUser));

        // Act: Ejecutamos changePassword
        String result = authUseCase.changePassword(user, newPassword);

        // Assert: Se espera que el método retorne el mensaje de éxito
        assertNotNull(result);
        assertEquals("Cambio de contraseña INCORRECTO. Vuelva a intentarlo.", result);
    }

    @Test
    public void changeEmail_success() {
        // Arrange: Preparamos los datos de entrada
        User user = new User();
        user.setId(1L);
        user.setEmail("oldEmail@example.com");
        user.setPassword("encodedPassword");
        user.setRole(Role.ADMIN);

        String newEmail = "newEmail@example.com";

        // Creamos el usuario actualizado que se devolverá al consultar el repositorio
        User updatedUser = new User();
        updatedUser.setId(1L);
        updatedUser.setEmail(newEmail);
        updatedUser.setPassword("encodedPassword");
        updatedUser.setRole(Role.ADMIN);

        // Stub: Simulamos que, tras actualizar, el repositorio devuelve el usuario con el email actualizado.
        // Suponemos que user.getUsername() retorna el email actualizado (o newEmail).
        when(userRepository.getUserByEmail(newEmail)).thenReturn(Optional.of(updatedUser));

        // Act: Ejecutamos changeEmail
        String result = authUseCase.changeEmail(user, newEmail);

        // Assert: Se espera que el método retorne el mensaje de éxito
        assertNotNull(result);
        assertEquals("Cambio de email CORRECTO.", result);

        // Verificamos que se llamó a updateUser con el usuario modificado
        verify(userRepository).updateUser(user);
    }

    @Test
    public void changeEmail_failure_incorrectChange() {
        // Arrange: Preparamos los datos de entrada
        User user = new User();
        user.setId(1L);
        user.setEmail("oldEmail@example.com");
        user.setPassword("encodedPassword");
        user.setRole(Role.ADMIN);

        String newEmail = "newEmail@example.com";

        // Creamos un usuario actualizado simulando un fallo en la actualización del email
        User updatedUser = new User();
        updatedUser.setId(1L);
        // Se simula que en la base de datos el email no se actualizó correctamente
        updatedUser.setEmail("incorrectEmail@example.com"); // distinto de newEmail
        updatedUser.setPassword("encodedPassword");
        updatedUser.setRole(Role.ADMIN);

        // Stub: cuando se consulte el usuario por el nuevo email, se retorna el usuario con email incorrecto
        when(userRepository.getUserByEmail(newEmail)).thenReturn(Optional.of(updatedUser));

        // Act: Ejecutamos changeEmail
        String result = authUseCase.changeEmail(user, newEmail);

        // Assert: Se espera que el método retorne el mensaje de fallo
        assertNotNull(result);
        assertEquals("Cambio de email INCORRECTO. Vuelva a intentarlo.", result);

        // Verificamos que se llamó a updateUser con el usuario modificado
        verify(userRepository).updateUser(user);
    }

    @Test
    public void changeEmail_failure_invalidFormatEmail() {
        // Arrange: Preparamos los datos de entrada
        User user = new User();
        user.setId(1L);
        user.setEmail("oldEmail@example.com");
        user.setPassword("encodedPassword");
        user.setRole(Role.ADMIN);

        String newEmail = "newEmail_example";

        // Act & Assert: Verificamos que se lanza la excepción esperada
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                authUseCase.changeEmail(user, newEmail)
        );

        // Validamos el mensaje de la excepción
        assertEquals("Correo electrónico obligatorio o inválido", exception.getMessage());
    }

    @Test
    public void changeEmail_nullField_shouldThrowException() {
        // Arrange: Preparamos los datos de entrada
        User user = new User();
        user.setId(1L);
        user.setEmail("oldEmail@example.com");
        user.setPassword("encodedPassword");
        user.setRole(Role.ADMIN);

        // Act & Assert: Verificamos que se lanza la excepción esperada
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                authUseCase.changeEmail(user, null)
        );

        // Validamos el mensaje de la excepción
        assertEquals("Correo electrónico obligatorio o inválido", exception.getMessage());
    }

    @Test
    public void changeEmail_failureUserNotFound() {
        // Arrange: Preparamos los datos de entrada
        User user = new User();
        user.setId(1L);
        user.setEmail("oldEmail@example.com");
        user.setPassword("encodedPassword");
        user.setRole(Role.ADMIN);

        String newEmail = "newEmail@example.com";

        // Simulamos que, tras la actualización, el repositorio no encuentra el usuario
        when(userRepository.getUserByEmail("newEmail@example.com"))
                .thenThrow(new RuntimeException("Usuario no encontrado"));

        // Act & Assert: Ejecutamos changePassword
        Exception exception = assertThrows(RuntimeException.class, () ->
                authUseCase.changeEmail(user, newEmail)
        );

        // Validamos el mensaje de la excepción
        assertEquals("Usuario no encontrado", exception.getMessage());

        // Verificamos que se haya invocado el método de el mock con un argumento que cumpla ciertas condiciones
        verify(userRepository).getUserByEmail("newEmail@example.com");
    }
}