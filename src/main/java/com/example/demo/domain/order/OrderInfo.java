package com.example.demo.domain.order;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OrderInfo {

    @Getter
    public static class CreateOrder {
        private final Long orderId;
        private final double finalTotalAmount;

        private CreateOrder(Long orderId, double finalTotalAmount) {
            this.orderId = orderId;
            this.finalTotalAmount = finalTotalAmount;
        }

        public static CreateOrder of(Long orderId, double finalTotalAmount) {
            return new CreateOrder(orderId, finalTotalAmount);
        }
    }

}