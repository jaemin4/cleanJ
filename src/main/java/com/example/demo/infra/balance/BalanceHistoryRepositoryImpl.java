package com.example.demo.infra.balance;


import com.example.demo.domain.balance.BalanceHistory;
import com.example.demo.domain.balance.BalanceHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BalanceHistoryRepositoryImpl implements BalanceHistoryRepository {

    private final BalanceHistoryJpaRepository balanceHistoryJpaRepository;

    @Override
    public void save(BalanceHistory balanceHistory) {
        balanceHistoryJpaRepository.save(balanceHistory);
    }
}
