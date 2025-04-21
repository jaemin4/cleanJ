package com.example.demo.infra.balance;

import com.example.demo.domain.balance.BalanceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BalanceHistoryJpaRepository extends JpaRepository<BalanceHistory, Long> {


}
