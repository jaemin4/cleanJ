package com.example.demo.domain.access;

import java.util.List;

public interface AccessLogRepository {
    void save(AccessLog accessLog);
    void saveAll(List<AccessLog> listAccessLog);
}
