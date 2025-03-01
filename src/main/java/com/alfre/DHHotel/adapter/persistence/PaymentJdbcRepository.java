package com.alfre.DHHotel.adapter.persistence;

import com.alfre.DHHotel.domain.model.MethodPayment;
import com.alfre.DHHotel.domain.model.Payment;
import com.alfre.DHHotel.domain.repository.PaymentRepository;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * This class contains the attributes and methods of the payment repository in the adapter layer that access to
 * the database of the API and performs the operations relation to payments
 *
 * @author Alfredo Sobrados González
 */
@Repository
public class PaymentJdbcRepository implements PaymentRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final SimpleJdbcInsert insert;

    private final PaymentMapper mapper = new PaymentMapper();

    private final String table = "Payment";

    /**
     * Constructs a PaymentJdbcRepository with the provided NamedParameterJdbcTemplate and DataSource.
     * This constructor initializes the jdbcTemplate and configures a SimpleJdbcInsert for the payment table.
     *
     * @param namedParameterJdbcTemplate the template for executing parameterized SQL queries
     * @param dataSource the DataSource for obtaining database connections
     */
    public PaymentJdbcRepository(NamedParameterJdbcTemplate namedParameterJdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = namedParameterJdbcTemplate;
        this.insert = new SimpleJdbcInsert(dataSource).withTableName(table)
                .usingGeneratedKeyColumns("id");
    }

    /**
     * Retrieves all payments from the database.
     *
     * @return a list of all Payment objects
     */
    @Override
    public List<Payment> getAllPayments() {
        String sql = "SELECT * FROM " + table;
        return jdbcTemplate.query(sql, mapper);
    }

    /**
     * Retrieves a payment by its unique identifier.
     *
     * @param id the unique identifier of the payment
     * @return an Optional containing the Payment if found, or an empty Optional if not found
     */
    @Override
    public Optional<Payment> getPaymentById(long id) {
        String sql = "SELECT * FROM " + table + " WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", id);
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, params, mapper));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * Retrieves all payments associated with a specific reservation.
     *
     * @param reservationId the identifier of the reservation
     * @return a list of Payment objects associated with the reservation
     */
    @Override
    public List<Payment> getPaymentsByReservationId(long reservationId) {
        String sql = "SELECT * FROM " + table + " WHERE reservation_id = :reservationId";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("reservationId", reservationId);
        return jdbcTemplate.query(sql, params, mapper);
    }

    /**
     * Creates a new payment record in the database.
     *
     * @param payment the Payment object to be inserted
     * @return the generated identifier of the newly created payment
     */
    @Override
    public long createPayment(Payment payment) {
        return insert.executeAndReturnKey(
                new MapSqlParameterSource()
                        .addValue("reservationId", payment.reservation_id)
                        .addValue("amount", payment.amount)
                        .addValue("payment_date", payment.payment_date)
                        .addValue("method", payment.method.name())
        ).longValue();
    }

    /**
     * Updates an existing payment record in the database.
     *
     * @param payment the Payment object containing updated data
     * @param id the unique identifier of the payment to update
     * @return the number of rows affected by the update
     */
    @Override
    public int updatePayment(Payment payment, long id) {
        String sql = "UPDATE " + table + " SET amount = :amount, " +
                "payment_date = :paymentDate, method = :method WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("amount", payment.amount)
                .addValue("paymentDate", payment.payment_date)
                .addValue("method", payment.method.name());
        return jdbcTemplate.update(sql, params);
    }

    /**
     * Deletes a payment record from the database by its unique identifier.
     *
     * @param id the unique identifier of the payment to delete
     * @return the number of rows affected by the delete operation
     */
    @Override
    public int deletePayment(long id) {
        String sql = "DELETE FROM " + table + " WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", id);
        return jdbcTemplate.update(sql, params);
    }

    /**
     * Retrieves the total amount paid for a given reservation.
     *
     * @param reservationId the identifier of the reservation
     * @return a BigDecimal representing the sum of all payments for the reservation; returns 0 if no payments exist
     */
    @Override
    public BigDecimal getTotalPaid(long reservationId) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM " + table + " WHERE reservation_id = :reservationId";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("reservationId", reservationId);
        BigDecimal result = jdbcTemplate.queryForObject(sql, params, BigDecimal.class);
        return (result != null ? result : BigDecimal.valueOf(0.0));
    }

    /**
     * Retrieves all payments associated with a specific client.
     * This method joins the Payment and Reservation tables to filter payments by client ID.
     *
     * @param clientId the identifier of the client
     * @return a list of Payment objects associated with the client
     */
    @Override
    public List<Payment> getPaymentsByClient(long clientId) {
        String sql = """
    SELECT p.*
    FROM Payment p
    JOIN Reservation r ON p.reservation_id = r.id
    WHERE r.client_id = :clientId""";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("clientId", clientId);
        return jdbcTemplate.query(sql, params, mapper);
    }

    /**
     * Deletes all payment records from the database.
     */
    @Override
    public void deleteAll() {
        String sql = "DELETE FROM " + table;
        jdbcTemplate.update(sql, new MapSqlParameterSource());
    }

    /**
     * Maps rows of a SQL ResultSet to Payment objects.
     */
    public static class PaymentMapper implements RowMapper<Payment> {

        /**
         * Maps the current row of the given ResultSet to a Payment object.
         *
         * @param rs the ResultSet to map (pre-initialized for the current row)
         * @param rowNum the number of the current row
         * @return the mapped Payment object
         * @throws SQLException if an SQL error occurs while mapping the row
         */
        @Override
        public Payment mapRow(ResultSet rs, int rowNum) throws SQLException {
            long id = rs.getLong("id");
            long reservationId = rs.getLong("reservation_id");
            BigDecimal amount = rs.getBigDecimal("amount");
            LocalDate paymentDate = rs.getObject("payment_date", LocalDate.class);
            MethodPayment method = MethodPayment.valueOf(rs.getString("method").toUpperCase());

            return new Payment(id, reservationId, amount, paymentDate, method);
        }
    }
}