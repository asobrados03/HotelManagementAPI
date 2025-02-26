package com.alfre.DHHotel.adapter.persistence;

import com.alfre.DHHotel.domain.model.Client;
import com.alfre.DHHotel.domain.repository.ClientRepository;
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
 * This class contains the attributes and methods of the client repository in the adapter layer that access to
 * the database of the API and performs the operations relation to clients
 *
 * @author Alfredo Sobrados González
 */
@Repository
public class ClientJdbcRepository implements ClientRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final SimpleJdbcInsert insert;

    private final ClientJdbcRepository.ClientMapper mapper = new ClientMapper();

    private final String table = "Client";

    /**
     * Constructs a ClientJdbcRepository with the provided NamedParameterJdbcTemplate and DataSource.
     * This constructor initializes the jdbcTemplate and configures a SimpleJdbcInsert for the table.
     *
     * @param namedParameterJdbcTemplate the template for executing parameterized SQL queries
     * @param dataSource the DataSource for obtaining database connections
     */
    public ClientJdbcRepository(NamedParameterJdbcTemplate namedParameterJdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = namedParameterJdbcTemplate;
        this.insert = new SimpleJdbcInsert(dataSource).withTableName(table)
                .usingGeneratedKeyColumns("id");
    }

    /**
     * Retrieves all clients from the database.
     *
     * @return a list of all Client objects
     */
    @Override
    public List<Client> getAllClients() {
        String sql = "SELECT * FROM " + table;
        return jdbcTemplate.query(sql, mapper);
    }

    /**
     * Retrieves a client by its unique identifier.
     *
     * @param id the unique identifier of the client
     * @return an Optional containing the Client if found, or an empty Optional if not found
     */
    @Override
    public Optional<Client> getClientById(long id) {
        String sql = "SELECT * FROM " + table + " WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", id);
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, params, mapper));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * Retrieves a client by its associated user ID.
     *
     * @param userId the user ID associated with the client
     * @return an Optional containing the Client if found, or an empty Optional if not found
     */
    @Override
    public Optional<Client> getClientByUserId(long userId) {
        String sql = "SELECT * FROM " + table + " WHERE user_id = :userId";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("userId", userId);
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, params, mapper));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * Creates a new client record in the database.
     *
     * @param newClient the Client object to be inserted
     * @return the generated ID of the newly created client
     */
    @Override
    public long createClient(Client newClient) {
        return insert.executeAndReturnKey(
                new MapSqlParameterSource()
                        .addValue("user_id", newClient.user_id)
                        .addValue("first_name", newClient.first_name)
                        .addValue("last_name", newClient.last_name)
                        .addValue("phone", newClient.phone)
        ).longValue();
    }

    /**
     * Updates an existing client record in the database.
     *
     * @param client the Client object containing updated values
     * @return the updated Client object after executing the update
     */
    @Override
    public Client updateClient(Client client) {
        String sql = "UPDATE " + table + " SET first_name = :first_name, last_name = :last_name, phone = :phone"
                + " WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", client.id)
                .addValue("user_id", client.user_id)
                .addValue("first_name", client.first_name)
                .addValue("last_name", client.last_name)
                .addValue("phone", client.phone);
        jdbcTemplate.update(sql, params);
        return client;
    }

    /**
     * Deletes all client records from the database.
     */
    @Override
    public void deleteAll() {
        String sql = "DELETE FROM " + table;
        jdbcTemplate.update(sql, new MapSqlParameterSource());
    }

    /**
     * Maps rows of a SQL ResultSet to Client objects.
     */
    public static class ClientMapper implements RowMapper<Client> {

        /**
         * Maps the current row of the given ResultSet to a Client object.
         *
         * @param rs the ResultSet to map (pre-initialized for the current row)
         * @param rowNum the number of the current row
         * @return a Client object corresponding to the current row
         * @throws SQLException if an SQL error occurs while mapping the row
         */
        @Override
        public Client mapRow(ResultSet rs, int rowNum) throws SQLException {
            long id = rs.getLong("id");
            long userId = rs.getLong("user_id");
            String firstName = rs.getString("first_name");
            String lastName = rs.getString("last_name");
            String phone = rs.getString("phone");

            return new Client(id, userId, firstName, lastName, phone);
        }
    }
}
