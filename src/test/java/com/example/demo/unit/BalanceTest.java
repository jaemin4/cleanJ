package com.example.demo.unit;

import com.example.demo.domain.balance.*;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Optional;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BalanceTest {
    @InjectMocks
    private BalanceService balanceService;

    @Mock
    private BalanceRepository balanceRepository;

    @Mock
    private BalanceHistoryRepository balanceHistoryRepository;

    @BeforeEach
    void setUp(){
        MockitoAnnotations.openMocks(this);
    }

    @DisplayName("새계좌 잔액 충전이 정상적으로 완료된다.")
    @Test
    void charge_1() {
        // given
        Long userId = 1L;
        long chargeAmount = 10_000L;
        BalanceCommand.Charge command = BalanceCommand.Charge.of(userId, chargeAmount);
        when(balanceRepository.findByUserId(userId)).thenReturn(Optional.empty());
        ArgumentCaptor<Balance> captor = ArgumentCaptor.forClass(Balance.class);
        when(balanceRepository.save(any(Balance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        balanceService.charge(command);

        // then
        verify(balanceRepository).save(captor.capture());
        Balance savedBalance = captor.getValue();
        Assertions.assertThat(savedBalance.getAmount()).isEqualTo(chargeAmount);
    }

    @DisplayName("기존 계좌 잔액 충전이 정상적으로 완료된다.")
    @Test
    void charge_2() {
        // given
        Long userId = 1L;
        long chargeAmount = 10_000L;
        Balance existingBalance = Balance.create(userId, 5_000L);
        when(balanceRepository.findByUserId(userId)).thenReturn(Optional.of(existingBalance));
        when(balanceRepository.save(existingBalance)).thenReturn(existingBalance);

        BalanceCommand.Charge command = BalanceCommand.Charge.of(userId, chargeAmount);

        // when
        balanceService.charge(command);

        // then
        verify(balanceRepository).save(existingBalance);
        assertThat(existingBalance.getAmount()).isEqualTo(15_000L);
    }

    @DisplayName("새계좌 충전금액이 0 이하일 때 예외가 발생한다.")
    @Test
    void charge_3() {
        // given
        Long userId = 2L;
        long chargeAmount = 0;

        BalanceCommand.Charge command = BalanceCommand.Charge.of(userId, chargeAmount);

        // when & then
        assertThatThrownBy(() -> balanceService.charge(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("요청 금액이 잘못되었습니다.");

    }

    @DisplayName("계좌가 없을 때 사용하면 예외가 발생한다.")
    @Test
    void use_1() {
        // given
        Long userId = 1L;
        long amount = 5_000L;
        BalanceCommand.Use command = BalanceCommand.Use.of(userId, amount);

        when(balanceRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> balanceService.use(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("계좌가 존재하지 않습니다.");
    }

    @DisplayName("잔액이 정상적으로 사용된다.")
    @Test
    void use_2() {
        // given
        Long userId = 2L;
        long initialAmount = 10_000L;
        long useAmount = 3_000L;

        Balance balance = Balance.create(userId, initialAmount);
        BalanceCommand.Use command = BalanceCommand.Use.of(userId, useAmount);

        when(balanceRepository.findByUserId(userId)).thenReturn(Optional.of(balance));
        when(balanceRepository.save(balance)).thenReturn(balance);

        // when
        balanceService.use(command);

        // then
        assertThat(balance.getAmount()).isEqualTo(7_000L);
        verify(balanceRepository).save(balance);
    }

    @DisplayName("잔액이 부족하면 예외가 발생한다.")
    @Test
    void use_3() {
        // given
        Long userId = 3L;
        long initialAmount = 2_000L;
        long useAmount = 5_000L;

        Balance balance = Balance.create(userId, initialAmount);
        BalanceCommand.Use command = BalanceCommand.Use.of(userId, useAmount);

        when(balanceRepository.findByUserId(userId)).thenReturn(Optional.of(balance));

        // when & then
        assertThatThrownBy(() -> balanceService.use(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("잔액이 부족합니다.");
    }

    @DisplayName("잔액 사용 시 이력이 정상적으로 저장된다.")
    @Test
    void history_1() {
        // given
        Long userId = 2L;
        long initialAmount = 10_000L;
        long useAmount = 3_000L;

        Balance balance = Balance.create(userId, initialAmount);
        BalanceCommand.Use command = BalanceCommand.Use.of(userId, useAmount);

        when(balanceRepository.findByUserId(userId)).thenReturn(Optional.of(balance));
        when(balanceRepository.save(balance)).thenReturn(balance);

        ArgumentCaptor<BalanceHistory> historyCaptor = ArgumentCaptor.forClass(BalanceHistory.class);

        // when
        balanceService.use(command);

        // then
        verify(balanceHistoryRepository).save(historyCaptor.capture());

        BalanceHistory history = historyCaptor.getValue();
        assertThat(history.getType()).isEqualTo(BalanceHistoryType.USE);
        assertThat(history.getAmount()).isEqualTo(useAmount);
        assertThat(history.getBalanceId()).isEqualTo(balance.getBalanceId());
    }

    @DisplayName("잔액 충전 시 이력이 정상적으로 저장된다.")
    @Test
    void history_2() {
        // given
        Long userId = 10L;
        long chargeAmount = 20_000L;
        BalanceCommand.Charge command = BalanceCommand.Charge.of(userId, chargeAmount);
        Balance createdBalance = Balance.create(userId, chargeAmount);
        ReflectionTestUtils.setField(createdBalance, "balanceId", 99L);
        when(balanceRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(balanceRepository.save(any(Balance.class))).thenReturn(createdBalance);
        ArgumentCaptor<BalanceHistory> captor = ArgumentCaptor.forClass(BalanceHistory.class);

        // when
        balanceService.charge(command);

        // then
        verify(balanceHistoryRepository).save(captor.capture());
        BalanceHistory savedHistory = captor.getValue();

        assertThat(savedHistory.getType()).isEqualTo(BalanceHistoryType.CHARGE);
        assertThat(savedHistory.getAmount()).isEqualTo(chargeAmount);
        assertThat(savedHistory.getBalanceId()).isEqualTo(99L);
    }

}
