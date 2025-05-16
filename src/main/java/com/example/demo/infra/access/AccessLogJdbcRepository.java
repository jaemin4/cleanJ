package com.example.demo.infra.access;

import com.example.demo.domain.access.AccessLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccessLogJdbcRepository {

    private final JdbcTemplate jdbcTemplate;

    public void saveAll(List<AccessLog> accessLogs) {
        String sql = "INSERT INTO t1_access_log (method, uri, query, request_body, response_body, " +
                "headers, user_agent, remote_ip, status, thread_name, request_at, response_at, duration_ms) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {

            @Override
            public void setValues(PreparedStatement ps, int i) {
                try {
                    AccessLog log = accessLogs.get(i);

                    ps.setString(1, nullable(log.getMethod()));
                    ps.setString(2, nullable(log.getUri()));
                    ps.setString(3, nullable(log.getQuery()));
                    ps.setString(4, nullable(log.getRequestBody()));
                    ps.setString(5, nullable(log.getResponseBody()));
                    ps.setString(6, nullable(log.getHeaders()));
                    ps.setString(7, nullable(log.getUserAgent()));
                    ps.setString(8, nullable(log.getRemoteIp()));
                    ps.setInt(9, log.getStatus());
                    ps.setString(10, nullable(log.getThreadName()));
                    ps.setTimestamp(11, toTimestamp(log.getRequestAt()));
                    ps.setTimestamp(12, toTimestamp(log.getResponseAt()));
                    ps.setLong(13, log.getDurationMs());

                } catch (Exception e) {
                    log.error("AccessLog JDBC Insert Fail - index: {}, error: {}", i, e.getMessage(), e);
                }
            }

            @Override
            public int getBatchSize() {
                return accessLogs.size();
            }

            private String nullable(String value) {
                return value != null ? value : "";
            }

            private Timestamp toTimestamp(java.time.LocalDateTime dateTime) {
                return dateTime != null ? Timestamp.valueOf(dateTime) : null;
            }
        });
    }
}
