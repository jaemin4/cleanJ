package com.example.demo.domain.order;

import com.example.demo.application.payment.PaymentCriteria;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OrderInfo {

    @Getter
    public static class CreateOrder {
        private final Long orderId;
        private final double productTotalPrice;

        private CreateOrder(Long orderId, double productTotalPrice) {
            this.orderId = orderId;
            this.productTotalPrice = productTotalPrice;
        }

        public static CreateOrder of(Long orderId, double productTotalPrice) {
            return new CreateOrder(orderId, productTotalPrice);
        }
    }

    @Getter
    public static class GetOrder{
        private final Long orderId;
        private final OrderStatus orderStatus;
        private final long productTotalPrice;

        public GetOrder(Long orderId, OrderStatus orderStatus, long productTotalPrice) {
            this.orderId = orderId;
            this.orderStatus = orderStatus;
            this.productTotalPrice = productTotalPrice;
        }

        public static GetOrder of(Long orderId, OrderStatus orderStatus, long productTotalPrice) {
            return new GetOrder(orderId,orderStatus, productTotalPrice);
        }

    }

    @Getter
    public static class GetOrderItems{
        List<OrderInfo.OrderProduct> items;

        private GetOrderItems(List<OrderInfo.OrderProduct> items) {
            this.items = items;
        }

        public static OrderInfo.GetOrderItems of(List<OrderInfo.OrderProduct> items) {
            return new OrderInfo.GetOrderItems(items);
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

        public static OrderInfo.OrderProduct of(Long productId, Long quantity) {
            return new OrderInfo.OrderProduct(productId,quantity);
        }

    }

}