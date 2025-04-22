package com.example.demo.concurrency;

import com.example.demo.domain.stock.Stock;
import com.example.demo.domain.stock.StockCommand;
import com.example.demo.domain.stock.StockService;
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

    @DisplayName("재고 감소 정상 작동 테스트")
    @Test
    public void 재고감소_테스트() {
        // given
        Long productId = 1L;
        int initialQuantity = 100;
        long deductQuantity = 10;

        // 재고 등록
        Stock stock = Stock.create(productId, initialQuantity);
        stockJpaRepository.save(stock);

        // when
        StockCommand.DeductStock command = StockCommand.DeductStock.of(
                List.of(StockCommand.OrderProduct.of(productId, deductQuantity))
        );
        stockService.deductStock(command);

        // then
        Stock updated = stockJpaRepository.findByProductId(productId).get();
        assertThat(updated.getQuantity()).isEqualTo(initialQuantity - deductQuantity);
    }

    @DisplayName("재고 감소 동시성 문제 재현")
    @Test
    public void 재고_동시_감소_테스트() throws InterruptedException {
        // given
        Long productId = 55L;
        int initialQuantity = 100;
        Long deductQuantity = 1L;
        int threadCount = 100;

        // 초기 재고 설정
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
        Stock updated = stockJpaRepository.findByProductId(productId).get();
        System.out.println("남은 수량: " + updated.getQuantity());

        // 동시성 문제 발생 시 0이 아닐 수 있음 (원래는 0이 되어야 정상)
        assertThat(updated.getQuantity()).isNotEqualTo(0);
    }



}
