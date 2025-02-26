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

/**
 * This class contains the attributes and methods for realize the unit tests
 * of the customers operations JDBC repository implementation.
 *
 * @author Alfredo Sobrados González
 */
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

    /**
     * Initializes the ClientJdbcRepository with mocked jdbcTemplate and dataSource.
     * <p>
     * This method injects the mocked jdbcTemplate and dataSource into the ClientJdbcRepository,
     * and then replaces the internally created SimpleJdbcInsert instance with a mock using ReflectionTestUtils.
     * </p>
     */
    @BeforeEach
    void setup() {
        clientRepository = new ClientJdbcRepository(jdbcTemplate, dataSource);
        ReflectionTestUtils.setField(clientRepository, "insert", insert);
    }

    /**
     * Tests that getAllClients() returns a non-null list of clients with the expected number of clients and
     * correct properties.
     */
    @Test
    public void testGetAllClients() {
        // Arrange: Prepare a list of two clients.
        List<Client> clientList = Arrays.asList(
                new Client(1L, 101L, "John", "Doe", "123456"),
                new Client(2L, 102L, "Jane", "Smith", "654321")
        );
        String sql = "SELECT * FROM " + table;

        // Stub the jdbcTemplate query to return the client list.
        when(jdbcTemplate.query(eq(sql), any(ClientJdbcRepository.ClientMapper.class))).thenReturn(clientList);

        // Act: Invoke getAllClients().
        List<Client> result = clientRepository.getAllClients();

        // Assert: Verify that the result is not null, has two elements, and the properties of the first client match.
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

    /**
     * Tests that getClientById(long) returns the expected client when found.
     */
    @Test
    public void testGetClientById_found() {
        // Arrange: Create a sample client with a specific id.
        long id = 1L;
        Client expectedClient = new Client(id, 101L, "John", "Doe", "123456");
        String sql = "SELECT * FROM " + table + " WHERE id = :id";

        // Stub the jdbcTemplate queryForObject to return the expected client.
        when(jdbcTemplate.queryForObject(eq(sql), any(MapSqlParameterSource.class),
                any(ClientJdbcRepository.ClientMapper.class))).thenReturn(expectedClient);

        // Act: Invoke getClientById().
        Optional<Client> result = clientRepository.getClientById(id);

        // Assert: Verify that the result is present and its properties match the expected client.
        assertTrue(result.isPresent(), "El cliente debe existir");
        Client actual = result.get();
        assertEquals(expectedClient.id, actual.id, "El ID debe coincidir");
        assertEquals(expectedClient.user_id, actual.user_id, "El user_id debe coincidir");
        assertEquals(expectedClient.first_name, actual.first_name, "El first_name debe coincidir");
        assertEquals(expectedClient.last_name, actual.last_name, "El last_name debe coincidir");
        assertEquals(expectedClient.phone, actual.phone, "El phone debe coincidir");
    }

    /**
     * Tests that getClientById(long) returns an empty Optional when the client is not found.
     */
    @Test
    public void testGetClientById_notFound() {
        // Arrange: Define a non-existent id.
        long id = 999L;
        String sql = "SELECT * FROM " + table + " WHERE id = :id";
        when(jdbcTemplate.queryForObject(eq(sql), any(MapSqlParameterSource.class),
                any(ClientJdbcRepository.ClientMapper.class)))
                .thenThrow(new EmptyResultDataAccessException(1));

        // Act: Invoke getClientById().
        Optional<Client> result = clientRepository.getClientById(id);

        // Assert: Verify that the Optional is empty.
        assertFalse(result.isPresent(), "No se debe encontrar el cliente");
    }

    /**
     * Tests that getClientByUserId(long) returns the expected client when found.
     */
    @Test
    public void testGetClientByUserId_found() {
        // Arrange: Set a user_id and create a client associated with that user_id.
        long userId = 101L;
        Client expectedClient = new Client(1L, userId, "John", "Doe", "123456");
        String sql = "SELECT * FROM " + table + " WHERE user_id = :userId";

        // Stub the jdbcTemplate queryForObject to return the expected client.
        when(jdbcTemplate.queryForObject(eq(sql), any(MapSqlParameterSource.class),
                any(ClientJdbcRepository.ClientMapper.class))).thenReturn(expectedClient);

        // Act: Invoke getClientByUserId().
        Optional<Client> result = clientRepository.getClientByUserId(userId);

        // Assert: Verify that the Optional contains the expected client.
        assertTrue(result.isPresent(), "El cliente debe existir");
        Client actual = result.get();
        assertEquals(expectedClient.id, actual.id, "El ID debe coincidir");
        assertEquals(expectedClient.user_id, actual.user_id, "El user_id debe coincidir");
        assertEquals(expectedClient.first_name, actual.first_name, "El first_name debe coincidir");
        assertEquals(expectedClient.last_name, actual.last_name, "El last_name debe coincidir");
        assertEquals(expectedClient.phone, actual.phone, "El phone debe coincidir");
    }

    /**
     * Tests that getClientByUserId(long) returns an empty Optional when the client is not found.
     */
    @Test
    public void testGetClientByUserId_notFound() {
        // Arrange: Use a non-existent user_id.
        long userId = 999L;
        String sql = "SELECT * FROM " + table + " WHERE user_id = :userId";
        when(jdbcTemplate.queryForObject(eq(sql), any(MapSqlParameterSource.class),
                any(ClientJdbcRepository.ClientMapper.class)))
                .thenThrow(new EmptyResultDataAccessException(1));

        // Act: Invoke getClientByUserId().
        Optional<Client> result = clientRepository.getClientByUserId(userId);

        // Assert: Verify that the Optional is empty.
        assertFalse(result.isPresent(), "No se debe encontrar el cliente");
    }

    /**
     * Tests that createClient(Client) correctly inserts a new client and sets the proper parameters.
     * <p>
     * The test stubs the SimpleJdbcInsert's executeAndReturnKey method to return a generated id,
     * and then captures the parameters passed to it to ensure they match the new client's properties.
     * </p>
     */
    @Test
    public void testCreateClient() {
        // Arrange: Create a new client with initial properties.
        Client newClient = new Client(0L, 101L, "John", "Doe", "123456");
        when(insert.executeAndReturnKey(any(MapSqlParameterSource.class))).thenReturn(1L);

        // Act: Invoke createClient().
        clientRepository.createClient(newClient);

        // Assert: Capture and verify the parameters passed to the insert.
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

    /**
     * Tests that updateClient(Client) updates an existing client and returns the updated client.
     * <p>
     * The test stubs the jdbcTemplate update method to return 1, captures the parameters passed to the update,
     * and asserts that they match the client's updated properties.
     * </p>
     */
    @Test
    public void testUpdateClient() {
        // Arrange: Create a client with initial values and then modify its properties.
        Client clientToUpdate = new Client(1L, 101L, "John", "Doe", "123456");
        clientToUpdate.first_name = "Johnny";
        clientToUpdate.last_name = "Doey";
        clientToUpdate.phone = "654321";

        String sql = "UPDATE " + table + " SET first_name = :first_name, last_name = :last_name, phone = :phone" +
                " WHERE id = :id";
        when(jdbcTemplate.update(eq(sql), any(MapSqlParameterSource.class))).thenReturn(1);

        // Act: Invoke updateClient().
        Client updatedClient = clientRepository.updateClient(clientToUpdate);

        // Assert: Capture and verify the update parameters.
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

        // Verify that the method returns the same updated client.
        assertEquals(clientToUpdate, updatedClient, "El cliente actualizado debe ser retornado");
    }
}