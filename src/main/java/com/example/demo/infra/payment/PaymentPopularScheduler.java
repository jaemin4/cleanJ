package com.example.demo.infra.payment;

import com.example.demo.domain.payment.PaymentHistory;
import com.example.demo.domain.payment.PaymentHistoryInfo;
import com.example.demo.domain.payment.PaymentHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentPopularScheduler {

    private final PaymentHistoryService paymentHistoryService;
    private final RedisTemplate<String,Object> redisTemplate;
    public static final String POPULAR_PRODUCTS_KEY = "popular:top5";

    @Scheduled(fixedRate = 300_000)
    public void refreshPopularProducts(){
        List<PaymentHistoryInfo.Top5Orders> top5 = paymentHistoryService.getTop5Orders();
        redisTemplate.opsForValue().set(POPULAR_PRODUCTS_KEY, top5, Duration.ofMinutes(6));
        log.info("인기 상품 캐시 갱신 완료: {}", top5);
    }

}
