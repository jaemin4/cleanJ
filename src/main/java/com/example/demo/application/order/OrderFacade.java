package com.example.demo.application.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderFacade {
    private final RedissonClient redissonClient;
    private final OrderTransaction orderTransaction;

    public OrderResult.Order order(OrderCriteria.Order criteria) {
        String lockKey = "lock:order:user:" + criteria.getUserId();
        RLock lock = redissonClient.getLock(lockKey);
        boolean isLocked = false;

        try {
            isLocked = lock.tryLock(3, 5, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new IllegalStateException("중복 주문 요청 중입니다. 잠시 후 다시 시도해주세요.");
            }

            return orderTransaction.createOrderWithTransaction(criteria);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("락 대기 중 인터럽트 발생", e);
        } catch (Exception e) {
            throw new RuntimeException("주문 처리 실패", e);
        } finally {
            if (isLocked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }




}
