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

/**
 * This class contains the attributes and methods of the reservation repository in the adapter layer that access to
 * the database of the API and performs the operations relation to reservations
 *
 * @author Alfredo Sobrados González
 */
@Repository
public class ReservationJdbcRepository implements ReservationRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final SimpleJdbcInsert insert;

    private final ReservationMapper mapper = new ReservationMapper();

    private final String table = "Reservation";

    private static final Logger logger = LoggerFactory.getLogger(ReservationJdbcRepository.class);

    /**
     * Constructs a ReservationJdbcRepository with the provided NamedParameterJdbcTemplate and DataSource.
     * This constructor initializes the jdbcTemplate and configures a SimpleJdbcInsert for the reservation table.
     *
     * @param namedParameterJdbcTemplate the template used for executing parameterized SQL queries
     * @param dataSource the DataSource for obtaining database connections
     */
    public ReservationJdbcRepository(NamedParameterJdbcTemplate namedParameterJdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = namedParameterJdbcTemplate;
        this.insert = new SimpleJdbcInsert(dataSource).withTableName(table)
                .usingGeneratedKeyColumns("id");
    }

    /**
     * Retrieves all reservations from the database.
     *
     * @return a list of all Reservation objects
     */
    @Override
    public List<Reservation> getAllReservations() {
        String sql = "SELECT * FROM " + table;
        return jdbcTemplate.query(sql, mapper);
    }

    /**
     * Retrieves a reservation by its unique identifier.
     *
     * @param id the unique identifier of the reservation
     * @return an Optional containing the Reservation if found, or an empty Optional if not found
     */
    @Override
    public Optional<Reservation> getReservationById(long id) {
        String sql = "SELECT * FROM " + table + " WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", id);
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, params, mapper));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * Retrieves all reservations associated with a given client.
     *
     * @param clientId the identifier of the client
     * @return a list of Reservation objects for the specified client
     */
    @Override
    public List<Reservation> getReservationsByClientId(Long clientId) {
        String sql = "SELECT * FROM " + table + " WHERE client_id = :clientId";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("clientId", clientId);
        return jdbcTemplate.query(sql, params, mapper);
    }

    /**
     * Creates a new reservation record in the database.
     *
     * @param newReservation the Reservation object to be inserted
     * @return the generated unique identifier for the newly created reservation
     */
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

    /**
     * Updates an existing reservation record in the database.
     *
     * @param updatedReservation the Reservation object containing updated values
     * @return the number of rows affected by the update operation
     */
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

    /**
     * Determines if a room is available for reservation within a specified date range.
     * Validates the input parameters before executing the query.
     *
     * @param roomId the identifier of the room to check availability for
     * @param startDate the start date of the desired reservation period
     * @param endDate the end date of the desired reservation period
     * @return true if the room is available (i.e., no overlapping reservations exist); false otherwise
     * @throws IllegalArgumentException if any of the parameters are null or if endDate is before startDate
     * @throws RuntimeException if a data access error occurs while checking availability
     */
    @Override
    public boolean isRoomAvailable(Long roomId, Date startDate, Date endDate) {
        // Validate input parameters
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

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("roomId", roomId)
                .addValue("startDate", startDate)
                .addValue("endDate", endDate);

        try {
            Integer count = jdbcTemplate.queryForObject(sql, params, Integer.class);
            return count == null || count == 0;
        } catch (DataAccessException e) {
            logger.error("Error al verificar la disponibilidad de la habitación: {}", e.getMessage());
            throw new RuntimeException("Error al verificar la disponibilidad de la habitación", e);
        }
    }

    /**
     * Deletes all reservation records from the database.
     */
    @Override
    public void deleteAll() {
        String sql = "DELETE FROM " + table;
        jdbcTemplate.update(sql, new MapSqlParameterSource());
    }

    /**
     * Maps rows of a ResultSet to Reservation objects.
     */
    public static class ReservationMapper implements RowMapper<Reservation> {

        /**
         * Maps the current row of the given ResultSet to a Reservation object.
         *
         * @param rs the ResultSet to map (pre-initialized for the current row)
         * @param rowNum the number of the current row
         * @return the mapped Reservation object
         * @throws SQLException if an SQL error occurs while mapping the row
         */
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