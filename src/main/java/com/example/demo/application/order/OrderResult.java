package com.example.demo.application.order;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class OrderResult {

    @Getter
    public static class Order {
        private final Long orderId;
        private final double totalPrice;

        private Order(Long orderId,double totalPrice) {
            this.orderId = orderId;
            this.totalPrice = totalPrice;
        }

        public static Order of(Long orderId, double totalPrice) {
            return new Order(orderId,totalPrice);
        }
    }
}
