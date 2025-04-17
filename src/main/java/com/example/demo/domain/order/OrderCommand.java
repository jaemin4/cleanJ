package com.example.demo.domain.order;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OrderCommand {

    @Getter
    public static class CreateOrder {
        private final long userId;
        private final double discountRate;
        private final long productTotalAmount;
        private final List<OrderProduct> orderProducts;

        private CreateOrder(long userId, double discountRate, long productTotalAmount, List<OrderProduct> orderProducts) {
            this.userId = userId;
            this.discountRate = discountRate;
            this.productTotalAmount = productTotalAmount;
            this.orderProducts = orderProducts;
        }


        public static CreateOrder of(long userId,double discountRate, long productTotalAmount, List<OrderProduct> orderProducts) {
            return new CreateOrder(userId,discountRate, productTotalAmount,orderProducts);
        }

    }

    @Getter
    public static class OrderProduct{
        private final Long productId;
        private final Long quantity;

        private OrderProduct(Long productId, Long quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }

        public static OrderProduct of(Long productId, Long quantity) {
            return new OrderProduct(productId,quantity);
        }


    }

}
