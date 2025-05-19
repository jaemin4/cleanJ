package com.example.demo.infra.access;

import com.example.demo.domain.access.AccessLog;
import com.example.demo.domain.access.AccessLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AccessLogRepositoryImpl implements AccessLogRepository {

    private final AccessLogJpaRepository accessLogJpaRepository;
    private final AccessLogJdbcRepository accessLogJdbcRepository;

    @Override
    public void save(AccessLog accessLog) {
        accessLogJpaRepository.save(accessLog);
    }

    @Override
    public void saveAll(List<AccessLog> listAccessLog) {
        accessLogJdbcRepository.saveAll(listAccessLog);
    }
}
