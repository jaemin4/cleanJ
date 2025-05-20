package com.example.demo.application.payment;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentTransactionResult {

    @Getter
    public static class Payment {
        private final String transactionId;
        private final String status;
        private final long finalAmount;

        public Payment(String transactionId, String status, long finalAmount) {
            this.transactionId = transactionId;
            this.status = status;
            this.finalAmount = finalAmount;
        }

        public static Payment of(String transactionId, String status, long finalAmount) {
            return new Payment(transactionId, status, finalAmount);
        }
    }
}
