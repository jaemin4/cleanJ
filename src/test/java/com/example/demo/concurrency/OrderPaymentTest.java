package com.example.demo.concurrency;

import com.example.demo.application.order.OrderCriteria;
import com.example.demo.application.order.OrderFacade;
import com.example.demo.application.order.OrderResult;
import com.example.demo.domain.order.Order;
import com.example.demo.domain.order.OrderRepository;
import com.example.demo.domain.order.OrderStatus;
import com.example.demo.domain.product.Product;
import com.example.demo.domain.product.ProductRepository;
import com.example.demo.domain.product.ProductSellingStatus;
import com.example.demo.domain.stock.Stock;
import com.example.demo.infra.stock.StockJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class OrderPaymentTest {

    @Autowired private OrderFacade orderFacade;
    @Autowired private ProductRepository productRepository;
    @Autowired private StockJpaRepository stockRepository;
    @Autowired private OrderRepository orderRepository;

    private final Long baseUserId = 1000L;

    @Test
    @DisplayName("여러 사용자가 동시에 주문 요청 시 모두 정상 처리되어야 한다")
    void orderFacade_concurrent_success() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final long currentUserId = baseUserId + i;

            executor.submit(() -> {
                try {
                    Product p1 = productRepository.save(Product.create("딸기케이크", 3000L, ProductSellingStatus.SELLING));
                    Product p2 = productRepository.save(Product.create("마카롱", 1500L, ProductSellingStatus.SELLING));
                    stockRepository.saveAll(List.of(
                            Stock.create(p1.getId(), 10),
                            Stock.create(p2.getId(), 10)
                    ));

                    List<OrderCriteria.OrderProduct> items = List.of(
                            OrderCriteria.OrderProduct.of(p1.getId(), 2L), // 6000
                            OrderCriteria.OrderProduct.of(p2.getId(), 3L)  // 4500
                    );
                    OrderCriteria.Order criteria = OrderCriteria.Order.of(currentUserId, items);

                    OrderResult.Order result = orderFacade.order(criteria);

                    assertThat(result).isNotNull();
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        List<Order> allOrders = orderRepository.findAll();

        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(allOrders).hasSize(threadCount);
        allOrders.forEach(order -> assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CREATED));
    }





}
