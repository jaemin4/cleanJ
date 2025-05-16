package com.example.demo.application.payment;

import com.example.demo.domain.balance.BalanceService;
import com.example.demo.domain.coupon.CouponService;
import com.example.demo.domain.order.OrderInfo;
import com.example.demo.domain.order.OrderService;
import com.example.demo.domain.order.OrderStatus;
import com.example.demo.domain.stock.StockService;
import com.example.demo.infra.payment.MockPaymentService;
import com.example.demo.infra.payment.PaymentMockResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import static com.example.demo.support.constants.RabbitmqConstant.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentTransaction {
    private final OrderService orderService;
    private final BalanceService balanceService;
    private final MockPaymentService mockPaymentService;
    private final CouponService couponService;
    private final StockService stockService;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public void processPaymentWithTransaction(PaymentCriteria.Payment criteria) {
        OrderInfo.GetOrder order = null;
        try {
            order = orderService.getOrderById(criteria.getOrderId());
            long finalAmount = order.getProductTotalPrice();
            if (order.getOrderStatus().equals(OrderStatus.PAID)) {
                log.info("이미 결제된 주문입니다.");
                throw new RuntimeException("이미 결제된 주문입니다.");
            }
            /*
                1. 쿠폰사용
            */
            if (criteria.getCouponId() != null) {
                couponService.use(criteria.toUseCouponCommand());
                finalAmount = couponService.calculateDiscountedAmount(finalAmount, criteria.getCouponId());
            }

            /*
                2. 포인트사용
            */
            balanceService.use(criteria.toBalanceUseCommand(finalAmount));

            /*
                3. 주문상태변경
            */
            orderService.updateOrderStatus(criteria.getOrderId(), OrderStatus.PAID);

            /*
                4. 외부 API 호출
            */
            PaymentMockResponse.MockPay mockPaymentResponse = mockPaymentService.callAndValidateMockApi(
                    criteria.toPaymentMockRequest(order.getProductTotalPrice())
            );

            if (!"SUCCESS".equals(mockPaymentResponse.getStatus())) {
                log.error("결제 실패: orderId={}, status={}", criteria.getOrderId(), mockPaymentResponse.getStatus());
                throw new RuntimeException("결제 API 실패");
            }
            /*
                5. 결제내역 저장
            */
            rabbitTemplate.convertAndSend(
                    EXCHANGE_PAYMENT_HISTORY, ROUTE_PAYMENT_HISTORY_DB_SAVE,
                    criteria.toPaymentHistoryConsumerCommand(finalAmount, mockPaymentResponse.getTransactionId(), mockPaymentResponse.getStatus())
            );

            /*
                6. Redis 랭킹 업데이트
            */

            rabbitTemplate.convertAndSend(
                    EXCHANGE_PAYMENT_HISTORY, ROUTE_PAYMENT_HISTORY_REDIS_UPDATE,
                    criteria.getOrderId()
            );


        } catch (Exception e) {
            log.warn("트랜잭션 내 결제 실패, 주문 상태 취소 및 재고 복구 수행");

            try {
                if (order != null && order.getOrderStatus().equals(OrderStatus.CREATED)) {
                    log.info("주문 상태 복구 및 재고 복구 시작: orderId={}", criteria.getOrderId());

                    // 1. 주문 상태 변경
                    orderService.updateOrderStatus(criteria.getOrderId(), OrderStatus.CANCELED);

                    // 2. 재고 복구
                    OrderInfo.GetOrderItems getOrderItems = orderService.getOrderItemByOrderId(criteria.getOrderId());
                    stockService.recoveryStock(criteria.toRecoveryStockCommand(getOrderItems));

                    log.info("주문 취소 및 재고 복구 완료");
                } else {
                    log.info("이미 취소된 주문입니다. orderId={}", criteria.getOrderId());
                }

            } catch (Exception recoveryEx) {
                log.error("회복 중 추가 오류 발생: {}", recoveryEx.getMessage(), recoveryEx);
            }

            throw new RuntimeException("결제 처리 중 예외 발생", e);
        }
    }


}
