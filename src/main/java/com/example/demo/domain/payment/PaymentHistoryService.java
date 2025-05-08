package com.example.demo.domain.payment;

import com.fasterxml.jackson.core.type.TypeReference;
import com.example.demo.support.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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

    public List<PaymentHistoryInfo.Top5OrdersForCaching> getPopularProductsFromCache() {
        try {
            log.info("받아올 데이터 : {}", Utils.toJson(redisTemplate.opsForValue().get(POPULAR_PRODUCTS_KEY)));
            String json = (String) redisTemplate.opsForValue().get(POPULAR_PRODUCTS_KEY);
            if (json == null) {
                return List.of();
            }

            return objectMapper.readValue(
                    json,
                    new TypeReference<List<PaymentHistoryInfo.Top5OrdersForCaching>>() {}
            );
        } catch (Exception e) {
            log.error("인기 상품 캐시 역직렬화 실패", e);
            return Collections.emptyList();
        }
    }




}
