package com.alfre.DHHotel.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;

import java.util.Optional;

import com.alfre.DHHotel.adapter.persistence.AdministratorJdbcRepository;
import com.alfre.DHHotel.domain.model.Administrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;

@ExtendWith(MockitoExtension.class)
public class AdministratorJdbcRepositoryTest {

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Mock
    private DataSource dataSource;

    @Mock
    private SimpleJdbcInsert insert;

    private AdministratorJdbcRepository administratorRepository;

    private final String table = "Administrator";

    @BeforeEach
    void setUp() {
        // Construimos el repositorio inyectando el jdbcTemplate y el dataSource mockeados.
        administratorRepository = new AdministratorJdbcRepository(jdbcTemplate, dataSource);
        // Inyectamos nuestro mock de SimpleJdbcInsert en el repositorio (ya que en el constructor se crea uno nuevo)
        ReflectionTestUtils.setField(administratorRepository, "insert", insert);
    }

    @Test
    public void testGetAllAdministrators() {
        // Arrange
        List<Administrator> administratorList = Arrays.asList(
                new Administrator(1L, 5L, "Admin1"),
                new Administrator(2L, 6L, "Admin2")
        );

        String sql = "SELECT * FROM " + table;

        // Configuramos el stub usando matchers para que coincida con la llamada real.
        when(jdbcTemplate.query(eq(sql), any(AdministratorJdbcRepository.AdministratorMapper.class)))
        .thenReturn(administratorList);

        // Act
        List<Administrator> listaObtenida = administratorRepository.getAllAdministrators();

        // Assert
        assertNotNull(listaObtenida, "La lista de administradores no debe ser nula");
        assertEquals(2, listaObtenida.size(), "Debe haber 2 administradores");

        // Validamos el primer elemento
        Administrator expected = administratorList.getFirst();
        Administrator actual = listaObtenida.getFirst();
        assertEquals(expected.id, actual.id, "El ID del primer administrador debe coincidir");
        assertEquals(expected.user_id, actual.user_id, "El user_id del primer administrador debe coincidir");
        assertEquals(expected.name, actual.name, "El nombre del primer administrador debe coincidir");
    }

    @Test
    public void testGetAdministratorByUserIdString_found() {
        // Arrange
        String userIdParam = "5";
        Administrator expectedAdmin = new Administrator(1L, 5L, "Admin1");
        String sql = "SELECT * FROM " + table + " WHERE user_id = :userId";

        // Simulamos que se encuentra el administrador
        when(jdbcTemplate.queryForObject(eq(sql), any(MapSqlParameterSource.class),
                any(AdministratorJdbcRepository.AdministratorMapper.class))).thenReturn(expectedAdmin);

        // Act
        Optional<Administrator> result = administratorRepository.getAdministratorByUserId(userIdParam);

        // Assert
        assertTrue(result.isPresent(), "El administrador debe existir");
        Administrator admin = result.get();
        assertEquals(expectedAdmin.id, admin.id, "El ID debe coincidir");
        assertEquals(expectedAdmin.user_id, admin.user_id, "El user_id debe coincidir");
        assertEquals(expectedAdmin.name, admin.name, "El nombre debe coincidir");
    }

    @Test
    public void testGetAdministratorByUserIdString_notFound() {
        // Arrange
        String userIdParam = "nonExisting";
        String sql = "SELECT * FROM " + table + " WHERE user_id = :userId";
        when(jdbcTemplate.queryForObject(eq(sql), any(MapSqlParameterSource.class),
                any(AdministratorJdbcRepository.AdministratorMapper.class)))
                .thenThrow(new EmptyResultDataAccessException(1));

        // Act
        Optional<Administrator> result = administratorRepository.getAdministratorByUserId(userIdParam);

        // Assert
        assertFalse(result.isPresent(), "No se debe encontrar el administrador");
    }

    @Test
    public void testGetAdministratorById_found() {
        // Arrange
        long id = 1L;
        Administrator expectedAdmin = new Administrator(id, 5L, "Admin1");
        String sql = "SELECT * FROM " + table + " WHERE id = :id";
        when(jdbcTemplate.queryForObject(eq(sql), any(MapSqlParameterSource.class),
                any(AdministratorJdbcRepository.AdministratorMapper.class))).thenReturn(expectedAdmin);

        // Act
        Optional<Administrator> result = administratorRepository.getAdministratorById(id);

        // Assert
        assertTrue(result.isPresent(), "El administrador debe existir");
        Administrator admin = result.get();
        assertEquals(expectedAdmin.id, admin.id, "El ID debe coincidir");
        assertEquals(expectedAdmin.user_id, admin.user_id, "El user_id debe coincidir");
        assertEquals(expectedAdmin.name, admin.name, "El nombre debe coincidir");
    }

    @Test
    public void testGetAdministratorById_notFound() {
        // Arrange
        long id = 100L;
        String sql = "SELECT * FROM " + table + " WHERE id = :id";
        when(jdbcTemplate.queryForObject(eq(sql), any(MapSqlParameterSource.class),
                any(AdministratorJdbcRepository.AdministratorMapper.class)))
                .thenThrow(new EmptyResultDataAccessException(1));

        // Act
        Optional<Administrator> result = administratorRepository.getAdministratorById(id);

        // Assert
        assertFalse(result.isPresent(), "No se debe encontrar el administrador");
    }

    @Test
    public void testCreateAdministrator() {
        // Arrange
        Administrator newAdmin = new Administrator(0L, 5L, "AdminNew"); // id se genera en la inserción
        when(insert.executeAndReturnKey(any(MapSqlParameterSource.class))).thenReturn(1L);

        // Act
        administratorRepository.createAdministrator(newAdmin);

        // Assert: capturamos los parámetros pasados al insert
        ArgumentCaptor<MapSqlParameterSource> captor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(insert).executeAndReturnKey(captor.capture());
        MapSqlParameterSource actualParams = captor.getValue();
        assertEquals(newAdmin.name, actualParams.getValue("name"), "El nombre debe coincidir");
        assertEquals(newAdmin.user_id, actualParams.getValue("user_id"), "El user_id debe coincidir");
    }

    @Test
    public void testUpdateAdministrator() {
        // Arrange
        Administrator adminToUpdate = new Administrator(1L, 5L, "AdminUpdated");
        long userId = 5L;
        String sql = "UPDATE " + table + " SET name = :name WHERE user_id = :userId ";
        when(jdbcTemplate.update(eq(sql), any(MapSqlParameterSource.class))).thenReturn(1);

        // Act
        int rowsUpdated = administratorRepository.updateAdministrator(adminToUpdate, userId);

        // Assert
        assertEquals(1, rowsUpdated, "Se debe actualizar 1 fila");

        // Verificamos los parámetros enviados en la actualización
        ArgumentCaptor<MapSqlParameterSource> captor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbcTemplate).update(eq(sql), captor.capture());
        MapSqlParameterSource actualParams = captor.getValue();
        assertEquals(adminToUpdate.name, actualParams.getValue("name"), "El nombre debe coincidir");
        assertEquals(userId, actualParams.getValue("userId"), "El userId debe coincidir");
    }

    @Test
    public void testGetAdministratorByUserIdLong_found() {
        // Arrange
        long userId = 5L;
        Administrator expectedAdmin = new Administrator(1L, userId, "Admin1");
        String sql = "SELECT * FROM " + table + " WHERE user_id = :userId";
        when(jdbcTemplate.queryForObject(eq(sql), any(MapSqlParameterSource.class),
                any(AdministratorJdbcRepository.AdministratorMapper.class))).thenReturn(expectedAdmin);

        // Act
        Optional<Administrator> result = administratorRepository.getAdministratorByUserId(userId);

        // Assert
        assertTrue(result.isPresent(), "El administrador debe existir");
        Administrator admin = result.get();
        assertEquals(expectedAdmin.id, admin.id, "El ID debe coincidir");
        assertEquals(expectedAdmin.user_id, admin.user_id, "El user_id debe coincidir");
        assertEquals(expectedAdmin.name, admin.name, "El nombre debe coincidir");
    }

    @Test
    public void testGetAdministratorByUserIdLong_notFound() {
        // Arrange
        long userId = 999L;
        String sql = "SELECT * FROM " + table + " WHERE user_id = :userId";
        when(jdbcTemplate.queryForObject(eq(sql), any(MapSqlParameterSource.class),
                any(AdministratorJdbcRepository.AdministratorMapper.class)))
                .thenThrow(new EmptyResultDataAccessException(1));

        // Act
        Optional<Administrator> result = administratorRepository.getAdministratorByUserId(userId);

        // Assert
        assertFalse(result.isPresent(), "No se debe encontrar el administrador");
    }
}