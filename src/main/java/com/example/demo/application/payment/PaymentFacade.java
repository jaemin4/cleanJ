package com.example.demo.application.payment;

import com.example.demo.domain.balance.BalanceService;
import com.example.demo.domain.coupon.CouponService;
import com.example.demo.domain.order.OrderInfo;
import com.example.demo.domain.order.OrderService;
import com.example.demo.domain.order.OrderStatus;
import com.example.demo.domain.payment.PaymentHistoryService;
import com.example.demo.domain.stock.StockCommand;
import com.example.demo.domain.stock.StockService;
import com.example.demo.infra.payment.MockPaymentService;
import com.example.demo.infra.payment.PaymentMockResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentFacade {

    private final PaymentHistoryService paymentHistoryService;
    private final OrderService orderService;
    private final BalanceService balanceService;
    private final MockPaymentService mockPaymentService;
    private final CouponService couponService;
    private final StockService stockService;

    @Transactional
    public void pay(PaymentCriteria.Payment criteria) {
        try {
            OrderInfo.GetOrder order = orderService.getOrderById(criteria.getOrderId());

            long finalAmount = BigDecimal.valueOf(order.getProductTotalPrice())
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValue();

            if (criteria.getCouponId() != null) {
                couponService.use(criteria.toUseCouponCommand());
                double discountRate = couponService.getDiscountRate(criteria.toGetDiscountRateCommand());
                finalAmount = (long) (finalAmount - (finalAmount * discountRate * 0.01));
            }

            balanceService.use(criteria.toBalanceUseCommand(finalAmount));

            orderService.updateOrderStatus(criteria.getOrderId(), OrderStatus.PAID);

            PaymentMockResponse.MockPay mockPaymentResponse = mockPaymentService.callAndValidateMockApi(
                    criteria.toPaymentMockRequest(order.getProductTotalPrice())
            );

            if (!"200".equals(mockPaymentResponse.getStatus())) {
                log.error("결제 실패: orderId={}, status={}", criteria.getOrderId(), mockPaymentResponse.getStatus());
                throw new RuntimeException("결제 API 실패");
            }

            try {
                paymentHistoryService.recordPaymentHistory(
                        criteria.toPaymentHistoryCommand(
                                mockPaymentResponse.getTransactionId(),
                                mockPaymentResponse.getStatus(),
                                criteria.getOrderId())
                );
            } catch (Exception e) {

                log.error("결제 이력 저장 실패: orderId={}, txId={}, error={}",
                        criteria.getOrderId(), mockPaymentResponse.getTransactionId(), e.getMessage());
            }

        } catch (Exception e) {
            log.warn("결제 실패, 주문 상태 취소 및 재고 복구: orderId={}", criteria.getOrderId());
            orderService.updateOrderStatus(criteria.getOrderId(), OrderStatus.CANCELED);

            OrderInfo.GetOrderItems getOrderItems = orderService.getOrderItemByOrderId(criteria.getOrderId());
            StockCommand.RecoveryStock recoveryStock = criteria.toRecoveryStockCommand(getOrderItems);
            stockService.recoveryStock(recoveryStock);

            throw new RuntimeException("결제 처리 중 예외 발생", e);
        }
    }




}
