package com.alfre.DHHotel.adapter.persistence;

import com.alfre.DHHotel.domain.model.Reservation;
import com.alfre.DHHotel.domain.repository.ReservationRepository;
import com.alfre.DHHotel.domain.model.ReservationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public class ReservationJdbcRepository implements ReservationRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final SimpleJdbcInsert insert;

    private final ReservationMapper mapper = new ReservationMapper();

    private final String table = "Reservation";

    private static final Logger logger = LoggerFactory.getLogger(ReservationJdbcRepository.class);

    public ReservationJdbcRepository(NamedParameterJdbcTemplate namedParameterJdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = namedParameterJdbcTemplate;
        this.insert = new SimpleJdbcInsert(dataSource).withTableName(table)
                .usingGeneratedKeyColumns("id");
    }

    @Override
    public List<Reservation> getAllReservations() {
        String sql = "SELECT * FROM " + table;
        return jdbcTemplate.query(sql, mapper);
    }

    @Override
    public Optional<Reservation> getReservationById(long id) {
        String sql = "SELECT * FROM " + table + " WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id);
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, params, mapper));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Reservation> getReservationsByClientId(Long clientId) {
        String sql = "SELECT * FROM " + table + " WHERE client_id = :clientId";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("clientId", clientId);
        return jdbcTemplate.query(sql, params, mapper);
    }

    @Override
    public long createReservation(Reservation newReservation) {
        return insert.executeAndReturnKey(
                new MapSqlParameterSource()
                .addValue("client_id", newReservation.client_id)
                .addValue("roomId", newReservation.room_id)
                .addValue("totalPrice", newReservation.total_price)
                .addValue("startDate", newReservation.start_date)
                .addValue("endDate", newReservation.end_date)
                .addValue("status", newReservation.status.name())
        ).longValue();
    }

    @Override
    public int updateReservation(Reservation updatedReservation) {
        String sql = "UPDATE " + table + " SET client_id = :clientId, room_id = :roomId, " +
                "total_price = :totalPrice, start_date = :startDate, end_date = :endDate, status = :status" +
                " WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", updatedReservation.id)
                .addValue("clientId", updatedReservation.client_id)
                .addValue("roomId", updatedReservation.room_id)
                .addValue("totalPrice", updatedReservation.total_price)
                .addValue("startDate", updatedReservation.start_date)
                .addValue("endDate", updatedReservation.end_date)
                .addValue("status", updatedReservation.status.name());
        return jdbcTemplate.update(sql, params);
    }

    @Override
    public boolean isRoomAvailable(Long roomId, Date startDate, Date endDate) {
        // Validación de parámetros de entrada
        if (roomId == null) {
            throw new IllegalArgumentException("El ID de la habitación no puede ser null");
        }
        if (startDate == null) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser null");
        }
        if (endDate == null) {
            throw new IllegalArgumentException("La fecha de fin no puede ser null");
        }
        if (endDate.before(startDate)) {
            throw new IllegalArgumentException("La fecha de salida no puede ser anterior a la fecha de entrada.");
        }

        String sql = """
            SELECT COALESCE(COUNT(*), 0)
            FROM %s
            WHERE room_id = :roomId
            AND (
                (start_date <= :endDate AND end_date >= :startDate)
                AND status NOT IN ('CANCELED')
            )
            AND room_id NOT IN (
                SELECT id
                FROM Room
                WHERE status = 'MAINTENANCE'
            )
            """.formatted(table);

        // Creación del objeto para manejar parámetros nombrados
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("roomId", roomId)
                .addValue("startDate", startDate)
                .addValue("endDate", endDate);

        try {
            Integer count = jdbcTemplate.queryForObject(sql, params, Integer.class);

            // La habitación está disponible si no hay reservas que se superpongan
            return count == null || count == 0;

        } catch (DataAccessException e) {
            // Log del error y relanzamiento como una excepción más específica
            logger.error("Error al verificar la disponibilidad de la habitación: {}", e.getMessage());
            throw new RuntimeException("Error al verificar la disponibilidad de la habitación", e);
        }
    }

    @Override
    public void deleteAll() {
        String sql = "DELETE FROM " + table;
        jdbcTemplate.update(sql, new MapSqlParameterSource());
    }

    public static class ReservationMapper implements RowMapper<Reservation> {

        @Override
        public Reservation mapRow(ResultSet rs, int rowNum) throws SQLException {
            long id = rs.getInt("id");
            long clientId = rs.getInt("client_id");
            long roomId = rs.getInt("room_id");
            BigDecimal totalPrice = rs.getBigDecimal("total_price");
            Date startDate = rs.getDate("start_date");
            Date endDate = rs.getDate("end_date");
            ReservationStatus status = ReservationStatus.valueOf(rs.getString("status").toUpperCase());

            return new Reservation(id, clientId, roomId, totalPrice, startDate, endDate, status);
        }
    }
}