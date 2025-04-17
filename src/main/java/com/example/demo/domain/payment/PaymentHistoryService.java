package com.example.demo.domain.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentHistoryService {

    private final PaymentHistoryRepository paymentHistoryRepository;

    public void recordPaymentHistory(PaymentHistoryCommand.Save command) {
        PaymentHistory paymentHistory = PaymentHistory.create(
                command.getUserId(),command.getAmount(),
                command.getOrderId(),command.getTransactionId(), command.getStatus());

        paymentHistoryRepository.save(paymentHistory);
    }


}
