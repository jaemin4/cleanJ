package com.example.demo.integrate;

import com.example.demo.application.order.OrderCriteria;
import com.example.demo.application.order.OrderFacade;
import com.example.demo.application.order.OrderResult;
import com.example.demo.application.payment.PaymentCriteria;
import com.example.demo.application.payment.PaymentFacade;
import com.example.demo.domain.balance.Balance;
import com.example.demo.domain.balance.BalanceRepository;
import com.example.demo.domain.payment.PaymentHistoryInfo.Top5OrdersForCaching;
import com.example.demo.domain.payment.PaymentHistoryRepository;
import com.example.demo.domain.payment.PaymentHistoryService;
import com.example.demo.domain.product.Product;
import com.example.demo.domain.product.ProductRepository;
import com.example.demo.domain.product.ProductSellingStatus;
import com.example.demo.domain.stock.Stock;
import com.example.demo.infra.stock.StockJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.example.demo.infra.payment.PaymentPopularScheduler.POPULAR_PRODUCTS_KEY;
import static com.example.demo.support.util.Utils.toJson;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class PopularTest {

    @Autowired private PaymentHistoryService service;
    @Autowired private PaymentFacade paymentFacade;
    @Autowired private OrderFacade orderFacade;
    @Autowired private BalanceRepository balanceRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private StockJpaRepository stockRepository;
    @Autowired private PaymentHistoryRepository paymentHistoryRepository;
    @Autowired private RedisTemplate<String, Object> redisTemplate;
    @Autowired private RedissonClient redissonClient;

    @DisplayName("20명 동시 주문/결제 후 인기상품 랭킹 조회")
    @Test
    void 동시_주문_후_인기상품_랭킹조회() throws InterruptedException {
        Product p1 = Product.create("딸기케이크", 3000L, ProductSellingStatus.SELLING);
        Product p2 = Product.create("마카롱", 1500L, ProductSellingStatus.SELLING);
        productRepository.saveAll(List.of(p1, p2));

        stockRepository.saveAll(List.of(
                new Stock(p1.getId(), 1),
                new Stock(p2.getId(), 1)
        ));

        for (long userId = 1; userId <= 20; userId++) {
            balanceRepository.save(Balance.create(userId, 100_000_000L));
        }

        ExecutorService executor = Executors.newFixedThreadPool(4);
        for (long userId = 1; userId <= 20; userId++) {
            long uid = userId;
            executor.submit(() -> {
                try {
                    var order = orderFacade.order(OrderCriteria.Order.of(uid, List.of(
                            OrderCriteria.OrderProduct.of(p1.getId(), 1L))));
                    paymentFacade.pay(PaymentCriteria.Payment.of(order.getOrderId(), uid, null));
                } catch (Exception ignored) {}
            });
        }
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        List<Top5OrdersForCaching> top5 = service.getPopularProducts();
        System.out.println("Redis 랭킹: " + toJson(top5));
        assertThat(top5.getFirst().getCount())
                .isEqualTo(paymentHistoryRepository.findTop5OrdersByPaidStatus().size());
    }

    @DisplayName("캐시 미스 시 DB 조회 후 Redis 저장")
    @Test
    void 캐시미스_발생시_DB조회후_Redis저장() {
        Product p = Product.create("딸기케이크", 3000L, ProductSellingStatus.SELLING);
        productRepository.save(p);
        stockRepository.save(new Stock(p.getId(), 10));

        for (long userId = 1; userId <= 5; userId++) {
            balanceRepository.save(Balance.create(userId, 100_000L));
            var order = orderFacade.order(OrderCriteria.Order.of(userId,
                    List.of(OrderCriteria.OrderProduct.of(p.getId(), 1L))));
            paymentFacade.pay(PaymentCriteria.Payment.of(order.getOrderId(), userId, null));
        }

        redisTemplate.delete(POPULAR_PRODUCTS_KEY);
        List<Top5OrdersForCaching> top5 = service.getPopularProducts();
        assertThat(top5.getFirst().getCount())
                .isEqualTo(paymentHistoryRepository.findTop5OrdersByPaidStatus().size());
    }

    @Test
    @DisplayName("락 획득 실패 후 캐시 재시도 성공")
    void 캐시미스_락획득실패시_재시도_후_캐시존재시_정상반환() throws InterruptedException {
        redisTemplate.delete(POPULAR_PRODUCTS_KEY);
        RLock lock = redissonClient.getLock("lock:popular:top5");
        lock.lock();

        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            redisTemplate.opsForZSet().add(POPULAR_PRODUCTS_KEY, "3", 30.0);
        }, 50, TimeUnit.MILLISECONDS);

        for (int i = 0; i < 10; i++) {
            Set<ZSetOperations.TypedTuple<Object>> cached = redisTemplate.opsForZSet()
                    .rangeWithScores(POPULAR_PRODUCTS_KEY, 0, 4);
            if (cached != null && !cached.isEmpty()) break;
            Thread.sleep(50);
        }

        List<Top5OrdersForCaching> result = service.getPopularProducts();
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getOrderId()).isEqualTo(3L);

        lock.unlock();
    }

    @Test
    @DisplayName("락 획득 실패 후 캐시 없음 예외")
    void 캐시미스_락획득실패_및_재시도후_캐시없을때_예외발생() throws InterruptedException {
        redisTemplate.delete(POPULAR_PRODUCTS_KEY);

        RLock lock = redissonClient.getLock("lock:popular:top5");
        assertThat(lock.isLocked()).isTrue();

        Thread testThread = new Thread(() -> {
            assertThatThrownBy(() -> service.getPopularProducts())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ZSET 캐시 초기화 대기 중 실패");
        });

        try {
            testThread.start();
            testThread.join();
        } finally {
            lock.unlock();
        }
    }





}
