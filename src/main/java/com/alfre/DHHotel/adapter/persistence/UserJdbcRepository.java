package com.alfre.DHHotel.adapter.persistence;

import com.alfre.DHHotel.domain.model.Role;
import com.alfre.DHHotel.domain.model.User;
import com.alfre.DHHotel.domain.repository.UserRepository;
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
 * This class contains the attributes and methods of the user repository in the adapter layer that access to
 * the database of the API and performs the operations relation to users
 *
 * @author Alfredo Sobrados González
 */
@Repository
public class UserJdbcRepository implements UserRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final SimpleJdbcInsert insert;

    private final UserJdbcRepository.UserMapper mapper = new UserJdbcRepository.UserMapper();

    private final String table = "Users";

    /**
     * Constructs a UserJdbcRepository with the provided NamedParameterJdbcTemplate and DataSource.
     * This constructor initializes the jdbcTemplate and sets up a SimpleJdbcInsert for the user table.
     *
     * @param namedParameterJdbcTemplate the template for executing parameterized SQL queries
     * @param dataSource the DataSource for obtaining database connections
     */
    public UserJdbcRepository(NamedParameterJdbcTemplate namedParameterJdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = namedParameterJdbcTemplate;
        this.insert = new SimpleJdbcInsert(dataSource).withTableName(table)
                .usingGeneratedKeyColumns("id");
    }

    /**
     * Retrieves all users from the database.
     *
     * @return a list of all User objects
     */
    @Override
    public List<User> getAllUsers() {
        String sql = "SELECT * FROM " + table;
        return jdbcTemplate.query(sql, mapper);
    }

    /**
     * Retrieves a user by their email address.
     *
     * @param email the email address of the user
     * @return an Optional containing the User if found, or an empty Optional if not found
     */
    @Override
    public Optional<User> getUserByEmail(String email) {
        String sql = "SELECT * FROM " + table + " WHERE email = :email";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("email", email);
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, params, mapper));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * Creates a new user record in the database.
     *
     * @param newUser the User object to be created
     * @return the generated unique identifier for the newly created user
     */
    @Override
    public long createUser(User newUser) {
        return insert.executeAndReturnKey(
                new MapSqlParameterSource()
                        .addValue("email", newUser.email)
                        .addValue("password", newUser.password)
                        .addValue("role", newUser.role)
        ).longValue();
    }

    /**
     * Updates an existing user record in the database.
     *
     * @param user the User object containing updated values
     */
    @Override
    public void updateUser(User user) {
        String sql = "UPDATE " + table + " SET email = :email, password = :password, role = :role WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", user.id)
                .addValue("email", user.email)
                .addValue("password", user.password)
                .addValue("role", user.role.name());
        jdbcTemplate.update(sql, params);
    }

    /**
     * Deletes a user record from the database by its unique identifier.
     *
     * @param id the unique identifier of the user to be deleted
     * @return the number of rows affected by the delete operation
     */
    @Override
    public int deleteUser(long id) {
        String sql = "DELETE FROM " + table + " WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", id);
        return jdbcTemplate.update(sql, params);
    }

    /**
     * Deletes all user records from the database.
     */
    @Override
    public void deleteAll() {
        String sql = "DELETE FROM " + table;
        jdbcTemplate.update(sql, new MapSqlParameterSource());
    }

    /**
     * Retrieves a user by their unique identifier.
     *
     * @param id the unique identifier of the user
     * @return an Optional containing the User if found, or an empty Optional if not found
     */
    @Override
    public Optional<User> getUserById(long id) {
        String sql = "SELECT * FROM " + table + " WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", id);
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, params, mapper));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * Maps rows of a SQL ResultSet to User objects.
     */
    public static class UserMapper implements RowMapper<User> {
        /**
         * Maps the current row of the given ResultSet to a User object.
         *
         * @param rs the ResultSet to map (pre-initialized for the current row)
         * @param rowNum the number of the current row
         * @return the mapped User object
         * @throws SQLException if an SQL error occurs while mapping the row
         */
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            long id = rs.getLong("id");
            String email = rs.getString("email");
            String password = rs.getString("password");
            Role role = Role.valueOf(rs.getString("role").toUpperCase());
            return new User(id, email, password, role);
        }
    }
}