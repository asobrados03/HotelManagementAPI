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

@ExtendWith(MockitoExtension.class)
public class AdministratorUseCaseTest {
    @Mock
    private AdministratorRepository administratorRepository;
    @Mock
    private UserRepository userRepository;

    private AdministratorUseCase administratorUseCase;

    @BeforeEach
    public void setup() {
        administratorUseCase = new AdministratorUseCase(administratorRepository, userRepository);
    }

    @Test
    public void getAllAdministrators_success() {
        // Arrange: Preparamos una lista de administradores
        List<Administrator> adminList = new ArrayList<>();
        Administrator admin = new Administrator();
        // Configuramos las propiedades de admin si es necesario
        adminList.add(admin);
        when(administratorRepository.getAllAdministrators()).thenReturn(adminList);

        // Act: Ejecutamos el método
        List<Administrator> result = administratorUseCase.getAllAdministrators();

        // Assert: Verificamos que el resultado sea el esperado y se llamó al método del repository
        assertEquals(adminList, result);
        verify(administratorRepository).getAllAdministrators();
    }

    @Test
    public void getAdministratorByUserId_success() {
        // Arrange: Creamos un administrador y simulamos su retorno por el repository
        Administrator admin = new Administrator();
        // Configuramos las propiedades de admin si es necesario
        when(administratorRepository.getAdministratorByUserId(1L)).thenReturn(Optional.of(admin));

        // Act
        Optional<Administrator> result = administratorUseCase.getAdministratorByUserId(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(admin, result.get());
        verify(administratorRepository).getAdministratorByUserId(1L);
    }

    @Test
    public void getAdministratorById_success() {
        // Arrange: Creamos un administrador y simulamos su retorno para el id dado
        Administrator admin = new Administrator();
        when(administratorRepository.getAdministratorById(10L)).thenReturn(Optional.of(admin));

        // Act
        Optional<Administrator> result = administratorUseCase.getAdministratorById(10L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(admin, result.get());
        verify(administratorRepository).getAdministratorById(10L);
    }

    @Test
    public void updateAdministrator_success() {
        // Arrange: Creamos un administrador y simulamos el retorno del update
        Administrator admin = new Administrator();
        // Configuramos las propiedades de admin si es necesario
        when(administratorRepository.updateAdministrator(admin, 1L)).thenReturn(1);

        // Act
        int result = administratorUseCase.updateAdministrator(admin, 1L);

        // Assert
        assertEquals(1, result);
        verify(administratorRepository).updateAdministrator(admin, 1L);
    }

    @Test
    public void deleteAdministrator_success() {
        // Arrange: Creamos un administrador y simulamos el retorno del delete
        Administrator admin = new Administrator();
        admin.setUser_id(5L);

        // Stubs
        when(userRepository.deleteUser(admin.user_id)).thenReturn(1);
        when(administratorRepository.getAdministratorById(5L)).thenReturn(Optional.of(admin));

        // Act
        int result  = administratorUseCase.deleteAdministrator(5L);

        // Assert
        assertEquals(1, result);
        verify(userRepository).deleteUser(5L);
    }

    @Test
    public void deleteAdministrator_failure_shouldThrowException() {
        // Arrange: Configuramos el comportamiento del repository para simular que no existe el administrador.
        when(administratorRepository.getAdministratorById(7L)).thenReturn(Optional.empty());

        // Act
        Exception exception = assertThrows(RuntimeException.class, () ->
                administratorUseCase.deleteAdministrator(7L)
        );

        // Assert
        assertEquals("No existe el administrador que quieres eliminar", exception.getMessage());
        verify(administratorRepository).getAdministratorById(7L);
    }
}
