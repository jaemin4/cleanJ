package com.example.demo.interfaces.balance;

import com.example.demo.domain.balance.BalanceCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BalanceRequest {

    @Getter
    @NoArgsConstructor
    public static class Charge{

        @NotNull(message = "잔액은 필수입니다.")
        @Positive(message = "잔액은 이어야만 합니다.")
        private Long amount;

        @NotNull(message = "사용자 아이디는 필수입니다.")
        private Long userId;

        public BalanceCommand.Charge toCommand() {
            return BalanceCommand.Charge.of(userId,amount);
        }
    }

}
