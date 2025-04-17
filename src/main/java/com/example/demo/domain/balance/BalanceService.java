package com.example.demo.domain.balance;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BalanceService {

    private final BalanceRepository balanceRepository;
    private final BalanceHistoryRepository balanceHistoryRepository;

    @Transactional
    public void charge(BalanceCommand.Charge command) {
        log.info("[BalanceService] 충전 요청: userId={}, amount={}", command.getUserId(), command.getAmount());
        Balance balance = balanceRepository.findByUserId(command.getUserId())
                .map(b -> {
                    log.info("[BalanceService] 기존 잔액 존재. 기존 금액: {}, 충전 금액: {}", b.getAmount(), command.getAmount());
                    b.charge(command.getAmount());
                    return balanceRepository.save(b);
                })
                .orElseGet(() -> {
                    log.info("[BalanceService] 잔액 정보 없음. 새 잔액 객체 생성: userId={}, amount={}", command.getUserId(), command.getAmount());
                    Balance newBalance = Balance.create(command.getUserId(), command.getAmount());
                    return balanceRepository.save(newBalance);
                });

        log.info("[BalanceService] 최종 잔액: userId={}, balanceId={}, amount={}", command.getUserId(), balance.getBalanceId(), balance.getAmount());

        balanceHistoryRepository.save(
                BalanceHistory.charge(balance.getBalanceId(), command.getAmount())
        );

        log.info("[BalanceService] 충전 이력 저장 완료: balanceId={}, amount={}", balance.getBalanceId(), command.getAmount());
    }



    @Transactional
    public void use(BalanceCommand.Use command) {
        Balance balance = balanceRepository.findByUserId(command.getUserId())
                .map(b -> {
                    b.use(command.getAmount());
                    return balanceRepository.save(b);
                })
                .orElseThrow(() -> new RuntimeException("잔액이 존재하지 않습니다."));


        balanceHistoryRepository.save(
                BalanceHistory.use(balance.getBalanceId(), command.getAmount())
        );

    }



}
