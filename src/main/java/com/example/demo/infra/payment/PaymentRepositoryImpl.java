package com.example.demo.infra.payment;

import com.example.demo.domain.payment.PaymentHistory;
import com.example.demo.domain.payment.PaymentHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentHistoryRepository {

    private final PaymentHistoryJpaRepository paymentLocalRepository;

    @Override
    public void save(PaymentHistory paymentHistory) {
        paymentLocalRepository.save(paymentHistory);
    }

    @Override
    public boolean existsByOrderId(Long orderId) {
        return paymentLocalRepository.existsByOrderId(orderId);
    }
}
