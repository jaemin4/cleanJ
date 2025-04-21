package com.example.demo.domain.stock;

import com.example.demo.infra.stock.StockJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StockService {
    private final StockJpaRepository stockRepository;

    public void deductStock(StockCommand.DeductStock command) {
        command.getProducts().forEach(product -> {
            Stock stock = stockRepository.findByProductId(product.getProductId());
            stock.deduct(product.getQuantity().intValue());
            stockRepository.save(stock);
        });
    }

    public void recoveryStock(StockCommand.RecoveryStock command){
        command.getProducts().forEach(product -> {
            Stock stock = stockRepository.findByProductId(product.getProductId());
            stock.recovery(product.getQuantity().intValue());
            stockRepository.save(stock);
        });
    }

}
