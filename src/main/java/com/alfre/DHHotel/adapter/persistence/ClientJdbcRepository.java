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

@Repository
public class ClientJdbcRepository implements ClientRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final SimpleJdbcInsert insert;

    private final ClientJdbcRepository.ClientMapper mapper = new ClientMapper();

    private final String table = "Client";

    public ClientJdbcRepository(NamedParameterJdbcTemplate namedParameterJdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = namedParameterJdbcTemplate;
        this.insert = new SimpleJdbcInsert(dataSource).withTableName(table)
                .usingGeneratedKeyColumns("id");
    }

    @Override
    public List<Client> getAllClients() {
        String sql = "SELECT * FROM "+table;
        return jdbcTemplate.query(sql, mapper);
    }

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

    @Override
    public void deleteAll() {
        String sql = "DELETE FROM " + table;
        jdbcTemplate.update(sql, new MapSqlParameterSource());
    }

    public static class ClientMapper implements RowMapper<Client> {

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
