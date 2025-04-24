package com.example.demo.domain.payment;

import com.example.demo.infra.payment.ResTopOrderFive;

import java.util.List;

public interface PaymentHistoryRepository {
    void save(PaymentHistory paymentHistory);

    boolean existsByOrderId(Long orderId);

    List<ResTopOrderFive> findTop5OrdersByPaidStatus();
}
