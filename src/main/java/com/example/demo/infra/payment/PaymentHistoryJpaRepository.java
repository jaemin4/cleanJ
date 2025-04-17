package com.example.demo.infra.payment;

import com.example.demo.domain.payment.PaymentHistory;
import org.springframework.data.jpa.repository.JpaRepository;


public interface PaymentHistoryJpaRepository extends JpaRepository<PaymentHistory, Long> {


    boolean existsByOrderId(Long orderId);
}
