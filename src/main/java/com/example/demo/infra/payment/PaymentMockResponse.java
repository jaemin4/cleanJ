package com.example.demo.infra.payment;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentMockResponse {

    @Getter
    public static class MockPay {

        private final String transactionId;
        private final String status;
        private final String message;

        private MockPay(String transactionId, String status, String message) {
            this.transactionId = transactionId;
            this.status = status;
            this.message = message;
        }

        public static MockPay of(String transactionId, String status, String message) {
            return new MockPay(transactionId, status, message);
        }
    }


}
