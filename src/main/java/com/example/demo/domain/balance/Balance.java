package com.example.demo.domain.balance;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "t1_balance", indexes = {
        @Index(name = "idx_balance_userId", columnList = "user_id")
})
public class Balance {

    private static final long MAX_BALANCE_AMOUNT = 10_000_000L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "balance_id")
    private Long balanceId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "amount", nullable = false)
    private Long amount;

    private Balance(Long userId, Long amount) {
        this.userId = userId;
        this.amount = amount;
    }

    public static Balance create(Long userId, long amount) {
        return new Balance(userId, amount);
    }

    public void charge(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
        }
        if (this.amount + amount > MAX_BALANCE_AMOUNT) {
            throw new IllegalArgumentException("최대 금액을 초과할 수 없습니다.");
        }
        this.amount += amount;
    }

    public void use(long amount) {
        if (this.amount < amount) {
            throw new IllegalArgumentException("잔액이 부족합니다.");
        }
        this.amount -= amount;
    }

    public void testSetAmount(long amount) {
        this.amount = amount;
    }



}
