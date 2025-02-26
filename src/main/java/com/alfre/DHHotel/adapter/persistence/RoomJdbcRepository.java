package com.alfre.DHHotel.adapter.persistence;

import com.alfre.DHHotel.domain.model.Room;
import com.alfre.DHHotel.domain.repository.RoomRepository;
import com.alfre.DHHotel.domain.model.RoomStatus;
import com.alfre.DHHotel.domain.model.RoomType;
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
 * This class contains the attributes and methods of the room repository in the adapter layer that access to the
 * database of the API and performs the operations relation to rooms
 *
 * @author Alfredo Sobrados González
 */
@Repository
public class RoomJdbcRepository implements RoomRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final SimpleJdbcInsert insert;
    private final RoomMapper mapper = new RoomMapper();
    private final String table = "Room";

    /**
     * Constructs a new RoomJdbcRepository with the specified JDBC template and data source.
     *
     * <p>This repository uses the provided {@link NamedParameterJdbcTemplate} for executing SQL queries
     * with named parameters, and initializes a {@link SimpleJdbcInsert} to handle insertion operations
     * into the configured table. The insert operation is set up to automatically retrieve generated keys,
     * specifically the "id" column.</p>
     *
     * @param namedParameterJdbcTemplate the JDBC template used for executing SQL queries with named parameters
     * @param dataSource the data source from which database connections are obtained
     */
    public RoomJdbcRepository(NamedParameterJdbcTemplate namedParameterJdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = namedParameterJdbcTemplate;
        this.insert = new SimpleJdbcInsert(dataSource)
                .withTableName(table)
                .usingGeneratedKeyColumns("id");
    }

    /**
     * Retrieves all rooms from the database.
     *
     * @return List of all rooms
     */
    @Override
    public List<Room> getAllRooms() {
        String sql = "SELECT * FROM " + table;
        return jdbcTemplate.query(sql, mapper);
    }

    /**
     * Retrieves a room by its ID from database.
     *
     * @param id The ID of the room to retrieve
     * @return Optional containing the room if found, empty otherwise
     */
    @Override
    public Optional<Room> getRoomById(long id) {
        String sql = "SELECT * FROM " + table + " WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", id);
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, params, mapper));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * Creates a new room in the database.
     *
     * @param newRoom The room to create
     * @return The ID of the newly created room
     */
    @Override
    public long createRoom(Room newRoom) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("room_number", newRoom.room_number)
                .addValue("type", newRoom.type.name())
                .addValue("price_per_night", newRoom.price_per_night)
                .addValue("status", newRoom.status.name());
        return insert.executeAndReturnKey(params).longValue();
    }

    /**
     * Updates an existing room in the database.
     *
     * @param room The room to update
     * @return The number of rows affected (should be 1 if successful)
     */
    @Override
    public int updateRoom(Room room, long id) {
        String sql = "UPDATE " + table + " SET room_number = :room_number, type = :type, " +
                "price_per_night = :price_per_night, status = :status WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("room_number", room.room_number)
                .addValue("type", room.type.name())
                .addValue("price_per_night", room.price_per_night)
                .addValue("status", room.status.name());
        return jdbcTemplate.update(sql, params);
    }

    /**
     * Deletes a room from the database by its ID.
     *
     * @param id The ID of the room to delete
     * @return The number of rows affected (should be 1 if successful)
     */
    @Override
    public int deleteRoom(long id) {
        String sql = "DELETE FROM " + table + " WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", id);
        return jdbcTemplate.update(sql, params);
    }

    /**
     * Retrieves rooms by their type from database.
     *
     * @param type The type of rooms to retrieve
     * @return List of rooms of the specified type
     */
    @Override
    public List<Room> getRoomsByType(RoomType type) {
        String sql = "SELECT * FROM " + table + " WHERE type = :type";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("type", type.name());
        return jdbcTemplate.query(sql, params, mapper);
    }

    /**
     * Retrieves available rooms (rooms with state AVAILABLE) from database.
     *
     * @return List of available rooms
     */
    @Override
    public List<Room> getAvailableRooms() {
        String sql = "SELECT * FROM " + table + " WHERE status = :status";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("status", RoomStatus.AVAILABLE.name());
        return jdbcTemplate.query(sql, params, mapper);
    }

    /**
     * Update the status of a single room in the database.
     *
     * @param id Room ID for which we are going to update the status
     * @param status value of room status to update
     * @return Number of rows affected after the update
     */
    @Override
    public int updateStatus(long id, RoomStatus status) {
        String sql = "UPDATE " + table + " SET status = :status WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("status", status.name());
        return jdbcTemplate.update(sql, params);
    }

    /**
     * Retrieves in maintenance rooms (rooms with state MAINTENANCE) from database.
     *
     * @return List of in maintenance rooms
     */
    @Override
    public List<Room> getRoomsInMaintenance() {
        String sql = "SELECT * FROM " + table + " WHERE status = 'MAINTENANCE'";
        return jdbcTemplate.query(sql, mapper);
    }

    /**
     * Delete all rooms in the database
     */
    @Override
    public void deleteAll() {
        String sql = "DELETE FROM " + table;
        jdbcTemplate.update(sql, new MapSqlParameterSource());
    }

    /**
     * A RowMapper implementation for mapping rows of a SQL ResultSet to Room objects.
     *
     * <p>This mapper converts each row from the ResultSet into a Room instance by extracting
     * the room's id, room number, type, price per night, and status. The type and status
     * values are normalized to uppercase and then converted to their respective enum types
     * ({@link RoomType} and {@link RoomStatus}).</p>
     */
    public static class RoomMapper implements RowMapper<Room> {
        /**
         * Maps the current row of the given ResultSet to a Room object.
         *
         * @param rs     the {@code ResultSet} to map (pre-initialized for the current row)
         * @param rowNum the number of the current row
         * @return a Room instance representing the data in the current row
         * @throws SQLException if a SQL exception occurs while processing the ResultSet
         */
        @Override
        public Room mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new Room(
                    rs.getLong("id"),
                    rs.getInt("room_number"),
                    RoomType.valueOf(rs.getString("type").toUpperCase()),
                    rs.getBigDecimal("price_per_night"),
                    RoomStatus.valueOf(rs.getString("status").toUpperCase())
            );
        }
    }
}