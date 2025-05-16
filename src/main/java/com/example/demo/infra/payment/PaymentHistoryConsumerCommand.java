package com.example.demo.infra.payment;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentHistoryConsumerCommand {

    @Getter
    public static class Save{
        private final Long userId;
        private final long amount;
        private final String transactionId;
        private final String status;
        private final Long orderId;

        private Save(Long userId, long amount, String transactionId, String status, Long orderId) {
            this.userId = userId;
            this.amount = amount;
            this.transactionId = transactionId;
            this.status = status;
            this.orderId = orderId;
        }

        public static Save of(Long userId, long amount, String transactionId, String status, Long orderId) {
            return new Save(userId, amount, transactionId, status, orderId);
        }


    }
}
