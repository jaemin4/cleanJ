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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import static com.example.demo.infra.payment.PaymentPopularScheduler.POPULAR_PRODUCTS_KEY;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentTransaction {
    private final OrderService orderService;
    private final BalanceService balanceService;
    private final MockPaymentService mockPaymentService;
    private final CouponService couponService;
    private final StockService stockService;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional
    public PaymentTransactionResult.Payment processPaymentWithTransaction(PaymentCriteria.Payment criteria) {
        OrderInfo.GetOrder order = null;
        try {

            //주문 조회
            order = orderService.getOrderById(criteria.getOrderId());
            long finalAmount = order.getProductTotalPrice();

            if (order.getOrderStatus().equals(OrderStatus.PAID)) {
                log.info("이미 결제된 주문입니다.");
                throw new RuntimeException("이미 결제된 주문입니다.");
            }

            //쿠폰사용
            if (criteria.getCouponId() != null) {
                couponService.use(criteria.toUseCouponCommand());
                finalAmount = couponService.calculateDiscountedAmount(finalAmount, criteria.getCouponId());
            }

            // 포인트사용
            balanceService.use(criteria.toBalanceUseCommand(finalAmount));

            //주문상태변경
            orderService.updateOrderStatus(criteria.getOrderId(), OrderStatus.PAID);

            //외부 API 호출
            PaymentMockResponse.MockPay mockPaymentResponse = mockPaymentService.callAndValidateMockApi(
                    criteria.toPaymentMockRequest(finalAmount)
            );

            //API 호출 검증
            if (!"SUCCESS".equals(mockPaymentResponse.getStatus())) {
                log.error("결제 실패: orderId={}, status={}", criteria.getOrderId(), mockPaymentResponse.getStatus());
                throw new RuntimeException("결제 API 실패");
            }

            return PaymentTransactionResult.Payment.of(
                    mockPaymentResponse.getTransactionId(),
                    mockPaymentResponse.getStatus(),
                    finalAmount
            );

        } catch (Exception e) {
            log.warn("트랜잭션 내 결제 실패, 주문 상태 취소 및 재고 복구 수행");

            try {
                if (order != null && order.getOrderStatus().equals(OrderStatus.CREATED)) {
                    log.info("주문 상태 복구 및 재고 복구 시작: orderId={}", criteria.getOrderId());

                    //주문 상태 변경
                    orderService.updateOrderStatus(criteria.getOrderId(), OrderStatus.CANCELED);

                    //재고 복구
                    OrderInfo.GetOrderItems getOrderItems = orderService.getOrderItemByOrderId(criteria.getOrderId());
                    stockService.recoveryStock(criteria.toRecoveryStockCommand(getOrderItems));

                    //쿠폰 랭킹 복구
                   // redisTemplate.opsForZSet().incrementScore(POPULAR_PRODUCTS_KEY, productId, -1);

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
