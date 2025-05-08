package com.example.demo.domain.payment;

import com.example.demo.infra.payment.ResTopOrderFive;
import com.fasterxml.jackson.core.type.TypeReference;
import com.example.demo.support.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import static com.example.demo.infra.payment.PaymentPopularScheduler.POPULAR_PRODUCTS_KEY;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentHistoryService {

    private final PaymentHistoryRepository paymentHistoryRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

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

    public List<PaymentHistoryInfo.Top5OrdersForCaching> getPopularProducts() {
        String json = (String) redisTemplate.opsForValue().get(POPULAR_PRODUCTS_KEY);
        if (json != null) {
            try {
                return objectMapper.readValue(
                        json,
                        new TypeReference<List<PaymentHistoryInfo.Top5OrdersForCaching>>() {}
                );
            } catch (Exception e) {
                log.error("캐시 역직렬화 실패", e);
            }
        }

        List<ResTopOrderFive> popularProducts = paymentHistoryRepository.findTop5OrdersByPaidStatus();
        try {
            String newJson = objectMapper.writeValueAsString(popularProducts);
            redisTemplate.opsForValue().set(POPULAR_PRODUCTS_KEY, newJson, Duration.ofMinutes(5));
        } catch (Exception e) {
            log.warn("인기 상품 캐시 저장 실패", e);
        }

        return PaymentHistoryInfo.Top5OrdersForCaching.fromResTopList(popularProducts);
    }




}
