package com.example.demo.application.payment;

import com.example.demo.infra.payment.PaymentMockRequest;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentCriteria {

    @Getter
    public static class pay {
        private final Long orderId;
        private final Long userId;
        private final Long couponId;

        private pay(Long orderId, Long userId, Long couponId) {
            this.orderId = orderId;
            this.userId = userId;
            this.couponId = couponId;
        }

        public static pay of(Long orderId, Long userId, Long couponId) {
            return new pay(userId, orderId, couponId);
        }

        public PaymentMockRequest.Mock toPaymentMockRequest(Long finalAmount) {
            return PaymentMockRequest.Mock.of(orderId,userId,finalAmount);
        }

        public PaymentProcessorCriteria.PayMockResponse toPaymentMockResponse(String transactionId, String status, String message ) {
            return PaymentProcessorCriteria.PayMockResponse.of(transactionId,status,message);
        }

        public PaymentProcessorCriteria.ConfirmPayment toConfirmPaymentCriteria(Long finalAmount, List<PaymentProcessorCriteria.OrderProduct> items) {
            return PaymentProcessorCriteria.ConfirmPayment.of(
                    orderId,
                    userId,
                    couponId,
                    items,
                    finalAmount
            );
        }
    }




}
