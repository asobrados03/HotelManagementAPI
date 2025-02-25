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
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public class PaymentJdbcRepository implements PaymentRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final SimpleJdbcInsert insert;

    private final PaymentMapper mapper = new PaymentMapper();

    private final String table = "Payment";

    public PaymentJdbcRepository(NamedParameterJdbcTemplate namedParameterJdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = namedParameterJdbcTemplate;
        this.insert = new SimpleJdbcInsert(dataSource).withTableName(table)
                .usingGeneratedKeyColumns("id");
    }

    @Override
    public List<Payment> getAllPayments() {
        String sql = "SELECT * FROM " + table;
        return jdbcTemplate.query(sql, mapper);
    }

    @Override
    public Optional<Payment> getPaymentById(long id) {
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
    public List<Payment> getPaymentsByReservationId(long reservationId) {
        String sql = "SELECT * FROM " + table + " WHERE reservation_id = :reservationId";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("reservationId", reservationId);
        return jdbcTemplate.query(sql, params, mapper);
    }

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

    @Override
    public int deletePayment(long id) {
        String sql = "DELETE FROM " + table + " WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("id", id);
        return jdbcTemplate.update(sql, params);
    }

    @Override
    public BigDecimal getTotalPaid(long reservationId) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM " + table + " WHERE reservation_id = :reservationId";
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("reservationId", reservationId);
        BigDecimal result = jdbcTemplate.queryForObject(sql, params, BigDecimal.class);

        return (result != null ? result : BigDecimal.valueOf(0.0));
    }

    @Override
    public List<Payment> getPaymentsByClient(long clientId) {
        String sql = """
        SELECT p.*
        FROM Payment p
        JOIN Reservation r ON p.reservation_id = r.id
        WHERE r.client_id = :clientId""";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("clientId", clientId);

        return jdbcTemplate.query(sql, params, mapper);
    }

    @Override
    public void deleteAll() {
        String sql = "DELETE FROM " + table;
        jdbcTemplate.update(sql, new MapSqlParameterSource());
    }

    public static class PaymentMapper implements RowMapper<Payment> {

        @Override
        public Payment mapRow(ResultSet rs, int rowNum) throws SQLException {
            long id = rs.getLong("id");
            long reservationId = rs.getLong("reservation_id");
            BigDecimal amount = rs.getBigDecimal("amount");
            Date paymentDate = new Date(rs.getTimestamp("payment_date").getTime());
            MethodPayment method = MethodPayment.valueOf(rs.getString("method").toUpperCase());

            return new Payment(id, reservationId, amount, paymentDate, method);
        }
    }
}