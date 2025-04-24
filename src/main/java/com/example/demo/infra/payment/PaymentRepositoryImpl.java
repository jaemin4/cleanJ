package com.example.demo.infra.payment;

import com.example.demo.domain.payment.PaymentHistory;
import com.example.demo.domain.payment.PaymentHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentHistoryRepository {

    private final PaymentHistoryJpaRepository paymentHistoryJpaRepository;
    private final PaymentHistoryJdbcRepository paymentHistoryJdbcRepository;

    @Override
    public void save(PaymentHistory paymentHistory) {
        paymentHistoryJpaRepository.save(paymentHistory);
    }

    @Override
    public boolean existsByOrderId(Long orderId) {
        return paymentHistoryJpaRepository.existsByOrderId(orderId);
    }

    @Override
    public List<ResTopOrderFive> findTop5OrdersByPaidStatus() {
        return paymentHistoryJdbcRepository.findTop5OrdersByPaidStatus();
    }
}
