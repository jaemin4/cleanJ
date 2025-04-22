package com.example.demo.unit;

import com.example.demo.domain.stock.Stock;
import com.example.demo.domain.stock.StockCommand;
import com.example.demo.domain.stock.StockService;
import com.example.demo.infra.stock.StockJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StockServiceTest {

    @InjectMocks
    private StockService stockService;

    @Mock
    private StockJpaRepository stockRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @DisplayName("재고 차감: 충분한 재고가 있을 때 정상적으로 차감하고 저장한다.")
    @Test
    void deductStock_success() {
        // given
        Long productId = 10L;
        int initialQty = 5;
        int deductQty = 3;
        Stock stock = Stock.create(productId, initialQty);

        when(stockRepository.findByProductId(productId)).thenReturn(Optional.of(stock));
        when(stockRepository.save(stock)).thenReturn(stock);

        StockCommand.OrderProduct op = StockCommand.OrderProduct.of(productId, (long) deductQty);
        StockCommand.DeductStock command = StockCommand.DeductStock.of(List.of(op));

        // when
        stockService.deductStock(command);

        // then
        ArgumentCaptor<Stock> captor = ArgumentCaptor.forClass(Stock.class);
        verify(stockRepository).save(captor.capture());
        Stock saved = captor.getValue();
        assertThat(saved.getQuantity()).isEqualTo(initialQty - deductQty);
    }

    @DisplayName("재고 차감: 재고가 부족하면 예외가 발생한다.")
    @Test
    void deductStock_insufficient() {
        // given
        Long productId = 20L;
        int initialQty = 2;
        int deductQty = 5;
        Stock stock = Stock.create(productId, initialQty);

        when(stockRepository.findByProductId(productId)).thenReturn(Optional.of(stock));

        StockCommand.OrderProduct op = StockCommand.OrderProduct.of(productId, (long) deductQty);
        StockCommand.DeductStock command = StockCommand.DeductStock.of(List.of(op));

        // when & then
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> stockService.deductStock(command)
        );
        assertThat(ex.getMessage()).contains("재고가 부족합니다.");

        // ensure save is never called
        verify(stockRepository, never()).save(any());
    }

    @DisplayName("재고 차감: 재고 기록이 없으면 예외가 발생한다.")
    @Test
    void deductStock_noRecord() {
        // given
        Long missingProductId = 99L;
        when(stockRepository.findByProductId(missingProductId))
                .thenReturn(Optional.empty());

        StockCommand.OrderProduct op = StockCommand.OrderProduct.of(missingProductId, 1L);
        StockCommand.DeductStock command = StockCommand.DeductStock.of(List.of(op));

        // when & then
        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> stockService.deductStock(command)
        );
        assertThat(ex.getMessage()).contains("해당 상품의 재고가 존재하지 않습니다.");

        verify(stockRepository, never()).save(any());
    }

    @DisplayName("재고 복구: 정상적으로 복구하고 저장한다.")
    @Test
    void recoveryStock_success() {
        // given
        Long productId = 30L;
        int initialQty = 4;
        int recoveryQty = 2;
        Stock stock = Stock.create(productId, initialQty);

        when(stockRepository.findByProductId(productId)).thenReturn(Optional.of(stock));
        when(stockRepository.save(stock)).thenReturn(stock);

        StockCommand.OrderProduct op = StockCommand.OrderProduct.of(productId, (long) recoveryQty);
        StockCommand.RecoveryStock command = StockCommand.RecoveryStock.of(List.of(op));

        // when
        stockService.recoveryStock(command);

        // then
        ArgumentCaptor<Stock> captor = ArgumentCaptor.forClass(Stock.class);
        verify(stockRepository).save(captor.capture());
        Stock saved = captor.getValue();
        assertThat(saved.getQuantity()).isEqualTo(initialQty + recoveryQty);
    }

    @DisplayName("재고 복구: 재고 기록이 없으면 예외가 발생한다.")
    @Test
    void recoveryStock_noRecord() {
        // given
        Long missingProductId = 55L;
        when(stockRepository.findByProductId(missingProductId)).thenReturn(Optional.empty());

        StockCommand.OrderProduct op = StockCommand.OrderProduct.of(missingProductId, 3L);
        StockCommand.RecoveryStock command = StockCommand.RecoveryStock.of(List.of(op));

        // when & then
        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> stockService.recoveryStock(command)
        );
        assertThat(ex.getMessage()).contains("해당 상품의 재고가 존재하지 않습니다.");

        verify(stockRepository, never()).save(any());
    }

    @DisplayName("Stock 생성 시 음수 재고로 생성하면 예외 발생")
    @Test
    void stock_create_negativeQuantity() {
        // when & then
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new Stock(40L, -1)
        );
        assertThat(ex.getMessage()).contains("재고 수량은 0 이상이어야 합니다.");
    }
}
