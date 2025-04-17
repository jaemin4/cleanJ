package com.example.demo.domain.balance;


import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BalanceHistoryCommand {

    @Getter
    public static class RecordHistory{
        private final Long balanceId;
        private final BalanceHistoryType type;
        private final Long amount;

        private RecordHistory(Long balanceId, BalanceHistoryType type, Long amount) {
            this.balanceId = balanceId;
            this.type = type;
            this.amount = amount;
        }

        public static RecordHistory of(Long balanceId, BalanceHistoryType type, Long amount) {
            return new RecordHistory(balanceId, type, amount);
        }

    }


}
