package com.example.demo.application.payment;

import com.example.demo.domain.balance.BalanceCommand;
import com.example.demo.domain.coupon.CouponCommand;
import com.example.demo.domain.order.OrderInfo;
import com.example.demo.domain.payment.PaymentHistoryCommand;
import com.example.demo.domain.stock.StockCommand;
import com.example.demo.infra.payment.PaymentHistoryConsumerCommand;
import com.example.demo.infra.payment.PaymentMockRequest;
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

        private Payment(Long orderId, Long userId, Long couponId) {
            this.orderId = orderId;
            this.userId = userId;
            this.couponId = couponId;
        }

        public static Payment of(Long orderId, Long userId, Long couponId) {
            return new Payment(orderId,userId,couponId);
        }

        public BalanceCommand.Use toBalanceUseCommand(Long amount){
            return BalanceCommand.Use.of(userId, amount);
        }

        public PaymentMockRequest.Mock toPaymentMockRequest(Long amount) {
            return PaymentMockRequest.Mock.of(orderId,userId,amount);
        }

        public CouponCommand.Use toUseCouponCommand() {
            return CouponCommand.Use.of(userId,couponId);
        }

        public StockCommand.RecoveryStock toRecoveryStockCommand(OrderInfo.GetOrderItems getOrderItems) {
            List<StockCommand.OrderProduct> products = getOrderItems.getItems().stream()
                    .map(item -> StockCommand.OrderProduct.of(item.getProductId(), item.getQuantity()))
                    .toList();

            return StockCommand.RecoveryStock.of(products);
        }

        public PaymentHistoryConsumerCommand.Save toPaymentHistoryConsumerCommand(long amount,String transactionId,String status) {
            return PaymentHistoryConsumerCommand.Save.of(userId,amount,transactionId,status,orderId);
        }


    }

}
