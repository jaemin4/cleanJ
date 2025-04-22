package com.example.demo.concurrency;

import com.example.demo.domain.product.Product;
import com.example.demo.domain.product.ProductSellingStatus;
import com.example.demo.domain.stock.Stock;
import com.example.demo.domain.stock.StockCommand;
import com.example.demo.domain.stock.StockService;
import com.example.demo.infra.product.ProductJpaRepository;
import com.example.demo.infra.stock.StockJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
public class StockTest {

    @Autowired
    private StockService stockService;

    @Autowired
    private StockJpaRepository stockJpaRepository;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @DisplayName("재고 감소 동시성 제어 성공")
    @Test
    public void 재고_동시_감소_성공_테스트() throws InterruptedException {
        // given: 테스트용 상품과 재고 등록
        Product product = Product.create("TestProduct", 10000, ProductSellingStatus.SELLING);

        productJpaRepository.save(product);

        Long productId = product.getId(); // 실제 DB에 저장된 ID
        int initialQuantity = 100;
        Long deductQuantity = 1L;
        int threadCount = 100;

        Stock stock = Stock.create(productId, initialQuantity);
        stockJpaRepository.save(stock);

        ExecutorService executorService = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.execute(() -> {
                try {
                    StockCommand.DeductStock command = StockCommand.DeductStock.of(
                            List.of(StockCommand.OrderProduct.of(productId, deductQuantity))
                    );
                    stockService.deductStock(command);
                } catch (Exception e) {
                    System.out.println("예외 발생: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        // then
        Stock updated = stockJpaRepository.findByProductId(productId).orElseThrow();
        System.out.println("남은 수량: " + updated.getQuantity());
        assertThat(updated.getQuantity()).isEqualTo(0);
    }



}
