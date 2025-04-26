package com.example.demo.domain.balance;

import java.util.Optional;

public interface BalanceRepository {
    Balance save(final Balance balance);
    Optional<Balance> findByUserId(final Long userId);
    void deleteAll();
}
