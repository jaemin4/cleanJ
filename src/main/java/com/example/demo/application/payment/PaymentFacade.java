package com.example.demo.application.payment;

import com.example.demo.domain.balance.BalanceService;
import com.example.demo.domain.coupon.CouponService;
import com.example.demo.domain.order.OrderInfo;
import com.example.demo.domain.order.OrderService;
import com.example.demo.domain.order.OrderStatus;
import com.example.demo.support.comm.aop.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentFacade {

    private final OrderService orderService;
    private final BalanceService balanceService;
    private final CouponService couponService;
    private final ApplicationEventPublisher eventPublisher;

    @DistributedLock(key = "'payment:user:' + #criteria.userId", waitTime = 3, leaseTime = 5)
    @Transactional
    public void pay(PaymentCriteria.Payment criteria) {
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

        eventPublisher.publishEvent(PaymentEventCommand.RequestPaymentApi.of(
                criteria.getOrderId(),
                criteria.getUserId(),
                finalAmount,
                criteria.getCouponId()

        ));
    }


}
