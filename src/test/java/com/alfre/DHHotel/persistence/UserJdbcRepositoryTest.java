package com.alfre.DHHotel.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import javax.sql.DataSource;

import com.alfre.DHHotel.domain.model.User;
import com.alfre.DHHotel.domain.model.Role;
import com.alfre.DHHotel.adapter.persistence.UserJdbcRepository;
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

/**
 * This class contains the attributes and methods for realize the unit tests
 * of the user operations JDBC repository implementation.
 *
 * @author Alfredo Sobrados González
 */
@ExtendWith(MockitoExtension.class)
public class UserJdbcRepositoryTest {

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Mock
    private DataSource dataSource;

    @Mock
    private SimpleJdbcInsert insert;

    private UserJdbcRepository userRepository;
    private final String table = "Users";

    /**
     * Sets up the UserJdbcRepository instance before each test.
     * <p>
     * This method instantiates the repository by injecting the mocked jdbcTemplate and dataSource,
     * and then replaces the internal SimpleJdbcInsert instance with a mock using ReflectionTestUtils.
     * </p>
     */
    @BeforeEach
    void setup() {
        // Inject the mocks into the repository constructor.
        userRepository = new UserJdbcRepository(jdbcTemplate, dataSource);
        // Replace the internal SimpleJdbcInsert instance with the mock.
        ReflectionTestUtils.setField(userRepository, "insert", insert);
    }

    /**
     * Tests that getUserByEmail returns a User when the user is found in the database.
     * <p>
     * The test stubs the jdbcTemplate queryForObject method to return a dummy User and then verifies
     * that the returned Optional contains the expected User.
     * </p>
     */
    @Test
    void testGetUserByEmail_found() {
        // Arrange
        String email = "user@example.com";
        Role role = Role.CLIENT;
        User user = new User(1L, email, "password123", role);
        String sql = "SELECT * FROM " + table + " WHERE email = :email";
        when(jdbcTemplate.queryForObject(eq(sql), any(MapSqlParameterSource.class),
                any(UserJdbcRepository.UserMapper.class)))
                .thenReturn(user);

        // Act
        Optional<User> result = userRepository.getUserByEmail(email);

        // Assert
        assertTrue(result.isPresent(), "El usuario debe existir");
        assertEquals(user, result.get(), "El usuario retornado debe coincidir");
    }

    /**
     * Tests that getUserByEmail returns an empty Optional when the user is not found.
     * <p>
     * The test stubs the jdbcTemplate queryForObject method to throw an EmptyResultDataAccessException,
     * and then verifies that the returned Optional is empty.
     * </p>
     */
    @Test
    void testGetUserByEmail_notFound() {
        // Arrange
        String email = "nonexistent@example.com";
        String sql = "SELECT * FROM " + table + " WHERE email = :email";
        when(jdbcTemplate.queryForObject(eq(sql), any(MapSqlParameterSource.class),
                any(UserJdbcRepository.UserMapper.class)))
                .thenThrow(new EmptyResultDataAccessException(1));

        // Act
        Optional<User> result = userRepository.getUserByEmail(email);

        // Assert
        assertFalse(result.isPresent(), "No se debe encontrar el usuario");
    }

    /**
     * Tests that createUser correctly inserts a new user and returns the generated ID.
     * <p>
     * The test stubs the SimpleJdbcInsert's executeAndReturnKey method to return a dummy ID,
     * and verifies that the parameters passed to the insert match the new user's properties.
     * </p>
     */
    @Test
    void testCreateUser() {
        // Arrange
        Role role = Role.ADMIN;
        User newUser = new User(0L, "newuser@example.com", "newpassword", role);
        when(insert.executeAndReturnKey(any(MapSqlParameterSource.class))).thenReturn(1L);

        // Act
        long generatedId = userRepository.createUser(newUser);

        // Assert
        assertEquals(1L, generatedId, "El ID generado debe ser 1");
        ArgumentCaptor<MapSqlParameterSource> captor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(insert).executeAndReturnKey(captor.capture());
        MapSqlParameterSource params = captor.getValue();
        assertEquals(newUser.email, params.getValue("email"), "El email debe coincidir");
        assertEquals(newUser.password, params.getValue("password"), "El password debe coincidir");
        assertEquals(newUser.role, params.getValue("role"), "El role debe coincidir");
    }

    /**
     * Tests that updateUser correctly updates an existing user.
     * <p>
     * The test stubs the jdbcTemplate update method to return 1 (indicating one row updated)
     * and verifies that the correct SQL parameters were passed to the update.
     * </p>
     */
    @Test
    void testUpdateUser() {
        // Arrange
        Role role = Role.ADMIN;
        User user = new User(1L, "updated@example.com", "updatedPass", role);
        String sql = "UPDATE " + table + " SET email = :email, password = :password, role = :role WHERE id = :id";
        when(jdbcTemplate.update(eq(sql), any(MapSqlParameterSource.class))).thenReturn(1);

        // Act
        userRepository.updateUser(user);

        // Assert
        ArgumentCaptor<MapSqlParameterSource> captor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbcTemplate).update(eq(sql), captor.capture());
        MapSqlParameterSource params = captor.getValue();
        assertEquals(user.id, params.getValue("id"), "El id debe coincidir");
        assertEquals(user.email, params.getValue("email"), "El email debe coincidir");
        assertEquals(user.password, params.getValue("password"), "El password debe coincidir");
        assertEquals(user.role.name(), params.getValue("role"), "El role debe coincidir");
    }

    /**
     * Tests that deleteUser correctly deletes a user and returns the number of rows affected.
     * <p>
     * The test stubs the jdbcTemplate update method for the DELETE SQL and verifies that the expected number of rows
     * is deleted, as well as the correct SQL parameters.
     * </p>
     */
    @Test
    void testDeleteUser() {
        // Arrange
        long id = 1L;
        String sql = "DELETE FROM " + table + " WHERE id = :id";
        when(jdbcTemplate.update(eq(sql), any(MapSqlParameterSource.class))).thenReturn(1);

        // Act
        int rows = userRepository.deleteUser(id);

        // Assert
        assertEquals(1, rows, "Se debe eliminar 1 fila");
        ArgumentCaptor<MapSqlParameterSource> captor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbcTemplate).update(eq(sql), captor.capture());
        MapSqlParameterSource params = captor.getValue();
        assertEquals(id, params.getValue("id"), "El id debe coincidir");
    }
}