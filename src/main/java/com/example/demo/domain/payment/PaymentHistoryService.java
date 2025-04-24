package com.example.demo.domain.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

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

    public List<PaymentHistoryInfo.Top5Orders> getTop5Orders() {
        return PaymentHistoryInfo.Top5Orders.fromResList(paymentHistoryRepository.findTop5OrdersByPaidStatus());
    }

}
