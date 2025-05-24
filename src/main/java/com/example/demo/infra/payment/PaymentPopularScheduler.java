package com.example.demo.infra.payment;

import com.example.demo.domain.payment.PaymentHistoryInfo;
import com.example.demo.domain.payment.PaymentHistoryService;
import com.example.demo.support.util.Utils;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentPopularScheduler {

    private final PaymentHistoryService paymentHistoryService;
    private final RedisTemplate<String,Object> redisTemplate;
    private final RedissonClient redissonClient;
    public static final String POPULAR_PRODUCTS_KEY = "popular:top5";

    @PostConstruct
    public void initPopularRanking() {
        RLock lock = redissonClient.getLock("lock:popular:products:init");
        boolean isLocked = false;

        try {
            isLocked = lock.tryLock(0, 3, TimeUnit.SECONDS);
            if (!isLocked) {
                log.info("다른 인스턴스가 초기 인기 상품 ZSET 등록 중, 이번 초기화 생략");
                return;
            }

            redisTemplate.delete(POPULAR_PRODUCTS_KEY);

            List<PaymentHistoryInfo.Top5Orders> top5 = paymentHistoryService.getTop5Orders();
            for (PaymentHistoryInfo.Top5Orders order : top5) {
                redisTemplate.opsForZSet().add(POPULAR_PRODUCTS_KEY, order.getOrderId(), order.getCount());
            }

            redisTemplate.expire(POPULAR_PRODUCTS_KEY, 10, TimeUnit.MINUTES);

            log.info("초기 인기 상품 ZSET 등록 완료: {}", Utils.toJson(top5));

        } catch (Exception e) {
            log.error("초기 인기 상품 ZSET 등록 실패", e);
        } finally {
            if (isLocked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }




}
