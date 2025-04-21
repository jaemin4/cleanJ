package com.example.demo.application.payment;

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
public class PaymentProcessorCriteria {

    @Getter
    public static class ConfirmPayment{
        private final Long orderId;
        private final Long userId;
        private final Long couponId;
        private final List<PaymentProcessorCriteria.OrderProduct> items;
        private final long finalAmount;

        private ConfirmPayment(Long orderId, Long userId, Long couponId, List<PaymentProcessorCriteria.OrderProduct> items, long finalAmount) {
            this.orderId = orderId;
            this.userId = userId;
            this.couponId = couponId;
            this.items = items;
            this.finalAmount = finalAmount;
        }

        public static ConfirmPayment of(Long orderId, Long userId, Long couponId, List<PaymentProcessorCriteria.OrderProduct> items, long finalAmount) {
            return new ConfirmPayment(orderId, userId, couponId, items, finalAmount);
        }

        public BalanceCommand.Use toBalanceUseCommand(Long amount){
            return BalanceCommand.Use.of(userId, amount);
        }

        public CouponCommand.Use toUseCouponCommand(){
            return CouponCommand.Use.of(couponId,userId);
        }

        public PaymentHistoryCommand.Save toPaymentHistoryCommand(String transactionId, String status, Long amount ) {
            return PaymentHistoryCommand.Save.of(userId,amount,orderId,transactionId,status);
        }

        public StockCommand.RecoveryStock toRecoveryStockCommand() {
            return StockCommand.RecoveryStock.of(
                    items.stream()
                            .map(item -> StockCommand.OrderProduct.of(item.productId, item.quantity))
                            .collect(Collectors.toList())
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

        public static PaymentProcessorCriteria.OrderProduct of(Long productId, Long quantity) {
            return new PaymentProcessorCriteria.OrderProduct(productId,quantity);
        }
    }

    @Getter
    public static class PayMockResponse{
        private final String transactionId;
        private final String status;
        private final String message;

        private PayMockResponse(String transactionId, String status, String message) {
            this.transactionId = transactionId;
            this.status = status;
            this.message = message;
        }

        public static PayMockResponse of(String transactionId, String status, String message) {
            return new PayMockResponse(transactionId,status,message);
        }


    }

}
