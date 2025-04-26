package com.example.demo.domain.stock;

import com.example.demo.infra.stock.StockJpaRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockService {
    private final StockJpaRepository stockRepository;

    @Transactional
    public void deductStock(StockCommand.DeductStock command) {
        command.getProducts().forEach(product -> {
            Stock stock = stockRepository.findWithPessimisticLock(product.getProductId()).orElseThrow(
                    () -> new IllegalStateException("Stock does not exist")
            );
            stock.deduct(product.getQuantity().intValue());
            stockRepository.save(stock);
        });
    }

    @Transactional
    public void recoveryStock(StockCommand.RecoveryStock command){
        command.getProducts().forEach(product -> {
            Stock stock = findStockOrThrow(product.getProductId());
            stock.recovery(product.getQuantity().intValue());
            stockRepository.save(stock);
        });
    }

    private Stock findStockOrThrow(Long productId) {
        return stockRepository.findByProductId(productId)
                .orElseThrow(() -> new RuntimeException(
                        "해당 상품의 재고가 존재하지 않습니다. productId=" + productId
                ));
    }

}
