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

@Repository
public class AdministratorJdbcRepository implements AdministratorRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final SimpleJdbcInsert insert;

    private final AdministratorMapper mapper = new AdministratorMapper();

    private final String table = "Administrator";

    public AdministratorJdbcRepository(NamedParameterJdbcTemplate namedParameterJdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = namedParameterJdbcTemplate;
        this.insert = new SimpleJdbcInsert(dataSource).withTableName(table)
                .usingGeneratedKeyColumns("id");
    }

    @Override
    public List<Administrator> getAllAdministrators() {
        String sql = "SELECT * FROM " + table;
        return jdbcTemplate.query(sql, mapper);
    }

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

    @Override
    public void createAdministrator(Administrator newAdministrator) {
        insert.executeAndReturnKey(
                new MapSqlParameterSource()
                        .addValue("name", newAdministrator.name)
                        .addValue("user_id", newAdministrator.user_id)
        );
    }

    @Override
    public int updateAdministrator(Administrator administrator, long userId) {
        String sql = "UPDATE " + table + " SET name = :name WHERE user_id = :userId ";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("name", administrator.name)
                .addValue("userId", userId);
        return jdbcTemplate.update(sql, params);
    }

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

    @Override
    public void deleteAll() {
        String sql = "DELETE FROM " + table;
        jdbcTemplate.update(sql, new MapSqlParameterSource());
    }


    public static class AdministratorMapper implements RowMapper<Administrator> {

        @Override
        public Administrator mapRow(ResultSet rs, int rowNum) throws SQLException {
            long id = rs.getLong("id");
            long userId = rs.getLong("user_id");
            String name = rs.getString("name");

            return new Administrator(id, userId, name);
        }
    }
}
