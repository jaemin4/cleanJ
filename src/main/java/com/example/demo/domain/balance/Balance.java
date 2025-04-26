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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "balance_id")
    private Long balanceId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Version
    private Long version;

    private Balance(Long userId, Long amount) {
        this.userId = userId;
        this.amount = amount;
    }

    public static Balance create(Long userId, long amount) {
        if(amount <=0){
            throw new IllegalArgumentException("요청 금액이 잘못되었습니다.");
        }

        return new Balance(userId, amount);
    }

    public void charge(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
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
