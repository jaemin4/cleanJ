package com.example.demo.application.payment;

import com.example.demo.domain.balance.BalanceCommand;
import com.example.demo.domain.coupon.CouponCommand;
import com.example.demo.domain.order.OrderInfo;
import com.example.demo.domain.payment.PaymentHistoryCommand;
import com.example.demo.domain.stock.StockCommand;
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

        public BalanceCommand.Use toBalanceUseCommand(Long amount){
            return BalanceCommand.Use.of(userId, amount);
        }

        public PaymentMockRequest.Mock toPaymentMockRequest(Long amount) {
            return PaymentMockRequest.Mock.of(orderId,userId,amount);
        }

        public PaymentHistoryCommand.Save toPaymentHistoryCommand(String transactionId, String status, Long amount ) {
            return PaymentHistoryCommand.Save.of(userId,amount,orderId,transactionId,status);
        }

        public CouponCommand.Use toUseCouponCommand() {
            return CouponCommand.Use.of(userId,couponId);
        }

        public CouponCommand.GetDiscountRate toGetDiscountRateCommand() {
            return CouponCommand.GetDiscountRate.of(couponId);
        }

        public StockCommand.RecoveryStock toRecoveryStockCommand(OrderInfo.GetOrderItems getOrderItems) {
            List<StockCommand.OrderProduct> products = getOrderItems.getItems().stream()
                    .map(item -> StockCommand.OrderProduct.of(item.getProductId(), item.getQuantity()))
                    .toList();

            return StockCommand.RecoveryStock.of(products);
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
