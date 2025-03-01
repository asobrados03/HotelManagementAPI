package com.alfre.DHHotel.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.alfre.DHHotel.domain.model.Reservation;
import com.alfre.DHHotel.domain.model.ReservationStatus;
import com.alfre.DHHotel.adapter.persistence.ReservationJdbcRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * This class contains the attributes and methods for realize the unit tests
 * of the reservations operations JDBC repository implementation.
 *
 * @author Alfredo Sobrados González
 */
@ExtendWith(MockitoExtension.class)
public class ReservationJdbcRepositoryTest {

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Mock
    private DataSource dataSource;

    @Mock
    private SimpleJdbcInsert insert;

    private ReservationJdbcRepository reservationRepository;

    private final String table = "Reservation";

    /**
     * Initializes the test environment before each test.
     * Injects jdbcTemplate and dataSource into the repository constructor,
     * and replaces the internal SimpleJdbcInsert instance with the provided mock.
     */
    @BeforeEach
    void setup() {
        // Inject jdbcTemplate and dataSource into the constructor
        reservationRepository = new ReservationJdbcRepository(jdbcTemplate, dataSource);
        // Replace the internal SimpleJdbcInsert instance with the mock
        ReflectionTestUtils.setField(reservationRepository, "insert", insert);
    }

    /**
     * Tests that getAllReservations() returns all reservations as expected.
     */
    @Test
    void testGetAllReservations() {
        // Arrange
        Reservation res1 = new Reservation(1L, 10L, 100L, BigDecimal.valueOf(500.0),
                LocalDate.of(2023, Month.JANUARY, 1),
                LocalDate.of(2023, Month.JANUARY, 5), ReservationStatus.CONFIRMED);
        Reservation res2 = new Reservation(2L, 20L, 200L, BigDecimal.valueOf(750.0),
                LocalDate.of(2023, Month.FEBRUARY, 1),
                LocalDate.of(2023, Month.FEBRUARY, 5), ReservationStatus.PENDING);

        List<Reservation> reservations = Arrays.asList(res1, res2);

        String sql = "SELECT * FROM " + table;

        when(jdbcTemplate.query(eq(sql), any(ReservationJdbcRepository.ReservationMapper.class)))
                .thenReturn(reservations);

        // Act
        List<Reservation> result = reservationRepository.getAllReservations();

        // Assert
        assertNotNull(result, "The list of reservations should not be null");
        assertEquals(2, result.size(), "Should return 2 reservations");
        assertEquals(res1, result.getFirst(), "The first reservation should match the expected one");
    }

    /**
     * Tests that getReservationById() returns the expected reservation when found.
     */
    @Test
    void testGetReservationById_found() {
        // Arrange
        long id = 1L;
        Reservation res = new Reservation(id, 10L, 100L, BigDecimal.valueOf(500.0),
                LocalDate.of(2023, Month.JANUARY, 1),
                LocalDate.of(2023, Month.JANUARY, 5), ReservationStatus.CONFIRMED);

        String sql = "SELECT * FROM " + table + " WHERE id = :id";

        when(jdbcTemplate.queryForObject(eq(sql), any(MapSqlParameterSource.class),
                any(ReservationJdbcRepository.ReservationMapper.class)))
                .thenReturn(res);

        // Act
        Optional<Reservation> result = reservationRepository.getReservationById(id);

        // Assert
        assertTrue(result.isPresent(), "The reservation should be present");
        assertEquals(res, result.get(), "The returned reservation should match the expected one");
    }

    /**
     * Tests that getReservationById() returns an empty Optional when no reservation is found.
     */
    @Test
    void testGetReservationById_notFound() {
        // Arrange
        long id = 999L;
        String sql = "SELECT * FROM " + table + " WHERE id = :id";

        when(jdbcTemplate.queryForObject(eq(sql), any(MapSqlParameterSource.class),
                any(ReservationJdbcRepository.ReservationMapper.class)))
                .thenThrow(new EmptyResultDataAccessException(1));

        // Act
        Optional<Reservation> result = reservationRepository.getReservationById(id);

        // Assert
        assertFalse(result.isPresent(), "No reservation should be found");
    }

    /**
     * Tests that getReservationsByClientId() returns the correct reservations for the given client.
     */
    @Test
    void testGetReservationsByClientId() {
        // Arrange
        Long clientId = 10L;
        Reservation res1 = new Reservation(1L, clientId, 100L, BigDecimal.valueOf(500.0),
                LocalDate.of(2023, Month.JANUARY, 1),
                LocalDate.of(2023, Month.JANUARY, 5), ReservationStatus.CONFIRMED);
        Reservation res2 = new Reservation(2L, clientId, 200L, BigDecimal.valueOf(750.0),
                LocalDate.of(2023, Month.FEBRUARY, 1),
                LocalDate.of(2023, Month.FEBRUARY, 5), ReservationStatus.PENDING);

        List<Reservation> reservations = Arrays.asList(res1, res2);

        String sql = "SELECT * FROM " + table + " WHERE client_id = :clientId";

        when(jdbcTemplate.query(eq(sql), any(MapSqlParameterSource.class),
                any(ReservationJdbcRepository.ReservationMapper.class)))
                .thenReturn(reservations);

        // Act
        List<Reservation> result = reservationRepository.getReservationsByClientId(clientId);

        // Assert
        assertNotNull(result, "The list of reservations should not be null");
        assertEquals(2, result.size(), "Should return 2 reservations");
        assertEquals(res1, result.getFirst(), "The first reservation should match the expected one");
    }

    /**
     * Tests that createReservation() correctly creates a new reservation and returns the generated ID.
     */
    @Test
    void testCreateReservation() {
        // Arrange
        Reservation newRes = new Reservation(0L, 10L, 100L, BigDecimal.valueOf(500.0),
                LocalDate.of(2023, Month.JANUARY, 1),
                LocalDate.of(2023, Month.JANUARY, 5), ReservationStatus.CONFIRMED);

        when(insert.executeAndReturnKey(any(MapSqlParameterSource.class))).thenReturn(1L);

        // Act
        long generatedId = reservationRepository.createReservation(newRes);

        // Assert
        assertEquals(1L, generatedId, "The generated ID should be 1");

        ArgumentCaptor<MapSqlParameterSource> captor = ArgumentCaptor.forClass(MapSqlParameterSource.class);

        verify(insert).executeAndReturnKey(captor.capture());

        MapSqlParameterSource params = captor.getValue();

        assertEquals(newRes.client_id, params.getValue("client_id"), "The client_id should match");
        assertEquals(newRes.room_id, params.getValue("roomId"), "The room_id should match");
        assertEquals(newRes.total_price, params.getValue("totalPrice"), "The totalPrice should match");
        assertEquals(newRes.start_date, params.getValue("startDate"), "The startDate should match");
        assertEquals(newRes.end_date, params.getValue("endDate"), "The endDate should match");
        assertEquals(newRes.status.name(), params.getValue("status"), "The status should match");
    }

    /**
     * Tests that updateReservation() correctly updates the reservation and returns the number of rows updated.
     */
    @Test
    void testUpdateReservation() {
        // Arrange
        Reservation updatedRes = new Reservation(1L, 10L, 100L, BigDecimal.valueOf(500.0),
                LocalDate.of(2023, Month.JANUARY, 1),
                LocalDate.of(2023, Month.JANUARY, 5), ReservationStatus.CONFIRMED);
        // Modify a field (for example, total_price)
        updatedRes.total_price = BigDecimal.valueOf(550.0);

        String sql = "UPDATE " + table + " SET client_id = :clientId, room_id = :roomId, " +
                "total_price = :totalPrice, start_date = :startDate, end_date = :endDate, status = :status" +
                " WHERE id = :id";

        when(jdbcTemplate.update(eq(sql), any(MapSqlParameterSource.class))).thenReturn(1);

        // Act
        int rows = reservationRepository.updateReservation(updatedRes);

        // Assert
        assertEquals(1, rows, "One row should be updated");

        ArgumentCaptor<MapSqlParameterSource> captor = ArgumentCaptor.forClass(MapSqlParameterSource.class);

        verify(jdbcTemplate).update(eq(sql), captor.capture());

        MapSqlParameterSource params = captor.getValue();

        assertEquals(updatedRes.id, params.getValue("id"), "The id should match");
        assertEquals(updatedRes.client_id, params.getValue("clientId"), "The client_id should match");
        assertEquals(updatedRes.room_id, params.getValue("roomId"), "The room_id should match");
        assertEquals(updatedRes.total_price, params.getValue("totalPrice"), "The total_price should match");
        assertEquals(updatedRes.start_date, params.getValue("startDate"), "The start_date should match");
        assertEquals(updatedRes.end_date, params.getValue("endDate"), "The end_date should match");
        assertEquals(updatedRes.status.name(), params.getValue("status"), "The status should match");
    }

    /**
     * Tests that isRoomAvailable() returns true when the room is available.
     * A room is available if there are no overlapping reservations and it is not under maintenance.
     */
    @Test
    void testIsRoomAvailable_available() {
        // Arrange: The room is available if there are no overlapping reservations (count = 0)
        Long roomId = 100L;
        LocalDate startDate = LocalDate.of(2023, Month.JANUARY, 1);
        LocalDate endDate = LocalDate.of(2023, Month.JANUARY, 5);

        String sql = String.format("""
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
        """, table);

        when(jdbcTemplate.queryForObject(eq(sql), any(MapSqlParameterSource.class), eq(Integer.class)))
                .thenReturn(0);

        // Act
        boolean available = reservationRepository.isRoomAvailable(roomId, startDate, endDate);

        // Assert
        assertTrue(available, "The room should be available");
    }

    /**
     * Tests that isRoomAvailable() returns false when the room is not available.
     * A room is unavailable if there is at least one overlapping reservation (count > 0).
     */
    @Test
    void testIsRoomAvailable_unavailable() {
        // Arrange: The room is not available if count > 0
        Long roomId = 100L;
        LocalDate startDate = LocalDate.of(2023, Month.MAY, 1);
        LocalDate endDate = LocalDate.of(2023, Month.MAY, 5);
        String sql = String.format("""
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
        """, table);

        when(jdbcTemplate.queryForObject(eq(sql), any(MapSqlParameterSource.class), eq(Integer.class)))
                .thenReturn(2);

        // Act
        boolean available = reservationRepository.isRoomAvailable(roomId, startDate, endDate);

        // Assert
        assertFalse(available, "The room should not be available");
    }

    /**
     * Tests that isRoomAvailable() throws an IllegalArgumentException when provided with invalid parameters.
     */
    @Test
    void testIsRoomAvailable_invalidParameters() {
        // Arrange: Test null or invalid dates

        Long roomId = 100L;
        LocalDate startDate = LocalDate.of(2023, Month.MAY, 5);
        LocalDate endDate = LocalDate.of(2023, Month.MAY, 1); // endDate before startDate

        // roomId is null
        Exception ex1 = assertThrows(IllegalArgumentException.class, () ->
                reservationRepository.isRoomAvailable(null, startDate, endDate));
        assertEquals("El ID de la habitación no puede ser null", ex1.getMessage());

        // startDate is null
        Exception ex2 = assertThrows(IllegalArgumentException.class, () ->
                reservationRepository.isRoomAvailable(roomId, null, endDate));
        assertEquals("La fecha de entrada no puede ser null", ex2.getMessage());

        // endDate is null
        Exception ex3 = assertThrows(IllegalArgumentException.class, () ->
                reservationRepository.isRoomAvailable(roomId, startDate, null));
        assertEquals("La fecha de salida no puede ser null", ex3.getMessage());

        // endDate before startDate
        Exception ex4 = assertThrows(IllegalArgumentException.class, () ->
                reservationRepository.isRoomAvailable(roomId, startDate, endDate));
        assertEquals("La fecha de salida no puede ser anterior a la fecha de entrada.", ex4.getMessage());
    }

    /**
     * Tests that isRoomAvailable() correctly handles a DataAccessException thrown by jdbcTemplate.
     */
    @Test
    void testIsRoomAvailable_dataAccessException() {
        // Arrange: Simulate jdbcTemplate.queryForObject throwing a DataAccessException
        Long roomId = 100L;
        LocalDate startDate = LocalDate.of(2023, Month.MAY, 1);
        LocalDate endDate = LocalDate.of(2023, Month.MAY, 5);

        String sql = String.format("""
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
        """, table);

        when(jdbcTemplate.queryForObject(eq(sql), any(MapSqlParameterSource.class), eq(Integer.class)))
                .thenThrow(new DataAccessException("DB error") {});

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                reservationRepository.isRoomAvailable(roomId, startDate, endDate));

        assertTrue(ex.getMessage().contains("Error al verificar la disponibilidad de la habitación"));
    }
}