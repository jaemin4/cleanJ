package com.example.demo.domain.balance;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "t1_balance_history")
public class BalanceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "balance_history_id")
    private Long id;

    @Column(name = "balance_id", nullable = false)
    private Long balanceId;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BalanceHistoryType type;

    private BalanceHistory(Long balanceId, Long amount,BalanceHistoryType type) {
        this.balanceId = balanceId;
        this.amount = amount;
        this.type = type;
    }

    public static BalanceHistory charge(Long balanceId, Long amount) {
        return new BalanceHistory(balanceId,amount, BalanceHistoryType.CHARGE);
    }

    public static BalanceHistory use(Long balanceId, Long amount){
        return new BalanceHistory(balanceId,amount,BalanceHistoryType.USE);
    }


}
