package com.example.demo.domain.stock;

import com.example.demo.infra.stock.StockJpaRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class StockService {
    private final StockJpaRepository stockRepository;
    private final RedissonClient redissonClient;
    private final StockTransactionService stockTransactionService;

    public void deductStock(StockCommand.DeductStock command) {
        String lockKey = "lock:stock:deduct";
        RLock lock = redissonClient.getLock(lockKey);

        boolean isLocked = false;
        try {
            isLocked = lock.tryLock(5, 3, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new IllegalStateException("락 획득 실패: 동시 요청 과다");
            }

            stockTransactionService.deductStockWithTransaction(command);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("락 대기 중 인터럽트 발생", e);
        } finally {
            if (isLocked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
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
