package com.example.demo.infra.payment;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class PaymentMockRequest {

    @NoArgsConstructor
    @Getter
    public static class Mock {
        private Long orderId;
        private Long userId;
        private Long amount;

        private Mock(Long orderId, Long userId, Long amount) {
            this.orderId = orderId;
            this.userId = userId;
            this.amount = amount;
        }

        public static Mock of(Long orderId, Long userId, Long amount) {
            return new Mock(orderId, userId, amount);
        }
    }
}
