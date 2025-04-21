package com.example.demo.integrate;

import com.example.demo.domain.balance.Balance;
import com.example.demo.domain.balance.BalanceCommand;
import com.example.demo.domain.balance.BalanceRepository;
import com.example.demo.domain.balance.BalanceService;
import com.example.demo.infra.balance.BalanceHistoryJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
public class BalanceTest {
    @Autowired
    private BalanceService balanceService;

    @Autowired
    private BalanceRepository balanceRepository;

    @Autowired
    private BalanceHistoryJpaRepository balanceHistoryJpaRepository;

    private final long userId = 30L;

    @BeforeEach
    void setUp() {
        // 초기 잔액 10,000원 충전
        balanceService.charge(BalanceCommand.Charge.of(userId, 10_000L));
    }

    @AfterEach
    void tearDown() {
        balanceRepository.findByUserId(userId).ifPresent(balance -> {
            balance.testSetAmount(0L); // 테스트용 메서드: 잔액 초기화
            balanceRepository.save(balance);
        });
    }

    @Test
    @DisplayName("잔액 충전 테스트")
    void testCharge() {
        // given
        long chargeAmount = 5_000L;

        // when
        balanceService.charge(BalanceCommand.Charge.of(userId, chargeAmount));

        // then
        Balance balance = balanceRepository.findByUserId(userId).orElseThrow();
        assertThat(balance.getAmount()).isEqualTo(15_000L);
    }

    @Test
    @DisplayName("잔액 사용 성공 테스트")
    void testUseSuccess() {
        // given
        long useAmount = 7_000L;

        // when
        balanceService.use(BalanceCommand.Use.of(userId, useAmount));

        // then
        Balance balance = balanceRepository.findByUserId(userId).orElseThrow();
        assertThat(balance.getAmount()).isEqualTo(3_000L);
    }

    @Test
    @DisplayName("잔액 부족 시 예외 발생 테스트")
    void testUseFail_InsufficientBalance() {
        // given
        long overAmount = 20_000L;

        // expect
        assertThatThrownBy(() ->
                balanceService.use(BalanceCommand.Use.of(userId, overAmount))
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("잔액이 부족");
    }

    @Test
    @DisplayName("존재하지 않는 유저의 잔액 사용 시 예외 발생 테스트")
    void testUseFail_UserNotFound() {
        long unknownUserId = 999L;

        assertThatThrownBy(() ->
                balanceService.use(BalanceCommand.Use.of(unknownUserId, 1_000L))
        ).isInstanceOf(RuntimeException.class)
                .hasMessageContaining("잔액이 존재하지 않습니다");
    }

    @Test
    @DisplayName("충전 시 이력 저장 테스트")
    void testChargeHistory() {
        long chargeAmount = 3_000L;
        int beforeSize = balanceHistoryJpaRepository.findAll().size();

        balanceService.charge(BalanceCommand.Charge.of(userId, chargeAmount));

        int afterSize = balanceHistoryJpaRepository.findAll().size();
        assertThat(afterSize).isEqualTo(beforeSize + 1);
    }

    @Test
    @DisplayName("사용 시 이력 저장 테스트")
    void testUseHistory() {
        long useAmount = 2_000L;
        int beforeSize = balanceHistoryJpaRepository.findAll().size();

        balanceService.use(BalanceCommand.Use.of(userId, useAmount));

        int afterSize = balanceHistoryJpaRepository.findAll().size();
        assertThat(afterSize).isEqualTo(beforeSize + 1);
    }



}
