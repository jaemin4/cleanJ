package com.example.demo.domain.stock;

import com.example.demo.infra.stock.StockJpaRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockTransactionService {

    private final StockJpaRepository stockRepository;

    @Transactional
    public void deductStockWithTransaction(StockCommand.DeductStock command) {
        command.getProducts().forEach(product -> {
            Stock stock = stockRepository.findByProductId(product.getProductId())
                    .orElseThrow(() -> new IllegalStateException("재고가 존재하지 않습니다."));

            stock.deduct(product.getQuantity().intValue());
            stockRepository.save(stock);
        });
    }

    @Transactional
    public void recoveryStockWithTransaction(StockCommand.RecoveryStock command) {
            command.getProducts().forEach(product -> {
                Stock stock = stockRepository.findByProductId(product.getProductId())
                        .orElseThrow(() -> new IllegalStateException("재고가 존재하지 않습니다."));

                stock.recovery(product.getQuantity().intValue());
                stockRepository.save(stock);
            });
    }

}
