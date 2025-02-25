package com.alfre.DHHotel.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import com.alfre.DHHotel.domain.model.Payment;
import com.alfre.DHHotel.domain.model.MethodPayment;
import com.alfre.DHHotel.adapter.persistence.PaymentJdbcRepository;

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
public class PaymentJdbcRepositoryTest {

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Mock
    private DataSource dataSource;

    @Mock
    private SimpleJdbcInsert insert;

    // Construiremos manualmente el repositorio
    private PaymentJdbcRepository paymentRepository;

    private final String table = "Payment";

    @BeforeEach
    void setup() {
        // Inyectamos los mocks jdbcTemplate y dataSource en el constructor
        paymentRepository = new PaymentJdbcRepository(jdbcTemplate, dataSource);
        // Reemplazamos la instancia de SimpleJdbcInsert creada internamente con nuestro mock
        ReflectionTestUtils.setField(paymentRepository, "insert", insert);
    }

    @Test
    public void testGetAllPayments() {
        // Arrange
        Payment payment1 = new Payment(1L, 100L, BigDecimal.valueOf(150.00),
                new Date(System.currentTimeMillis()), MethodPayment.CARD);
        Payment payment2 = new Payment(2L, 101L, BigDecimal.valueOf(200.00),
                new Date(System.currentTimeMillis()), MethodPayment.CASH);
        List<Payment> payments = Arrays.asList(payment1, payment2);

        String sql = "SELECT * FROM " + table;

        when(jdbcTemplate.query(eq(sql), any(PaymentJdbcRepository.PaymentMapper.class)))
                .thenReturn(payments);

        // Act
        List<Payment> result = paymentRepository.getAllPayments();

        // Assert
        assertNotNull(result, "La lista de pagos no debe ser nula");
        assertEquals(2, result.size(), "Debe retornar 2 pagos");

        Payment expected = payments.getFirst();
        Payment actual = result.getFirst();

        assertEquals(expected.id, actual.id, "El ID debe coincidir");
        assertEquals(expected.reservation_id, actual.reservation_id, "El reservation_id debe coincidir");
        assertEquals(expected.amount, actual.amount, "El monto debe coincidir");
        assertEquals(expected.payment_date, actual.payment_date, "La fecha debe coincidir");
        assertEquals(expected.method, actual.method, "El método de pago debe coincidir");
    }

    @Test
    public void testGetPaymentById_found() {
        // Arrange
        long id = 1L;
        Payment expectedPayment = new Payment(id, 100L, BigDecimal.valueOf(150.00),
                new Date(System.currentTimeMillis()), MethodPayment.CARD);

        String sql = "SELECT * FROM " + table + " WHERE id = :id";

        when(jdbcTemplate.queryForObject(eq(sql), any(MapSqlParameterSource.class),
                any(PaymentJdbcRepository.PaymentMapper.class))).thenReturn(expectedPayment);

        // Act
        Optional<Payment> result = paymentRepository.getPaymentById(id);

        // Assert
        assertTrue(result.isPresent(), "El pago debe existir");

        Payment actual = result.get();

        assertEquals(expectedPayment.id, actual.id, "El ID debe coincidir");
        assertEquals(expectedPayment.reservation_id, actual.reservation_id, "El reservation_id debe coincidir");
        assertEquals(expectedPayment.amount, actual.amount, "El monto debe coincidir");
        assertEquals(expectedPayment.payment_date, actual.payment_date, "La fecha debe coincidir");
        assertEquals(expectedPayment.method, actual.method, "El método debe coincidir");
    }

    @Test
    public void testGetPaymentById_notFound() {
        // Arrange
        long id = 999L;

        String sql = "SELECT * FROM " + table + " WHERE id = :id";

        when(jdbcTemplate.queryForObject(eq(sql), any(MapSqlParameterSource.class),
                any(PaymentJdbcRepository.PaymentMapper.class)))
                .thenThrow(new EmptyResultDataAccessException(1));

        // Act
        Optional<Payment> result = paymentRepository.getPaymentById(id);

        // Assert
        assertFalse(result.isPresent(), "El pago no debe existir");
    }

    @Test
    public void testGetPaymentsByReservationId() {
        // Arrange
        long reservationId = 100L;

        Payment payment1 = new Payment(1L, reservationId, BigDecimal.valueOf(150.00),
                new Date(System.currentTimeMillis()), MethodPayment.CARD);
        Payment payment2 = new Payment(2L, reservationId, BigDecimal.valueOf(200.00),
                new Date(System.currentTimeMillis()), MethodPayment.CASH);

        List<Payment> payments = Arrays.asList(payment1, payment2);

        String sql = "SELECT * FROM " + table + " WHERE reservation_id = :reservationId";

        when(jdbcTemplate.query(eq(sql), any(MapSqlParameterSource.class),
                any(PaymentJdbcRepository.PaymentMapper.class)))
                .thenReturn(payments);

        // Act
        List<Payment> result = paymentRepository.getPaymentsByReservationId(reservationId);

        // Assert
        assertNotNull(result, "La lista de pagos no debe ser nula");
        assertEquals(2, result.size(), "Debe retornar 2 pagos");

        Payment expected = payments.getFirst();
        Payment actual = result.getFirst();

        assertEquals(expected.id, actual.id, "El ID debe coincidir");
    }

    @Test
    public void testCreatePayment() {
        // Arrange
        Payment payment = new Payment(0L, 100L, BigDecimal.valueOf(150.00),
                new Date(System.currentTimeMillis()), MethodPayment.CARD);

        when(insert.executeAndReturnKey(any(MapSqlParameterSource.class))).thenReturn(1L);

        // Act
        long generatedId = paymentRepository.createPayment(payment);

        // Assert
        assertEquals(1L, generatedId, "El ID generado debe ser 1");

        ArgumentCaptor<MapSqlParameterSource> captor = ArgumentCaptor.forClass(MapSqlParameterSource.class);

        verify(insert).executeAndReturnKey(captor.capture());

        MapSqlParameterSource params = captor.getValue();

        assertEquals(payment.reservation_id, params.getValue("reservationId"),
                "El reservationId debe coincidir");
        assertEquals(payment.amount, params.getValue("amount"),
                "El monto debe coincidir");
        assertEquals(payment.payment_date, params.getValue("payment_date"),
                "La fecha de pago debe coincidir");
        assertEquals(payment.method.name(), params.getValue("method"), "El método debe coincidir");
    }

    @Test
    public void testUpdatePayment() {
        // Arrange
        Payment payment = new Payment(1L, 100L, BigDecimal.valueOf(150.00),
                new Date(System.currentTimeMillis()), MethodPayment.CARD);
        // Actualizamos algunos campos, por ejemplo, el monto y la fecha de pago
        payment.amount = BigDecimal.valueOf(175.00);
        payment.payment_date = new Date(System.currentTimeMillis());

        String sql = "UPDATE " + table + " SET amount = :amount, payment_date = :paymentDate, method = :method" +
                " WHERE id = :id";

        when(jdbcTemplate.update(eq(sql), any(MapSqlParameterSource.class))).thenReturn(1);

        // Act
        int rows = paymentRepository.updatePayment(payment, payment.id);

        // Assert
        assertEquals(1, rows, "Se debe actualizar 1 fila");

        ArgumentCaptor<MapSqlParameterSource> captor = ArgumentCaptor.forClass(MapSqlParameterSource.class);

        verify(jdbcTemplate).update(eq(sql), captor.capture());

        MapSqlParameterSource params = captor.getValue();

        assertEquals(payment.id, params.getValue("id"), "El ID debe coincidir");
        assertEquals(payment.amount, params.getValue("amount"), "El monto debe coincidir");
        assertEquals(payment.payment_date, params.getValue("paymentDate"), "La fecha debe coincidir");
        assertEquals(payment.method.name(), params.getValue("method"), "El método debe coincidir");
    }

    @Test
    public void testDeletePayment() {
        // Arrange
        long id = 1L;

        String sql = "DELETE FROM " + table + " WHERE id = :id";

        when(jdbcTemplate.update(eq(sql), any(MapSqlParameterSource.class))).thenReturn(1);

        // Act
        int rows = paymentRepository.deletePayment(id);

        // Assert
        assertEquals(1, rows, "Se debe eliminar 1 fila");

        ArgumentCaptor<MapSqlParameterSource> captor = ArgumentCaptor.forClass(MapSqlParameterSource.class);

        verify(jdbcTemplate).update(eq(sql), captor.capture());

        MapSqlParameterSource params = captor.getValue();

        assertEquals(id, params.getValue("id"), "El ID debe coincidir");
    }

    @Test
    public void testGetTotalPaid() {
        // Arrange
        long reservationId = 100L;

        String sql = "SELECT COALESCE(SUM(amount), 0) FROM " + table + " WHERE reservation_id = :reservationId";

        BigDecimal sum = BigDecimal.valueOf(350.00);

        when(jdbcTemplate.queryForObject(eq(sql), any(MapSqlParameterSource.class), eq(BigDecimal.class)))
                .thenReturn(sum);

        // Act
        BigDecimal result = paymentRepository.getTotalPaid(reservationId);

        // Assert
        assertNotNull(result, "El resultado no debe ser nulo");
        assertEquals(sum, result, "La suma debe coincidir");
    }

    @Test
    public void testGetTotalPaid_nullResult() {
        // Arrange
        long reservationId = 100L;

        String sql = "SELECT COALESCE(SUM(amount), 0) FROM " + table + " WHERE reservation_id = :reservationId";

        when(jdbcTemplate.queryForObject(eq(sql), any(MapSqlParameterSource.class), eq(BigDecimal.class)))
                .thenReturn(null);

        // Act
        BigDecimal result = paymentRepository.getTotalPaid(reservationId);

        // Assert
        assertNotNull(result, "El resultado no debe ser nulo");
        assertEquals(BigDecimal.valueOf(0.0), result, "La suma debe ser 0 cuando es nula");
    }

    @Test
    public void testGetPaymentsByClient() {
        // Arrange
        long clientId = 200L;

        Payment payment1 = new Payment(1L, 100L, BigDecimal.valueOf(150.00),
                new Date(System.currentTimeMillis()), MethodPayment.CARD);

        Payment payment2 = new Payment(2L, 101L, BigDecimal.valueOf(200.00),
                new Date(System.currentTimeMillis()), MethodPayment.CASH);

        List<Payment> payments = Arrays.asList(payment1, payment2);

        String sql = """
                SELECT p.*
                FROM Payment p
                JOIN Reservation r ON p.reservation_id = r.id
                WHERE r.client_id = :clientId""";

        when(jdbcTemplate.query(eq(sql), any(MapSqlParameterSource.class),
                any(PaymentJdbcRepository.PaymentMapper.class)))
                .thenReturn(payments);

        // Act
        List<Payment> result = paymentRepository.getPaymentsByClient(clientId);

        // Assert
        assertNotNull(result, "La lista de pagos no debe ser nula");
        assertEquals(2, result.size(), "Debe retornar 2 pagos");

        Payment expected = payments.getFirst();
        Payment actual = result.getFirst();

        assertEquals(expected.id, actual.id, "El ID debe coincidir");
    }
}