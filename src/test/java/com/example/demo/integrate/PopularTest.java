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
import com.example.demo.infra.stock.StockJpaRepository;
import com.example.demo.support.util.Utils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;

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
        System.out.println("Redis 랭킹: " + Utils.toJson(top5));
        long redisCount = top5.getFirst().getCount();

        long dbCount = paymentHistoryRepository.findTop5OrdersByPaidStatus().size();
        System.out.println("DB 결제 건수: " + dbCount);

        org.assertj.core.api.Assertions.assertThat(redisCount).isEqualTo(dbCount);
    }



}
