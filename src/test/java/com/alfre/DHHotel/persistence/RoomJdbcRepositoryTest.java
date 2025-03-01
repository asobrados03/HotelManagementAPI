package com.alfre.DHHotel.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import com.alfre.DHHotel.domain.model.Room;
import com.alfre.DHHotel.domain.model.RoomStatus;
import com.alfre.DHHotel.domain.model.RoomType;
import com.alfre.DHHotel.adapter.persistence.RoomJdbcRepository;
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

/**
 * This class contains the attributes and methods for realize the unit tests
 * of the rooms management operations JDBC repository implementation.
 *
 * @author Alfredo Sobrados González
 */
@ExtendWith(MockitoExtension.class)
public class RoomJdbcRepositoryTest {

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Mock
    private DataSource dataSource;

    @Mock
    private SimpleJdbcInsert insert;

    private RoomJdbcRepository roomRepository;
    private final String table = "Room";

    /**
     * Sets up the RoomJdbcRepository by injecting jdbcTemplate and dataSource into its constructor,
     * and replaces the internal SimpleJdbcInsert instance with the provided mock.
     */
    @BeforeEach
    void setup() {
        // Inject jdbcTemplate and dataSource into the constructor
        roomRepository = new RoomJdbcRepository(jdbcTemplate, dataSource);
        // Replace the internal SimpleJdbcInsert instance with our mock
        ReflectionTestUtils.setField(roomRepository, "insert", insert);
    }

    /**
     * Tests that getAllRooms() returns all rooms as expected.
     */
    @Test
    void testGetAllRooms() {
        // Arrange
        Room room1 = new Room(1L, 101, RoomType.SINGLE, BigDecimal.valueOf(100.0), RoomStatus.AVAILABLE);
        Room room2 = new Room(2L, 102, RoomType.DOUBLE, BigDecimal.valueOf(150.0), RoomStatus.OCCUPIED);

        List<Room> rooms = Arrays.asList(room1, room2);

        String sql = "SELECT * FROM " + table;

        when(jdbcTemplate.query(eq(sql), any(RoomJdbcRepository.RoomMapper.class))).thenReturn(rooms);

        // Act
        List<Room> result = roomRepository.getAllRooms();

        // Assert
        assertNotNull(result, "The list of rooms should not be null");
        assertEquals(2, result.size(), "Should return 2 rooms");
        assertEquals(room1, result.getFirst(), "The first room should match the expected one");
    }

    /**
     * Tests that getRoomById() returns the expected room when found.
     */
    @Test
    void testGetRoomById_found() {
        // Arrange
        long id = 1L;
        Room room = new Room(id, 101, RoomType.SINGLE, BigDecimal.valueOf(100.0), RoomStatus.AVAILABLE);

        String sql = "SELECT * FROM " + table + " WHERE id = :id";

        when(jdbcTemplate.queryForObject(eq(sql), any(MapSqlParameterSource.class),
                any(RoomJdbcRepository.RoomMapper.class)))
                .thenReturn(room);

        // Act
        Optional<Room> result = roomRepository.getRoomById(id);

        // Assert
        assertTrue(result.isPresent(), "The room should exist");
        assertEquals(room, result.get(), "The returned room should match the expected one");
    }

    /**
     * Tests that getRoomById() returns an empty Optional when the room is not found.
     */
    @Test
    void testGetRoomById_notFound() {
        // Arrange
        long id = 999L;
        String sql = "SELECT * FROM " + table + " WHERE id = :id";

        when(jdbcTemplate.queryForObject(eq(sql), any(MapSqlParameterSource.class),
                any(RoomJdbcRepository.RoomMapper.class)))
                .thenThrow(new EmptyResultDataAccessException(1));

        // Act
        Optional<Room> result = roomRepository.getRoomById(id);

        // Assert
        assertFalse(result.isPresent(), "No room should be found");
    }

    /**
     * Tests that createRoom() successfully creates a new room and returns the generated ID.
     */
    @Test
    void testCreateRoom() {
        // Arrange
        Room newRoom = new Room(0L, 101, RoomType.SINGLE, BigDecimal.valueOf(100.0),
                RoomStatus.AVAILABLE);

        when(insert.executeAndReturnKey(any(MapSqlParameterSource.class))).thenReturn(1L);

        // Act
        long generatedId = roomRepository.createRoom(newRoom);

        // Assert
        assertEquals(1L, generatedId, "The generated ID should be 1");

        ArgumentCaptor<MapSqlParameterSource> captor = ArgumentCaptor.forClass(MapSqlParameterSource.class);

        verify(insert).executeAndReturnKey(captor.capture());

        MapSqlParameterSource params = captor.getValue();

        assertEquals(newRoom.room_number, params.getValue("room_number"),
                "The room_number should match");
        assertEquals(newRoom.type.name(), params.getValue("type"), "The type should match");
        assertEquals(newRoom.price_per_night, params.getValue("price_per_night"),
                "The price_per_night should match");
        assertEquals(newRoom.status.name(), params.getValue("status"), "The status should match");
    }

    /**
     * Tests that updateRoom() correctly updates a room and returns the number of rows updated.
     */
    @Test
    void testUpdateRoom() {
        // Arrange
        long id = 1L;
        Room room = new Room(id, 101, RoomType.SINGLE, BigDecimal.valueOf(100.0), RoomStatus.AVAILABLE);
        String sql = "UPDATE " + table + " SET room_number = :room_number, type = :type, " +
                "price_per_night = :price_per_night, status = :status WHERE id = :id";
        when(jdbcTemplate.update(eq(sql), any(MapSqlParameterSource.class))).thenReturn(1);

        // Act
        int rows = roomRepository.updateRoom(room, id);

        // Assert
        assertEquals(1, rows, "One row should be updated");

        ArgumentCaptor<MapSqlParameterSource> captor = ArgumentCaptor.forClass(MapSqlParameterSource.class);

        verify(jdbcTemplate).update(eq(sql), captor.capture());

        MapSqlParameterSource params = captor.getValue();

        assertEquals(id, params.getValue("id"), "The id should match");
        assertEquals(room.room_number, params.getValue("room_number"), "The room_number should match");
        assertEquals(room.type.name(), params.getValue("type"), "The type should match");
        assertEquals(room.price_per_night, params.getValue("price_per_night"),
                "The price_per_night should match");
        assertEquals(room.status.name(), params.getValue("status"), "The status should match");
    }

    /**
     * Tests that deleteRoom() correctly deletes a room and returns the number of rows deleted.
     */
    @Test
    void testDeleteRoom() {
        // Arrange
        long id = 1L;
        String sql = "DELETE FROM " + table + " WHERE id = :id";
        when(jdbcTemplate.update(eq(sql), any(MapSqlParameterSource.class))).thenReturn(1);

        // Act
        int rows = roomRepository.deleteRoom(id);

        // Assert
        assertEquals(1, rows, "One row should be deleted");

        ArgumentCaptor<MapSqlParameterSource> captor = ArgumentCaptor.forClass(MapSqlParameterSource.class);

        verify(jdbcTemplate).update(eq(sql), captor.capture());

        MapSqlParameterSource params = captor.getValue();

        assertEquals(id, params.getValue("id"), "The id should match");
    }

    /**
     * Tests that getRoomsByType() returns the correct list of rooms for a given room type.
     */
    @Test
    void testGetRoomsByType() {
        // Arrange
        RoomType type = RoomType.DOUBLE;
        Room room1 = new Room(1L, 102, type, BigDecimal.valueOf(150.0), RoomStatus.AVAILABLE);
        Room room2 = new Room(2L, 103, type, BigDecimal.valueOf(160.0), RoomStatus.OCCUPIED);

        List<Room> rooms = Arrays.asList(room1, room2);

        String sql = "SELECT * FROM " + table + " WHERE type = :type";

        when(jdbcTemplate.query(eq(sql), any(MapSqlParameterSource.class), any(RoomJdbcRepository.RoomMapper.class)))
                .thenReturn(rooms);

        // Act
        List<Room> result = roomRepository.getRoomsByType(type);

        // Assert
        assertNotNull(result, "The list of rooms should not be null");
        assertEquals(2, result.size(), "Should return 2 rooms");
        assertEquals(room1, result.getFirst(), "The first room should match the expected one");
    }

    /**
     * Tests that getAvailableRooms() returns the correct list of available rooms.
     */
    @Test
    void testGetAvailableRooms() {
        // Arrange
        RoomStatus available = RoomStatus.AVAILABLE;

        Room room1 = new Room(1L, 101, RoomType.SINGLE, BigDecimal.valueOf(100.0), available);
        List<Room> rooms = List.of(room1);

        String sql = "SELECT * FROM " + table + " WHERE status = :status";

        when(jdbcTemplate.query(eq(sql), any(MapSqlParameterSource.class), any(RoomJdbcRepository.RoomMapper.class)))
                .thenReturn(rooms);

        // Act
        List<Room> result = roomRepository.getAvailableRooms();

        // Assert
        assertNotNull(result, "The list of available rooms should not be null");
        assertEquals(1, result.size(), "Should return 1 room");
        assertEquals(room1, result.getFirst(), "The room should match the expected one");
    }

    /**
     * Tests that updateStatus() correctly updates the status of a room and returns the number of rows updated.
     */
    @Test
    void testUpdateStatus() {
        // Arrange
        long id = 1L;
        RoomStatus newStatus = RoomStatus.OCCUPIED;

        String sql = "UPDATE " + table + " SET status = :status WHERE id = :id";

        when(jdbcTemplate.update(eq(sql), any(MapSqlParameterSource.class))).thenReturn(1);

        // Act
        int rows = roomRepository.updateStatus(id, newStatus);

        // Assert
        assertEquals(1, rows, "One row should be updated");

        ArgumentCaptor<MapSqlParameterSource> captor = ArgumentCaptor.forClass(MapSqlParameterSource.class);

        verify(jdbcTemplate).update(eq(sql), captor.capture());

        MapSqlParameterSource params = captor.getValue();

        assertEquals(id, params.getValue("id"), "The id should match");
        assertEquals(newStatus.name(), params.getValue("status"), "The status should match");
    }

    /**
     * Tests that getRoomsInMaintenance() returns the list of rooms that are under maintenance.
     */
    @Test
    void testGetRoomsInMaintenance() {
        // Arrange
        Room room1 = new Room(1L, 101, RoomType.SINGLE, BigDecimal.valueOf(100.0), RoomStatus.MAINTENANCE);
        List<Room> rooms = List.of(room1);

        String sql = "SELECT * FROM " + table + " WHERE status = 'MAINTENANCE'";

        when(jdbcTemplate.query(eq(sql), any(RoomJdbcRepository.RoomMapper.class)))
                .thenReturn(rooms);

        // Act
        List<Room> result = roomRepository.getRoomsInMaintenance();

        // Assert
        assertNotNull(result, "The list of rooms in maintenance should not be null");
        assertEquals(1, result.size(), "Should return 1 room");
        assertEquals(room1, result.getFirst(), "The room should match the expected one");
    }
}