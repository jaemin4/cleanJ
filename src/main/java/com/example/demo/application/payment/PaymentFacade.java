package com.example.demo.application.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentFacade {

    private final RedissonClient redissonClient;
    private final PaymentTransaction paymentTransaction;

    public void pay(PaymentCriteria.Payment criteria) {
        String lockKey = "lock:payment:user:" + criteria.getUserId();
        RLock lock = redissonClient.getLock(lockKey);
        boolean isLocked = false;

        try {
            isLocked = lock.tryLock(3, 5, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new IllegalStateException("중복 결제 요청입니다. 잠시 후 다시 시도해주세요.");
            }

            paymentTransaction.processPaymentWithTransaction(criteria);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("결제 락 대기 중 인터럽트 발생", e);
        } catch (Exception e) {
            throw new RuntimeException("결제 처리 중 예외 발생 : " + e.getMessage());
        } finally {
            if (isLocked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }




}
