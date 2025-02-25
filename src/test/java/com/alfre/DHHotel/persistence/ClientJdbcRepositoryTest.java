package com.alfre.DHHotel.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.alfre.DHHotel.domain.model.Client;
import com.alfre.DHHotel.adapter.persistence.ClientJdbcRepository;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class ClientJdbcRepositoryTest {

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Mock
    private DataSource dataSource;

    @Mock
    private SimpleJdbcInsert insert;

    private ClientJdbcRepository clientRepository;

    private final String table = "Client";

    @BeforeEach
    void setup() {
        // Se inyectan el jdbcTemplate y dataSource mockeados en el constructor.
        clientRepository = new ClientJdbcRepository(jdbcTemplate, dataSource);
        // Reemplazamos la instancia de SimpleJdbcInsert que se creó en el constructor por nuestro mock.
        ReflectionTestUtils.setField(clientRepository, "insert", insert);
    }

    @Test
    public void testGetAllClients() {
        // Arrange
        List<Client> clientList = Arrays.asList(
                new Client(1L, 101L, "John", "Doe", "123456"),
                new Client(2L, 102L, "Jane", "Smith", "654321")
        );
        String sql = "SELECT * FROM " + table;

        when(jdbcTemplate.query(eq(sql), any(ClientJdbcRepository.ClientMapper.class))).thenReturn(clientList);

        // Act
        List<Client> result = clientRepository.getAllClients();

        // Assert
        assertNotNull(result, "La lista de clientes no debe ser nula");
        assertEquals(2, result.size(), "Debe haber 2 clientes");

        Client expected = clientList.getFirst();
        Client actual = result.getFirst();

        assertEquals(expected.id, actual.id, "El ID debe coincidir");
        assertEquals(expected.user_id, actual.user_id, "El user_id debe coincidir");
        assertEquals(expected.first_name, actual.first_name, "El first_name debe coincidir");
        assertEquals(expected.last_name, actual.last_name, "El last_name debe coincidir");
        assertEquals(expected.phone, actual.phone, "El phone debe coincidir");
    }

    @Test
    public void testGetClientById_found() {
        // Arrange
        long id = 1L;
        Client expectedClient = new Client(id, 101L, "John", "Doe", "123456");

        String sql = "SELECT * FROM " + table + " WHERE id = :id";

        when(jdbcTemplate.queryForObject(eq(sql), any(MapSqlParameterSource.class),
                any(ClientJdbcRepository.ClientMapper.class)))
                .thenReturn(expectedClient);

        // Act
        Optional<Client> result = clientRepository.getClientById(id);

        // Assert
        assertTrue(result.isPresent(), "El cliente debe existir");

        Client actual = result.get();

        assertEquals(expectedClient.id, actual.id, "El ID debe coincidir");
        assertEquals(expectedClient.user_id, actual.user_id, "El user_id debe coincidir");
        assertEquals(expectedClient.first_name, actual.first_name, "El first_name debe coincidir");
        assertEquals(expectedClient.last_name, actual.last_name, "El last_name debe coincidir");
        assertEquals(expectedClient.phone, actual.phone, "El phone debe coincidir");
    }

    @Test
    public void testGetClientById_notFound() {
        // Arrange
        long id = 999L;

        String sql = "SELECT * FROM " + table + " WHERE id = :id";

        when(jdbcTemplate.queryForObject(eq(sql), any(MapSqlParameterSource.class),
                any(ClientJdbcRepository.ClientMapper.class)))
                .thenThrow(new EmptyResultDataAccessException(1));

        // Act
        Optional<Client> result = clientRepository.getClientById(id);

        // Assert
        assertFalse(result.isPresent(), "No se debe encontrar el cliente");
    }

    @Test
    public void testGetClientByUserId_found() {
        // Arrange
        long userId = 101L;

        Client expectedClient = new Client(1L, userId, "John", "Doe", "123456");

        String sql = "SELECT * FROM " + table + " WHERE user_id = :userId";

        when(jdbcTemplate.queryForObject(eq(sql), any(MapSqlParameterSource.class),
                any(ClientJdbcRepository.ClientMapper.class)))
                .thenReturn(expectedClient);

        // Act
        Optional<Client> result = clientRepository.getClientByUserId(userId);

        // Assert
        assertTrue(result.isPresent(), "El cliente debe existir");

        Client actual = result.get();

        assertEquals(expectedClient.id, actual.id, "El ID debe coincidir");
        assertEquals(expectedClient.user_id, actual.user_id, "El user_id debe coincidir");
        assertEquals(expectedClient.first_name, actual.first_name, "El first_name debe coincidir");
        assertEquals(expectedClient.last_name, actual.last_name, "El last_name debe coincidir");
        assertEquals(expectedClient.phone, actual.phone, "El phone debe coincidir");
    }

    @Test
    public void testGetClientByUserId_notFound() {
        // Arrange
        long userId = 999L;

        String sql = "SELECT * FROM " + table + " WHERE user_id = :userId";

        when(jdbcTemplate.queryForObject(eq(sql), any(MapSqlParameterSource.class),
                any(ClientJdbcRepository.ClientMapper.class))).thenThrow(new EmptyResultDataAccessException(1));

        // Act
        Optional<Client> result = clientRepository.getClientByUserId(userId);

        // Assert
        assertFalse(result.isPresent(), "No se debe encontrar el cliente");
    }

    @Test
    public void testCreateClient() {
        // Arrange
        Client newClient = new Client(0L, 101L, "John", "Doe", "123456");

        when(insert.executeAndReturnKey(any(MapSqlParameterSource.class))).thenReturn(1L);

        // Act
        clientRepository.createClient(newClient);

        // Assert: Capturamos los parámetros enviados al insert.
        ArgumentCaptor<MapSqlParameterSource> captor = ArgumentCaptor.forClass(MapSqlParameterSource.class);

        verify(insert).executeAndReturnKey(captor.capture());

        MapSqlParameterSource actualParams = captor.getValue();

        assertEquals(newClient.user_id, actualParams.getValue("user_id"), "El user_id debe coincidir");
        assertEquals(newClient.first_name, actualParams.getValue("first_name"),
                "El first_name debe coincidir");
        assertEquals(newClient.last_name, actualParams.getValue("last_name"),
                "El last_name debe coincidir");
        assertEquals(newClient.phone, actualParams.getValue("phone"), "El phone debe coincidir");
    }

    @Test
    public void testUpdateClient() {
        // Arrange
        Client clientToUpdate = new Client(1L, 101L, "John", "Doe", "123456");
        // Modificamos algunos datos
        clientToUpdate.first_name = "Johnny";
        clientToUpdate.last_name = "Doey";
        clientToUpdate.phone = "654321";

        String sql = "UPDATE " + table + " SET first_name = :first_name, last_name = :last_name, phone = :phone" +
                " WHERE id = :id";

        when(jdbcTemplate.update(eq(sql), any(MapSqlParameterSource.class))).thenReturn(1);

        // Act
        Client updatedClient = clientRepository.updateClient(clientToUpdate);

        // Assert: Capturamos los parámetros enviados a la actualización.
        ArgumentCaptor<MapSqlParameterSource> captor = ArgumentCaptor.forClass(MapSqlParameterSource.class);

        verify(jdbcTemplate).update(eq(sql), captor.capture());

        MapSqlParameterSource actualParams = captor.getValue();

        assertEquals(clientToUpdate.id, actualParams.getValue("id"), "El ID debe coincidir");
        assertEquals(clientToUpdate.user_id, actualParams.getValue("user_id"),
                "El user_id debe coincidir");
        assertEquals(clientToUpdate.first_name, actualParams.getValue("first_name"),
                "El first_name debe coincidir");
        assertEquals(clientToUpdate.last_name, actualParams.getValue("last_name"),
                "El last_name debe coincidir");
        assertEquals(clientToUpdate.phone, actualParams.getValue("phone"), "El phone debe coincidir");

        // Además, se verifica que se retorne el mismo cliente actualizado.
        assertEquals(clientToUpdate, updatedClient, "El cliente actualizado debe ser retornado");
    }
}