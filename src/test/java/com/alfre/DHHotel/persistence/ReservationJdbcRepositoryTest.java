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
import java.sql.Date;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class ReservationJdbcRepositoryTest {

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Mock
    private DataSource dataSource;

    @Mock
    private SimpleJdbcInsert insert;

    // Construiremos manualmente el repositorio para inyectar los mocks
    private ReservationJdbcRepository reservationRepository;

    private final String table = "Reservation";

    @BeforeEach
    void setup() {
        // Inyecta jdbcTemplate y dataSource en el constructor
        reservationRepository = new ReservationJdbcRepository(jdbcTemplate, dataSource);
        // Reemplaza la instancia interna de SimpleJdbcInsert por el mock
        ReflectionTestUtils.setField(reservationRepository, "insert", insert);
    }

    @Test
    void testGetAllReservations() {
        // Arrange
        Reservation res1 = new Reservation(1L, 10L, 100L, BigDecimal.valueOf(500.0),
                Date.valueOf("2023-01-01"), Date.valueOf("2023-01-05"), ReservationStatus.CONFIRMED);
        Reservation res2 = new Reservation(2L, 20L, 200L, BigDecimal.valueOf(750.0),
                Date.valueOf("2023-02-01"), Date.valueOf("2023-02-05"), ReservationStatus.PENDING);

        List<Reservation> reservations = Arrays.asList(res1, res2);

        String sql = "SELECT * FROM " + table;

        when(jdbcTemplate.query(eq(sql), any(ReservationJdbcRepository.ReservationMapper.class)))
                .thenReturn(reservations);

        // Act
        List<Reservation> result = reservationRepository.getAllReservations();

        // Assert
        assertNotNull(result, "La lista de reservas no debe ser nula");
        assertEquals(2, result.size(), "Debe retornar 2 reservas");
        assertEquals(res1, result.get(0), "La primera reserva debe coincidir");
    }

    @Test
    void testGetReservationById_found() {
        // Arrange
        long id = 1L;
        Reservation res = new Reservation(id, 10L, 100L, BigDecimal.valueOf(500.0),
                Date.valueOf("2023-01-01"), Date.valueOf("2023-01-05"), ReservationStatus.CONFIRMED);

        String sql = "SELECT * FROM " + table + " WHERE id = :id";

        when(jdbcTemplate.queryForObject(eq(sql), any(MapSqlParameterSource.class),
                any(ReservationJdbcRepository.ReservationMapper.class)))
                .thenReturn(res);

        // Act
        Optional<Reservation> result = reservationRepository.getReservationById(id);

        // Assert
        assertTrue(result.isPresent(), "La reserva debe existir");
        assertEquals(res, result.get(), "La reserva retornada debe coincidir con la esperada");
    }

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
        assertFalse(result.isPresent(), "No se debe encontrar la reserva");
    }

    @Test
    void testGetReservationsByClientId() {
        // Arrange
        Long clientId = 10L;
        Reservation res1 = new Reservation(1L, clientId, 100L, BigDecimal.valueOf(500.0),
                Date.valueOf("2023-01-01"), Date.valueOf("2023-01-05"), ReservationStatus.CONFIRMED);
        Reservation res2 = new Reservation(2L, clientId, 200L, BigDecimal.valueOf(750.0),
                Date.valueOf("2023-02-01"), Date.valueOf("2023-02-05"), ReservationStatus.PENDING);

        List<Reservation> reservations = Arrays.asList(res1, res2);

        String sql = "SELECT * FROM " + table + " WHERE client_id = :clientId";

        when(jdbcTemplate.query(eq(sql), any(MapSqlParameterSource.class),
                any(ReservationJdbcRepository.ReservationMapper.class)))
                .thenReturn(reservations);

        // Act
        List<Reservation> result = reservationRepository.getReservationsByClientId(clientId);

        // Assert
        assertNotNull(result, "La lista de reservas no debe ser nula");
        assertEquals(2, result.size(), "Debe retornar 2 reservas");
        assertEquals(res1, result.get(0), "La primera reserva debe coincidir");
    }

    @Test
    void testCreateReservation() {
        // Arrange
        Reservation newRes = new Reservation(0L, 10L, 100L, BigDecimal.valueOf(500.0),
                Date.valueOf("2023-01-01"), Date.valueOf("2023-01-05"), ReservationStatus.CONFIRMED);

        when(insert.executeAndReturnKey(any(MapSqlParameterSource.class))).thenReturn(1L);

        // Act
        long generatedId = reservationRepository.createReservation(newRes);

        // Assert
        assertEquals(1L, generatedId, "El ID generado debe ser 1");

        ArgumentCaptor<MapSqlParameterSource> captor = ArgumentCaptor.forClass(MapSqlParameterSource.class);

        verify(insert).executeAndReturnKey(captor.capture());

        MapSqlParameterSource params = captor.getValue();

        assertEquals(newRes.client_id, params.getValue("client_id"), "El client_id debe coincidir");
        assertEquals(newRes.room_id, params.getValue("roomId"), "El room_id debe coincidir");
        assertEquals(newRes.total_price, params.getValue("totalPrice"), "El totalPrice debe coincidir");
        assertEquals(newRes.start_date, params.getValue("startDate"), "La startDate debe coincidir");
        assertEquals(newRes.end_date, params.getValue("endDate"), "La endDate debe coincidir");
        assertEquals(newRes.status.name(), params.getValue("status"), "El status debe coincidir");
    }

    @Test
    void testUpdateReservation() {
        // Arrange
        Reservation updatedRes = new Reservation(1L, 10L, 100L, BigDecimal.valueOf(500.0),
                Date.valueOf("2023-01-01"), Date.valueOf("2023-01-05"), ReservationStatus.CONFIRMED);
        // Modificamos algún campo (por ejemplo, total_price)
        updatedRes.total_price = BigDecimal.valueOf(550.0);

        String sql = "UPDATE " + table + " SET client_id = :clientId, room_id = :roomId, " +
                "total_price = :totalPrice, start_date = :startDate, end_date = :endDate, status = :status" +
                " WHERE id = :id";

        when(jdbcTemplate.update(eq(sql), any(MapSqlParameterSource.class))).thenReturn(1);

        // Act
        int rows = reservationRepository.updateReservation(updatedRes);

        // Assert
        assertEquals(1, rows, "Se debe actualizar 1 fila");

        ArgumentCaptor<MapSqlParameterSource> captor = ArgumentCaptor.forClass(MapSqlParameterSource.class);

        verify(jdbcTemplate).update(eq(sql), captor.capture());

        MapSqlParameterSource params = captor.getValue();

        assertEquals(updatedRes.id, params.getValue("id"), "El id debe coincidir");
        assertEquals(updatedRes.client_id, params.getValue("clientId"), "El client_id debe coincidir");
        assertEquals(updatedRes.room_id, params.getValue("roomId"), "El room_id debe coincidir");
        assertEquals(updatedRes.total_price, params.getValue("totalPrice"), "El total_price debe coincidir");
        assertEquals(updatedRes.start_date, params.getValue("startDate"), "La start_date debe coincidir");
        assertEquals(updatedRes.end_date, params.getValue("endDate"), "La end_date debe coincidir");
        assertEquals(updatedRes.status.name(), params.getValue("status"), "El status debe coincidir");
    }

    @Test
    void testIsRoomAvailable_available() {
        // Arrange: La habitación está disponible si no existen reservas que se superpongan (count = 0)
        Long roomId = 100L;
        Date startDate = Date.valueOf("2023-05-01");
        Date endDate = Date.valueOf("2023-05-05");

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
        assertTrue(available, "La habitación debe estar disponible");
    }

    @Test
    void testIsRoomAvailable_unavailable() {
        // Arrange: La habitación no está disponible si count > 0
        Long roomId = 100L;
        Date startDate = Date.valueOf("2023-05-01");
        Date endDate = Date.valueOf("2023-05-05");
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
        assertFalse(available, "La habitación no debe estar disponible");
    }

    @Test
    void testIsRoomAvailable_invalidParameters() {
        // Validaciones de parámetros nulos o fechas inválidas

        Long roomId = 100L;
        Date startDate = Date.valueOf("2023-05-05");
        Date endDate = Date.valueOf("2023-05-01"); // endDate anterior a startDate

        // roomId null
        Exception ex1 = assertThrows(IllegalArgumentException.class, () ->
                reservationRepository.isRoomAvailable(null, startDate, endDate));
        assertEquals("El ID de la habitación no puede ser null", ex1.getMessage());

        // startDate null
        Exception ex2 = assertThrows(IllegalArgumentException.class, () ->
                reservationRepository.isRoomAvailable(roomId, null, endDate));
        assertEquals("La fecha de inicio no puede ser null", ex2.getMessage());

        // endDate null
        Exception ex3 = assertThrows(IllegalArgumentException.class, () ->
                reservationRepository.isRoomAvailable(roomId, startDate, null));
        assertEquals("La fecha de fin no puede ser null", ex3.getMessage());

        // endDate antes que startDate
        Exception ex4 = assertThrows(IllegalArgumentException.class, () ->
                reservationRepository.isRoomAvailable(roomId, startDate, endDate));
        assertEquals("La fecha de fin no puede ser anterior a la fecha de inicio", ex4.getMessage());
    }

    @Test
    void testIsRoomAvailable_dataAccessException() {
        // Simula que jdbcTemplate.queryForObject lanza una DataAccessException
        Long roomId = 100L;
        Date startDate = Date.valueOf("2023-05-01");
        Date endDate = Date.valueOf("2023-05-05");

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