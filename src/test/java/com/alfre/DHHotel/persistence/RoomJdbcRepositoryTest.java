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

@ExtendWith(MockitoExtension.class)
public class RoomJdbcRepositoryTest {

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Mock
    private DataSource dataSource;

    @Mock
    private SimpleJdbcInsert insert;

    // Construimos manualmente el repositorio para inyectar los mocks
    private RoomJdbcRepository roomRepository;
    private final String table = "Room";

    @BeforeEach
    void setup() {
        // Inyectamos jdbcTemplate y dataSource en el constructor
        roomRepository = new RoomJdbcRepository(jdbcTemplate, dataSource);
        // Reemplazamos la instancia interna de SimpleJdbcInsert por nuestro mock
        ReflectionTestUtils.setField(roomRepository, "insert", insert);
    }

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
        assertNotNull(result, "La lista de habitaciones no debe ser nula");
        assertEquals(2, result.size(), "Debe retornar 2 habitaciones");
        assertEquals(room1, result.getFirst(), "La primera habitación debe coincidir");
    }

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
        assertTrue(result.isPresent(), "La habitación debe existir");
        assertEquals(room, result.get(), "La habitación retornada debe coincidir");
    }

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
        assertFalse(result.isPresent(), "No se debe encontrar la habitación");
    }

    @Test
    void testCreateRoom() {
        // Arrange
        Room newRoom = new Room(0L, 101, RoomType.SINGLE, BigDecimal.valueOf(100.0),
                RoomStatus.AVAILABLE);

        when(insert.executeAndReturnKey(any(MapSqlParameterSource.class))).thenReturn(1L);

        // Act
        long generatedId = roomRepository.createRoom(newRoom);

        // Assert
        assertEquals(1L, generatedId, "El ID generado debe ser 1");

        ArgumentCaptor<MapSqlParameterSource> captor = ArgumentCaptor.forClass(MapSqlParameterSource.class);

        verify(insert).executeAndReturnKey(captor.capture());

        MapSqlParameterSource params = captor.getValue();

        assertEquals(newRoom.room_number, params.getValue("room_number"),
                "El room_number debe coincidir");
        assertEquals(newRoom.type.name(), params.getValue("type"), "El type debe coincidir");
        assertEquals(newRoom.price_per_night, params.getValue("price_per_night"),
                "El price_per_night debe coincidir");
        assertEquals(newRoom.status.name(), params.getValue("status"), "El status debe coincidir");
    }

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
        assertEquals(1, rows, "Se debe actualizar 1 fila");

        ArgumentCaptor<MapSqlParameterSource> captor = ArgumentCaptor.forClass(MapSqlParameterSource.class);

        verify(jdbcTemplate).update(eq(sql), captor.capture());

        MapSqlParameterSource params = captor.getValue();

        assertEquals(id, params.getValue("id"), "El id debe coincidir");
        assertEquals(room.room_number, params.getValue("room_number"), "El room_number debe coincidir");
        assertEquals(room.type.name(), params.getValue("type"), "El type debe coincidir");
        assertEquals(room.price_per_night, params.getValue("price_per_night"), "El price_per_night debe coincidir");
        assertEquals(room.status.name(), params.getValue("status"), "El status debe coincidir");
    }

    @Test
    void testDeleteRoom() {
        // Arrange
        long id = 1L;
        String sql = "DELETE FROM " + table + " WHERE id = :id";
        when(jdbcTemplate.update(eq(sql), any(MapSqlParameterSource.class))).thenReturn(1);

        // Act
        int rows = roomRepository.deleteRoom(id);

        // Assert
        assertEquals(1, rows, "Se debe eliminar 1 fila");

        ArgumentCaptor<MapSqlParameterSource> captor = ArgumentCaptor.forClass(MapSqlParameterSource.class);

        verify(jdbcTemplate).update(eq(sql), captor.capture());

        MapSqlParameterSource params = captor.getValue();

        assertEquals(id, params.getValue("id"), "El id debe coincidir");
    }

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
        assertNotNull(result, "La lista de habitaciones no debe ser nula");
        assertEquals(2, result.size(), "Debe retornar 2 habitaciones");
        assertEquals(room1, result.getFirst(), "La primera habitación debe coincidir");
    }

    @Test
    void testGetAvailableRooms() {
        // Arrange
        RoomStatus available = RoomStatus.AVAILABLE;

        Room room1 = new Room(1L, 101, RoomType.SINGLE, BigDecimal.valueOf(100.0), available);
        List<Room> rooms = List.of(room1);

        String sql = "SELECT * FROM " + table + " WHERE state = :state";

        when(jdbcTemplate.query(eq(sql), any(MapSqlParameterSource.class), any(RoomJdbcRepository.RoomMapper.class)))
                .thenReturn(rooms);

        // Act
        List<Room> result = roomRepository.getAvailableRooms();

        // Assert
        assertNotNull(result, "La lista de habitaciones disponibles no debe ser nula");
        assertEquals(1, result.size(), "Debe retornar 1 habitación");
        assertEquals(room1, result.getFirst(), "La habitación debe coincidir");
    }

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
        assertEquals(1, rows, "Se debe actualizar 1 fila");

        ArgumentCaptor<MapSqlParameterSource> captor = ArgumentCaptor.forClass(MapSqlParameterSource.class);

        verify(jdbcTemplate).update(eq(sql), captor.capture());

        MapSqlParameterSource params = captor.getValue();

        assertEquals(id, params.getValue("id"), "El id debe coincidir");
        assertEquals(newStatus.name(), params.getValue("status"), "El status debe coincidir");
    }

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
        assertNotNull(result, "La lista de habitaciones en mantenimiento no debe ser nula");
        assertEquals(1, result.size(), "Debe retornar 1 habitación");
        assertEquals(room1, result.getFirst(), "La habitación debe coincidir");
    }
}