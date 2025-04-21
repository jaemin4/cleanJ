package com.example.demo.domain.payment;

public interface PaymentHistoryRepository {
    void save(PaymentHistory paymentHistory);

    boolean existsByOrderId(Long orderId);
}
