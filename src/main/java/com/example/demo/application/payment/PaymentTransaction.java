package com.example.demo.application.payment;

import com.example.demo.domain.balance.BalanceService;
import com.example.demo.domain.coupon.CouponService;
import com.example.demo.domain.order.OrderInfo;
import com.example.demo.domain.order.OrderService;
import com.example.demo.domain.order.OrderStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentTransaction {

    private final OrderService orderService;
    private final BalanceService balanceService;
    private final CouponService couponService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void processPaymentWithTransaction(PaymentCriteria.Payment criteria) {
        try {
            OrderInfo.GetOrder order = orderService.getOrderById(criteria.getOrderId());
            long finalAmount = order.getProductTotalPrice();

            if (order.getOrderStatus().equals(OrderStatus.PAID)) {
                log.info("이미 결제된 주문입니다.");
                throw new RuntimeException("이미 결제된 주문입니다.");
            }
            if (criteria.getCouponId() != null) {
                couponService.use(criteria.toUseCouponCommand());
                finalAmount = couponService.calculateDiscountedAmount(finalAmount, criteria.getCouponId());
            }
            balanceService.use(criteria.toBalanceUseCommand(finalAmount));
            orderService.updateOrderStatus(criteria.getOrderId(), OrderStatus.PAID);

            eventPublisher.publishEvent(PaymentEvent.RequestPaymentApi.of(
                    criteria.getOrderId(),criteria.getUserId(),finalAmount,criteria.getCouponId()));

        } catch (Exception e) {
            eventPublisher.publishEvent(PaymentEvent.RecoveryOrder.of(criteria.getOrderId()));
            throw new RuntimeException("결제 처리 중 예외 발생", e);
        }
    }



}
