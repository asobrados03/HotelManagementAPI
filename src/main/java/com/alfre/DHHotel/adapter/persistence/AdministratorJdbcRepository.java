package com.alfre.DHHotel.adapter.persistence;

import com.alfre.DHHotel.domain.model.Administrator;
import com.alfre.DHHotel.domain.repository.AdministratorRepository;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * This class contains the attributes and methods of the administrator repository in the adapter layer that access to
 * the database of the API and performs the operations relation to administrators
 *
 * @author Alfredo Sobrados González
 */
@Repository
public class AdministratorJdbcRepository implements AdministratorRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final SimpleJdbcInsert insert;

    private final AdministratorMapper mapper = new AdministratorMapper();

    private final String table = "Administrator";

    /**
     * Constructs an AdministratorJdbcRepository with the given NamedParameterJdbcTemplate and DataSource.
     *
     * @param namedParameterJdbcTemplate the NamedParameterJdbcTemplate to use for executing queries
     * @param dataSource the DataSource for obtaining database connections
     */
    public AdministratorJdbcRepository(NamedParameterJdbcTemplate namedParameterJdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = namedParameterJdbcTemplate;
        this.insert = new SimpleJdbcInsert(dataSource).withTableName(table)
                .usingGeneratedKeyColumns("id");
    }

    /**
     * Retrieves all administrators from the database.
     *
     * @return a list of all Administrator objects
     */
    @Override
    public List<Administrator> getAllAdministrators() {
        String sql = "SELECT * FROM " + table;
        return jdbcTemplate.query(sql, mapper);
    }

    /**
     * Retrieves an administrator by the given user ID (as a String).
     *
     * @param userId the user ID as a String
     * @return an Optional containing the Administrator if found, or an empty Optional if not found
     */
    @Override
    public Optional<Administrator> getAdministratorByUserId(String userId) {
        String sql = "SELECT * FROM " + table + " WHERE user_id = :userId";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("userId", userId);
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, params, mapper));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * Retrieves an administrator by its ID.
     *
     * @param id the identifier of the administrator
     * @return an Optional containing the Administrator if found, or an empty Optional if not found
     */
    @Override
    public Optional<Administrator> getAdministratorById(long id) {
        String sql = "SELECT * FROM " + table + " WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", id);
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, params, mapper));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * Creates a new administrator in the database.
     *
     * @param newAdministrator the Administrator object to be created
     */
    @Override
    public void createAdministrator(Administrator newAdministrator) {
        insert.executeAndReturnKey(
                new MapSqlParameterSource()
                        .addValue("name", newAdministrator.name)
                        .addValue("user_id", newAdministrator.user_id)
        );
    }

    /**
     * Updates the administrator information for the given user ID.
     *
     * @param administrator the Administrator object containing updated data
     * @param userId the user ID associated with the administrator to update
     * @return the number of rows affected by the update
     */
    @Override
    public int updateAdministrator(Administrator administrator, long userId) {
        String sql = "UPDATE " + table + " SET name = :name WHERE user_id = :userId ";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("name", administrator.name)
                .addValue("userId", userId);
        return jdbcTemplate.update(sql, params);
    }

    /**
     * Retrieves an administrator by the given user ID (as a long).
     *
     * @param userId the user ID as a long
     * @return an Optional containing the Administrator if found, or an empty Optional if not found
     */
    @Override
    public Optional<Administrator> getAdministratorByUserId(long userId) {
        String sql = "SELECT * FROM " + table + " WHERE user_id = :userId";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("userId", userId);
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, params, mapper));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * Deletes all administrator records from the database.
     */
    @Override
    public void deleteAll() {
        String sql = "DELETE FROM " + table;
        jdbcTemplate.update(sql, new MapSqlParameterSource());
    }

    /**
     * Maps rows of a ResultSet to Administrator objects.
     */
    public static class AdministratorMapper implements RowMapper<Administrator> {

        /**
         * Maps the current row of the given ResultSet to an Administrator object.
         *
         * @param rs the ResultSet to map (pre-initialized for the current row)
         * @param rowNum the number of the current row
         * @return the mapped Administrator object
         * @throws SQLException if an SQL error occurs while mapping the row
         */
        @Override
        public Administrator mapRow(ResultSet rs, int rowNum) throws SQLException {
            long id = rs.getLong("id");
            long userId = rs.getLong("user_id");
            String name = rs.getString("name");

            return new Administrator(id, userId, name);
        }
    }
}
