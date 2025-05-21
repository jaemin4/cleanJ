package com.example.demo.application.payment;

import com.example.demo.domain.order.OrderInfo;
import com.example.demo.domain.stock.StockCommand;
import lombok.Getter;

import java.util.List;

public class PaymentEvent {

    @Getter
    public static class RequestPaymentApi {
        private final Long orderId;
        private final Long userId;
        private final long finalAmount;
        private final Long couponId;


        private RequestPaymentApi(Long orderId, Long userId, long finalAmount, Long couponId) {
            this.orderId = orderId;
            this.userId = userId;
            this.finalAmount = finalAmount;
            this.couponId = couponId;
        }

        public static RequestPaymentApi of(Long orderId, Long userId, long finalAmount, Long couponId) {
            return new RequestPaymentApi(orderId, userId, finalAmount, couponId);
        }

    }

    @Getter
    public static class RecoveryOrder{
        private final Long orderId;

        private RecoveryOrder(Long orderId) {
            this.orderId = orderId;
        }

        public static RecoveryOrder of(Long orderId) {
            return new RecoveryOrder(orderId);
        }

        public StockCommand.RecoveryStock toRecoveryStockCommand(OrderInfo.GetOrderItems getOrderItems) {
            List<StockCommand.OrderProduct> products = getOrderItems.getItems().stream()
                    .map(item -> StockCommand.OrderProduct.of(item.getProductId(), item.getQuantity()))
                    .toList();

            return StockCommand.RecoveryStock.of(products);
        }
    }

    @Getter
    public static class RecoveryPayment{
        private final Long orderId;
        private final Long userId;
        private final Long couponId;
        private final long finalAmount;

        private RecoveryPayment(Long orderId, Long userId, Long couponId, long finalAmount) {
            this.orderId = orderId;
            this.userId = userId;
            this.couponId = couponId;
            this.finalAmount = finalAmount;
        }

        public static RecoveryPayment of(Long orderId, Long userId, Long couponId, long finalAmount) {
            return new RecoveryPayment(orderId, userId, couponId, finalAmount);
        }


    }



}
