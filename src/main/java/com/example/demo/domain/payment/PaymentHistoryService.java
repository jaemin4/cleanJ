package com.example.demo.domain.payment;

import com.example.demo.infra.payment.ResTopOrderFive;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.example.demo.infra.payment.PaymentPopularScheduler.POPULAR_PRODUCTS_KEY;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentHistoryService {

    private final PaymentHistoryRepository paymentHistoryRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final RedissonClient redissonClient;

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

    public List<PaymentHistoryInfo.Top5OrdersForCaching> getPopularProducts() {
        Set<ZSetOperations.TypedTuple<Object>> zset =
                redisTemplate.opsForZSet().reverseRangeWithScores(POPULAR_PRODUCTS_KEY, 0, 4);

        if (zset != null && !zset.isEmpty()) {
            return mapToCachingList(zset);
        }

        RLock lock = redissonClient.getLock("lock:popular:top5");
        boolean isLocked = false;

        try {
            isLocked = lock.tryLock(5, 2, TimeUnit.SECONDS);

            if (isLocked) {
                zset = redisTemplate.opsForZSet().reverseRangeWithScores(POPULAR_PRODUCTS_KEY, 0, 4);
                if (zset != null && !zset.isEmpty()) {
                    return mapToCachingList(zset);
                }

                redisTemplate.delete(POPULAR_PRODUCTS_KEY);
                List<ResTopOrderFive> popularProducts = paymentHistoryRepository.findTop5OrdersByPaidStatus();

                for (ResTopOrderFive item : popularProducts) {
                    redisTemplate.opsForZSet().add(POPULAR_PRODUCTS_KEY, item.getOrderId(), item.getCount());
                }

                redisTemplate.expire(POPULAR_PRODUCTS_KEY, 10, TimeUnit.MINUTES); // TTL 10분 설정
                log.info("[CACHE] 인기 상품 ZSET 캐시 재등록 완료");

                return PaymentHistoryInfo.Top5OrdersForCaching.fromResTopList(popularProducts);
            } else {
                log.warn("[LOCK] 락 획득 실패 - 캐시 재시도");
                Thread.sleep(100);

                zset = redisTemplate.opsForZSet().reverseRangeWithScores(POPULAR_PRODUCTS_KEY, 0, 4);
                if (zset != null && !zset.isEmpty()) {
                    return mapToCachingList(zset);
                } else {
                    throw new IllegalStateException("캐시 초기화 대기 중에도 ZSET 없음 - 실패 처리");
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("락 대기 중 인터럽트 발생", e);
        } catch (Exception e) {
            log.error("[ERROR] 인기 상품 조회 실패: {}", e.getMessage(), e);
            throw new RuntimeException("인기 상품 조회 실패", e);
        } finally {
            if (isLocked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private List<PaymentHistoryInfo.Top5OrdersForCaching> mapToCachingList(Set<ZSetOperations.TypedTuple<Object>> zset) {
        return zset.stream()
                .map(tuple -> new PaymentHistoryInfo.Top5OrdersForCaching(
                        Long.parseLong(String.valueOf(tuple.getValue())),
                        tuple.getScore().longValue()
                ))
                .toList();
    }




}
