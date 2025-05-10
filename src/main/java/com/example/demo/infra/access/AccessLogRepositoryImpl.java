package com.example.demo.infra.access;

import com.example.demo.domain.access.AccessLog;
import com.example.demo.domain.access.AccessLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AccessLogRepositoryImpl implements AccessLogRepository {

    private final AccessLogJpaRepository accessLogJpaRepository;

    @Override
    public void save(AccessLog accessLog) {
        accessLogJpaRepository.save(accessLog);
    }
}
