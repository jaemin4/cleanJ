package com.example.demo.application.payment;

import com.example.demo.domain.balance.BalanceService;
import com.example.demo.domain.coupon.CouponService;
import com.example.demo.domain.order.OrderService;
import com.example.demo.domain.order.OrderStatus;
import com.example.demo.domain.payment.PaymentHistoryService;
import com.example.demo.domain.stock.StockService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentProcessor {
    private final CouponService couponService;
    private final BalanceService balanceService;
    private final PaymentHistoryService paymentHistoryService;
    private final OrderService orderService;
    private final StockService stockService;

    @Transactional
    public void confirmPayment(PaymentProcessorCriteria.ConfirmPayment criteria, PaymentProcessorCriteria.PayMockResponse response) {
        long finalAmount = criteria.getFinalAmount();
        try {
            // 1. 쿠폰 사용
            couponService.use(criteria.toUseCouponCommand());

            // 2. 잔액 차감
            balanceService.use(criteria.toBalanceUseCommand(finalAmount));

            // 3. 결제 이력 저장
            paymentHistoryService.recordPaymentHistory(
                    criteria.toPaymentHistoryCommand(
                            response.getTransactionId(),
                            response.getStatus(),
                            criteria.getOrderId()
                    )
            );

            // 4. 주문 상태 변경
            orderService.updateOrderStatus(criteria.getOrderId(), OrderStatus.PAID);
            log.info("결제 성공: orderId={}, finalAmount={}", criteria.getOrderId(), finalAmount);

        } catch (Exception e) {
            log.warn("결제 실패, 주문 상태 취소 및 재고 복구: orderId={}", criteria.getOrderId());
            // 5-1. 주문 상태 변경(CANCLE)
            orderService.updateOrderStatus(criteria.getOrderId(), OrderStatus.CANCELED);

            // 5-2. 재고 회복
            stockService.recoveryStock(criteria.toRecoveryStockCommand());

            throw new RuntimeException("결제 처리 중 예외 발생", e);
        }
    }
}
