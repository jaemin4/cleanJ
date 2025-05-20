package com.example.demo.domain.stock;

import com.example.demo.support.comm.aop.DistributedLock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class StockService {
    private final StockTransactionService stockTransactionService;

    @DistributedLock(key = "'lock:stock:deduct'", waitTime = 5, leaseTime = 3)
    public void deductStock(StockCommand.DeductStock command) {
        stockTransactionService.deductStockWithTransaction(command);
    }

    @DistributedLock(key = "'lock:stock:recovery'", waitTime = 5, leaseTime = 3)
    public void recoveryStock(StockCommand.RecoveryStock command) {
        stockTransactionService.recoveryStockWithTransaction(command);
    }




}
