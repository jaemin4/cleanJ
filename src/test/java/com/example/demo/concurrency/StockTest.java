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
        StockCommand.deductStock command = StockCommand.deductStock.of(
                List.of(StockCommand.OrderProduct.of(productId, deductQuantity))
        );
        stockService.deductStock(command);

        // then
        Stock updated = stockJpaRepository.findByProductId(productId);
        assertThat(updated.getQuantity()).isEqualTo(initialQuantity - deductQuantity);
    }



}
