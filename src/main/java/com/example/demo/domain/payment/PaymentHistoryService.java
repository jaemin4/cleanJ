package com.example.demo.domain.payment;

import com.example.demo.support.Utils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentHistoryService {

    private final PaymentHistoryRepository paymentHistoryRepository;

    @Transactional
    public void recordPaymentHistory(PaymentHistoryCommand.Save command) {
        PaymentHistory paymentHistory = PaymentHistory.create(
                command.getUserId(),command.getAmount(),
                command.getOrderId(),command.getTransactionId(), command.getStatus());

        paymentHistoryRepository.save(paymentHistory);
    }

    public List<PaymentHistoryInfo.Top5Orders> getTop5Orders() {
        return PaymentHistoryInfo.Top5Orders.fromResList(paymentHistoryRepository.findTop5OrdersByPaidStatus());
    }

    @Async
    @Transactional
    public void tryRecordPaymentHistory(PaymentHistoryCommand.ReTryRecord command, int retryCount) {
        for (int i = 0; i < retryCount; i++) {
            try {
                paymentHistoryRepository.save(PaymentHistory.create(
                        command.getUserId(),command.getAmount(),
                        command.getOrderId(),command.getTransactionId(),
                        command.getStatus())
                );
                return;
            } catch (Exception e) {
                log.warn("결제 이력 저장 재시도 {}/{} 실패: {}", i + 1, retryCount, e.getMessage());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {}
            }
        }

        log.error("결제 이력 저장 재시도 실패: {}", Utils.toJson(command));
    }

}
