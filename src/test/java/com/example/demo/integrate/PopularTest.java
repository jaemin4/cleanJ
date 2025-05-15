package com.example.demo.integrate;

import com.example.demo.application.order.OrderCriteria;
import com.example.demo.application.order.OrderFacade;
import com.example.demo.application.order.OrderResult;
import com.example.demo.application.payment.PaymentCriteria;
import com.example.demo.application.payment.PaymentFacade;
import com.example.demo.domain.balance.Balance;
import com.example.demo.domain.balance.BalanceRepository;
import com.example.demo.domain.payment.PaymentHistoryInfo;
import com.example.demo.domain.payment.PaymentHistoryRepository;
import com.example.demo.domain.payment.PaymentHistoryService;
import com.example.demo.domain.product.Product;
import com.example.demo.domain.product.ProductRepository;
import com.example.demo.domain.product.ProductSellingStatus;
import com.example.demo.domain.stock.Stock;
import com.example.demo.infra.payment.ResTopOrderFive;
import com.example.demo.infra.stock.StockJpaRepository;
import com.example.demo.support.util.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.testcontainers.shaded.com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;

import static com.example.demo.infra.payment.PaymentPopularScheduler.POPULAR_PRODUCTS_KEY;
import static com.example.demo.support.util.Utils.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
public class PopularTest {

    @Autowired
    private PaymentHistoryService paymentHistoryService;

    @Autowired
    private PaymentFacade paymentFacade;

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    BalanceRepository balanceRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    StockJpaRepository stockRepository;

    @Autowired
    PaymentHistoryRepository paymentHistoryRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private static final String POPULAR_PRODUCTS_KEY = "popular:products:zset";
    @Autowired
    private ObjectMapper objectMapper;

    @DisplayName("20명 동시 주문/결제 후 인기상품 랭킹 조회")
    @Test
    void 동시성_인기상품_테스트() throws InterruptedException {
        Product p1 = Product.create("딸기케이크", 3000L, ProductSellingStatus.SELLING);
        Product p2 = Product.create("마카롱", 1500L, ProductSellingStatus.SELLING);
        productRepository.saveAll(List.of(p1, p2));
        Long productId1 = p1.getId();
        Long productId2 = p2.getId();

        stockRepository.saveAll(List.of(
                new Stock(productId1, 1),
                new Stock(productId2, 1)
        ));

        for (long userId = 1; userId <= 20; userId++) {
            balanceRepository.save(Balance.create(userId, 100_000_000L));
        }

        int threadCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Runnable> tasks = new ArrayList<>();
        for (long userId = 1; userId <= 20; userId++) {
            long uid = userId;
            tasks.add(() -> {
                try {
                    OrderResult.Order orderResult = orderFacade.order(OrderCriteria.Order.of(
                            uid,
                            List.of(OrderCriteria.OrderProduct.of(productId1, 1L))
                    ));
                    paymentFacade.pay(PaymentCriteria.Payment.of(
                            orderResult.getOrderId(),
                            uid,
                            null
                    ));
                } catch (Exception e) {

                }
            });
        }
        tasks.forEach(executor::submit);
        executor.shutdown();
        executor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS);

        List<PaymentHistoryInfo.Top5OrdersForCaching> top5 = paymentHistoryService.getPopularProducts();
        System.out.println("Redis 랭킹: " + toJson(top5));
        long redisCount = top5.getFirst().getCount();

        long dbCount = paymentHistoryRepository.findTop5OrdersByPaidStatus().size();
        System.out.println("DB 결제 건수: " + dbCount);

        org.assertj.core.api.Assertions.assertThat(redisCount).isEqualTo(dbCount);
    }

    @DisplayName("캐시 미스 발생 시 DB에서 조회 후 Redis에 캐싱되는지 테스트")
    @Test
    void 캐시미스_테스트() throws Exception {
        Product p = Product.create("딸기케이크", 3000L, ProductSellingStatus.SELLING);
        productRepository.save(p);
        stockRepository.save(new Stock(p.getId(), 10));

        for (long userId = 1; userId <= 5; userId++) {
            balanceRepository.save(Balance.create(userId, 100_000L));

            OrderResult.Order orderResult = orderFacade.order(OrderCriteria.Order.of(
                    userId, List.of(OrderCriteria.OrderProduct.of(p.getId(), 1L))
            ));

            paymentFacade.pay(PaymentCriteria.Payment.of(orderResult.getOrderId(), userId, null));
        }

        redisTemplate.delete(POPULAR_PRODUCTS_KEY);
        List<PaymentHistoryInfo.Top5OrdersForCaching> result = paymentHistoryService.getPopularProducts();
        System.out.println("조회 결과: " + toJson(result));

        String redisRaw = redisTemplate.opsForValue().get(POPULAR_PRODUCTS_KEY);
        System.out.println("조회 결과: " + redisRaw);
        assertThat(redisRaw).isNotNull();

        String expectedJson = objectMapper.writeValueAsString(result);
        assertThat(redisRaw).isEqualTo(expectedJson);
    }








}
