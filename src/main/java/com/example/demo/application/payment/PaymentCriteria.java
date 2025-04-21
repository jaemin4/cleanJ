package com.example.demo.application.payment;

import com.example.demo.application.order.OrderCriteria;
import com.example.demo.domain.balance.BalanceCommand;
import com.example.demo.domain.coupon.CouponCommand;
import com.example.demo.domain.payment.PaymentHistoryCommand;
import com.example.demo.domain.stock.StockCommand;
import com.example.demo.infra.payment.PaymentMockRequest;
import com.example.demo.infra.payment.PaymentMockResponse;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentCriteria {

    @Getter
    public static class Payment {
        private final Long orderId;
        private final Long userId;
        private final Long couponId;
        private final List<OrderProduct> items;

        private Payment(Long orderId, Long userId, Long couponId, List<OrderProduct> items) {
            this.orderId = orderId;
            this.userId = userId;
            this.couponId = couponId;
            this.items = items;
        }

        public static Payment of(Long orderId, Long userId, Long couponId, List<OrderProduct> items) {
            return new Payment(userId, orderId, couponId, items);
        }

        public PaymentMockRequest.Mock toPaymentMockRequest(Long finalAmount) {
            return PaymentMockRequest.Mock.of(orderId,userId,finalAmount);
        }

        public PaymentProcessorCriteria.PayMockResponse toPaymentMockResponse(String transactionId, String status, String message ) {
            return PaymentProcessorCriteria.PayMockResponse.of(transactionId,status,message);
        }

        public PaymentProcessorCriteria.ConfirmPayment toConfirmPaymentCriteria(Long finalAmount) {
            return PaymentProcessorCriteria.ConfirmPayment.of(
                    orderId,
                    userId,
                    couponId,
                    items.stream()
                            .map(item -> PaymentProcessorCriteria.OrderProduct.of(item.getProductId(),item.getQuantity()))
                            .toList(),
                    finalAmount
            );
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

        public static PaymentCriteria.OrderProduct of(Long productId, Long quantity) {
            return new PaymentCriteria.OrderProduct(productId,quantity);
        }

    }


}
