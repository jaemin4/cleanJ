package com.example.demo.infra.access;

import com.example.demo.domain.access.AccessLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessLogJpaRepository extends JpaRepository<AccessLog, Long> {

}
