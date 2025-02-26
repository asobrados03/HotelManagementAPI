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

/**
 * This class contains the attributes and methods for realize the unit tests
 * of the system administrators JDBC repository implementation.
 *
 * @author Alfredo Sobrados González
 */
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

    /**
     * Sets up the AdministratorJdbcRepository test environment.
     * <p>
     * This method injects the mocked jdbcTemplate and dataSource into the repository,
     * and uses ReflectionTestUtils to set the "insert" field to the mocked SimpleJdbcInsert.
     * </p>
     */
    @BeforeEach
    void setup() {
        // Construct the repository by injecting the mocked jdbcTemplate and dataSource.
        administratorRepository = new AdministratorJdbcRepository(jdbcTemplate, dataSource);
        // Inject the mocked SimpleJdbcInsert into the repository.
        ReflectionTestUtils.setField(administratorRepository, "insert", insert);
    }

    /**
     * Tests that {@code getAllAdministrators()} returns the expected list of administrators.
     * <p>
     * It stubs the jdbcTemplate query call with a SQL statement and an AdministratorMapper,
     * and verifies that the returned list is not null, has the expected size, and the first element's properties match.
     * </p>
     */
    @Test
    public void testGetAllAdministrators() {
        // Arrange
        List<Administrator> administratorList = Arrays.asList(
                new Administrator(1L, 5L, "Admin1"),
                new Administrator(2L, 6L, "Admin2")
        );

        String sql = "SELECT * FROM " + table;

        // Configure the stub using matchers to match the real call.
        when(jdbcTemplate.query(eq(sql), any(AdministratorJdbcRepository.AdministratorMapper.class)))
                .thenReturn(administratorList);

        // Act
        List<Administrator> listaObtenida = administratorRepository.getAllAdministrators();

        // Assert
        assertNotNull(listaObtenida, "The list of administrators should not be null");
        assertEquals(2, listaObtenida.size(), "There should be 2 administrators");

        // Validate the first element
        Administrator expected = administratorList.get(0);
        Administrator actual = listaObtenida.get(0);
        assertEquals(expected.id, actual.id, "The ID of the first administrator should match");
        assertEquals(expected.user_id, actual.user_id, "The user_id of the first administrator should match");
        assertEquals(expected.name, actual.name, "The name of the first administrator should match");
    }

    /**
     * Tests that {@code getAdministratorByUserId(String)} returns an administrator when found.
     * <p>
     * It simulates a repository call that finds an administrator for a given userId parameter (as a String),
     * then verifies that the returned Optional is present and its properties match the expected administrator.
     * </p>
     */
    @Test
    public void testGetAdministratorByUserIdString_found() {
        // Arrange
        String userIdParam = "5";
        Administrator expectedAdmin = new Administrator(1L, 5L, "Admin1");
        String sql = "SELECT * FROM " + table + " WHERE user_id = :userId";

        // Simulate that the administrator is found.
        when(jdbcTemplate.queryForObject(eq(sql), any(MapSqlParameterSource.class),
                any(AdministratorJdbcRepository.AdministratorMapper.class))).thenReturn(expectedAdmin);

        // Act
        Optional<Administrator> result = administratorRepository.getAdministratorByUserId(userIdParam);

        // Assert
        assertTrue(result.isPresent(), "The administrator should exist");
        Administrator admin = result.get();
        assertEquals(expectedAdmin.id, admin.id, "The ID should match");
        assertEquals(expectedAdmin.user_id, admin.user_id, "The user_id should match");
        assertEquals(expectedAdmin.name, admin.name, "The name should match");
    }

    /**
     * Tests that {@code getAdministratorByUserId(String)} returns an empty Optional when no administrator is found.
     * <p>
     * It simulates the jdbcTemplate throwing an EmptyResultDataAccessException, and verifies that the result is not present.
     * </p>
     */
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
        assertFalse(result.isPresent(), "The administrator should not be found");
    }

    /**
     * Tests that {@code getAdministratorById(long)} returns the expected administrator when found.
     * <p>
     * It stubs the jdbcTemplate call for a given id and verifies that the returned Optional contains the expected administrator.
     * </p>
     */
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
        assertTrue(result.isPresent(), "The administrator should exist");
        Administrator admin = result.get();
        assertEquals(expectedAdmin.id, admin.id, "The ID should match");
        assertEquals(expectedAdmin.user_id, admin.user_id, "The user_id should match");
        assertEquals(expectedAdmin.name, admin.name, "The name should match");
    }

    /**
     * Tests that {@code getAdministratorById(long)} returns an empty Optional when no administrator is found.
     * <p>
     * It simulates the jdbcTemplate throwing an EmptyResultDataAccessException, and verifies that the returned Optional is empty.
     * </p>
     */
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
        assertFalse(result.isPresent(), "The administrator should not be found");
    }

    /**
     * Tests that {@code createAdministrator(Administrator)} successfully creates an administrator.
     * <p>
     * It stubs the SimpleJdbcInsert to return a generated key, then verifies that the parameters passed to the insert
     * match the properties of the new administrator.
     * </p>
     */
    @Test
    public void testCreateAdministrator() {
        // Arrange
        Administrator newAdmin = new Administrator(0L, 5L, "AdminNew"); // ID is generated on insertion.
        when(insert.executeAndReturnKey(any(MapSqlParameterSource.class))).thenReturn(1L);

        // Act
        administratorRepository.createAdministrator(newAdmin);

        // Assert: Capture the parameters passed to the insert.
        ArgumentCaptor<MapSqlParameterSource> captor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(insert).executeAndReturnKey(captor.capture());
        MapSqlParameterSource actualParams = captor.getValue();
        assertEquals(newAdmin.name, actualParams.getValue("name"), "The name should match");
        assertEquals(newAdmin.user_id, actualParams.getValue("user_id"), "The user_id should match");
    }

    /**
     * Tests that {@code updateAdministrator(Administrator, long)} updates an administrator correctly.
     * <p>
     * It stubs the jdbcTemplate update method to return the number of rows updated, then verifies that the method returns
     * the expected value and that the correct parameters were passed.
     * </p>
     */
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
        assertEquals(1, rowsUpdated, "One row should be updated");

        // Verify the parameters sent in the update
        ArgumentCaptor<MapSqlParameterSource> captor = ArgumentCaptor.forClass(MapSqlParameterSource.class);
        verify(jdbcTemplate).update(eq(sql), captor.capture());
        MapSqlParameterSource actualParams = captor.getValue();
        assertEquals(adminToUpdate.name, actualParams.getValue("name"), "The name should match");
        assertEquals(userId, actualParams.getValue("userId"), "The userId should match");
    }

    /**
     * Tests that {@code getAdministratorByUserId(long)} returns the correct administrator when found.
     * <p>
     * It simulates the repository returning an administrator for a given userId (as long) and verifies that the Optional is present.
     * </p>
     */
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
        assertTrue(result.isPresent(), "The administrator should exist");
        Administrator admin = result.get();
        assertEquals(expectedAdmin.id, admin.id, "The ID should match");
        assertEquals(expectedAdmin.user_id, admin.user_id, "The user_id should match");
        assertEquals(expectedAdmin.name, admin.name, "The name should match");
    }

    /**
     * Tests that {@code getAdministratorByUserId(long)} returns an empty Optional when no administrator is found.
     * <p>
     * It simulates the repository throwing an EmptyResultDataAccessException and verifies that the result is empty.
     * </p>
     */
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
        assertFalse(result.isPresent(), "The administrator should not be found");
    }
}