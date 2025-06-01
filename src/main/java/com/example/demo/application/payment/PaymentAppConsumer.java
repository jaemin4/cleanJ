package com.example.demo.application.payment;

import com.example.demo.domain.balance.BalanceCommand;
import com.example.demo.domain.balance.BalanceService;
import com.example.demo.domain.coupon.CouponCommand;
import com.example.demo.domain.coupon.CouponService;
import com.example.demo.domain.order.OrderInfo;
import com.example.demo.domain.order.OrderService;
import com.example.demo.domain.order.OrderStatus;
import com.example.demo.domain.stock.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentAppConsumer {

    private final OrderService orderService;
    private final StockService stockService;
    private final BalanceService balanceService;
    private final CouponService couponService;

    @KafkaListener(
            topics = "order.recovery",
            groupId = "consumer-group-order-recovery", // 명확한 groupId
            containerFactory = "kafkaListenerContainerFactory",
            concurrency = "1"
    )
    @Transactional
    public void recoveryOrder(PaymentEventCommand.RecoveryOrder event) {
        try {
            OrderInfo.GetOrder order = orderService.getOrderById(event.getOrderId());
            if (order != null && order.getOrderStatus().equals(OrderStatus.CREATED)) {
                log.info("주문 상태 복구 및 재고 복구 시작 : {}", order.getOrderId());

                orderService.updateOrderStatus(order.getOrderId(), OrderStatus.CANCELED);

                OrderInfo.GetOrderItems getOrderItems = orderService.getOrderItemByOrderId(order.getOrderId());
                stockService.recoveryStock(event.toRecoveryStockCommand(getOrderItems));

                log.info("주문 취소 및 재고 복구 완료");
            } else {
                log.info("이미 취소된 주문입니다. orderId={}", event.getOrderId());
            }

        } catch (Exception recoveryEx) {
            log.error("회복 중 추가 오류 발생: {}", recoveryEx.getMessage(), recoveryEx);
            throw new RuntimeException("회복 중 추가 오류 발생");
        }
    }

    @KafkaListener(
            topics = "payment.recovery",
            groupId = "consumer-group-payment-recovery",
            containerFactory = "kafkaListenerContainerFactory",
            concurrency = "1"
    )
    @Transactional
    public void recoveryPayment(PaymentEventCommand.RecoveryPayment event) {
        try {
            if (event.getCouponId() != null) {
                couponService.issue(CouponCommand.Issue.of(event.getUserId(), event.getCouponId()));
            }
            balanceService.charge(BalanceCommand.Charge.of(event.getUserId(), event.getFinalAmount()));
            orderService.updateOrderStatus(event.getOrderId(), OrderStatus.CANCELED);
        } catch (Exception recoveryEx) {
            log.error("결제 복구 중 오류 발생: {}", recoveryEx.getMessage(), recoveryEx);
            throw new RuntimeException("결제 복구 중 오류 발생");
        }
    }
}
