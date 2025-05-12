package com.example.demo.infra.payment;

import com.example.demo.domain.payment.PaymentHistoryInfo;
import com.example.demo.domain.payment.PaymentHistoryService;
import com.example.demo.support.util.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;
    private final RedissonClient redissonClient;
    public static final String POPULAR_PRODUCTS_KEY = "popular:top5";


    /*@Scheduled(fixedRate = 300_000)
    public void refreshPopularProducts() {
        RLock lock = redissonClient.getLock("lock:popular:top5:scheduler");
        boolean isLocked = false;

        try {
            isLocked = lock.tryLock(0, 3, TimeUnit.SECONDS);
            if (!isLocked) {
                log.info("다른 인스턴스가 인기 상품 캐시 갱신 중, 이번 실행은 생략");
                return;
            }

            List<PaymentHistoryInfo.Top5Orders> top5 = paymentHistoryService.getTop5Orders();
            String json = objectMapper.writeValueAsString(top5);

            redisTemplate.opsForValue().set(POPULAR_PRODUCTS_KEY, json, Duration.ofMinutes(6));
            log.info("인기 상품 캐시 갱신 완료: {}", Utils.toJson(json));

        } catch (Exception e) {
            log.error("인기 상품 캐시 갱신 실패", e);
        } finally {
            if (isLocked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }*/

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

            List<PaymentHistoryInfo.Top5Orders> top5 = paymentHistoryService.getTop5Orders();
            for (PaymentHistoryInfo.Top5Orders order : top5) {
                redisTemplate.opsForZSet().add(POPULAR_PRODUCTS_KEY, order.getOrderId(), order.getCount());
            }

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
