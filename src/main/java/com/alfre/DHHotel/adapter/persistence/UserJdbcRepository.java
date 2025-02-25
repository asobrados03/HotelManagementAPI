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

@Repository
public class UserJdbcRepository implements UserRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final SimpleJdbcInsert insert;

    private final UserJdbcRepository.UserMapper mapper = new UserJdbcRepository.UserMapper();

    private final String table = "Users";

    public UserJdbcRepository(NamedParameterJdbcTemplate namedParameterJdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = namedParameterJdbcTemplate;
        this.insert = new SimpleJdbcInsert(dataSource).withTableName(table)
                .usingGeneratedKeyColumns("id");
    }

    @Override
    public List<User> getAllUsers() {
        String sql = "SELECT * FROM " + table;
        return jdbcTemplate.query(sql, mapper);
    }

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

    @Override
    public long createUser(User newUser) {
        return insert.executeAndReturnKey(
                new MapSqlParameterSource()
                        .addValue("email", newUser.email)
                        .addValue("password", newUser.password)
                        .addValue("role", newUser.role)
        ).longValue();
    }

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

    @Override
    public int deleteUser(long id) {
        String sql = "DELETE FROM " + table + " WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id);
        return jdbcTemplate.update(sql, params);
    }

    @Override
    public void deleteAll() {
        String sql = "DELETE FROM " + table;
        jdbcTemplate.update(sql, new MapSqlParameterSource());
    }

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

    public static class UserMapper implements RowMapper<User> {
        @Override
        public User mapRow (ResultSet rs,int rowNum) throws SQLException {
            long id = rs.getLong("id");
            String email = rs.getString("email");
            String password = rs.getString("password");
            Role role = Role.valueOf(rs.getString("role")
                    .toUpperCase());

            return new User(id, email, password, role);
        }
    }
}