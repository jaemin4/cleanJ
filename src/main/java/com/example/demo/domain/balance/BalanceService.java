package com.example.demo.domain.balance;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BalanceService {

    private final BalanceRepository balanceRepository;
    private final BalanceHistoryRepository balanceHistoryRepository;

    @Transactional
    public void charge(BalanceCommand.Charge command) {
        try {
            Balance balance = balanceRepository.findByUserId(command.getUserId())
                    .map(b -> {
                        b.charge(command.getAmount());
                        return balanceRepository.save(b);
                    })
                    .orElseGet(() -> {
                        Balance newBalance = Balance.create(command.getUserId(), command.getAmount());
                        return balanceRepository.save(newBalance);
                    });

            balanceHistoryRepository.save(
                    BalanceHistory.charge(balance.getBalanceId(), command.getAmount())
            );
        }catch(ObjectOptimisticLockingFailureException e) {
            log.error("충전 중 낙관적 락 충돌 발생: userId={}, amount={}", command.getUserId(), command.getAmount());
            throw new RuntimeException("잔액 충전 중 다른 요청과 충돌했습니다. 다시 시도해주세요.", e);
        }
    }

    @Transactional
    public void use(BalanceCommand.Use command) {
        try{
            Balance balance = balanceRepository.findByUserId(command.getUserId())
                    .map(b -> {
                        b.use(command.getAmount());
                        return balanceRepository.save(b);
                    })
                    .orElseThrow(() -> new RuntimeException("계좌가 존재하지 않습니다."));
            balanceHistoryRepository.save(
                    BalanceHistory.use(balance.getBalanceId(), command.getAmount())
            );
        }catch (ObjectOptimisticLockingFailureException e) {
            log.error("잔액 사용 중 낙관적 락 충돌 발생: userId={}, amount={}", command.getUserId(), command.getAmount());
            throw new RuntimeException("잔액 사용 중 다른 요청과 충돌했습니다. 다시 시도해주세요.", e);
        }
    }


}
