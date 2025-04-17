package com.example.demo.infra.payment;

import com.example.demo.domain.payment.PaymentHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class PaymentJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public void saveAll(List<PaymentHistory> paymentHistories) {
        String sql = "INSERT INTO t_payment_history (user_id, amount, order_id, transaction_id, status) " +
                "VALUES (?, ?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                PaymentHistory ph = paymentHistories.get(i);
                ps.setLong(1, ph.getUserId());
                ps.setLong(2, ph.getAmount());
                ps.setLong(3, ph.getOrderId());
                ps.setString(4, ph.getTransactionId());
                ps.setString(5, ph.getStatus());
            }

            @Override
            public int getBatchSize() {
                return paymentHistories.size();
            }
        });
    }
    public int count() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_payment_history", Integer.class);
    }

    public List<Map<String, Object>> findTop5OrdersByPaidStatusIndexed() {
        String sql = """
        SELECT order_id, COUNT(*) AS cnt
        FROM t_payment_history
        WHERE status = 'PAID'
        GROUP BY order_id
        ORDER BY cnt DESC
        LIMIT 5
    """;

        return jdbcTemplate.queryForList(sql);
    }

}
