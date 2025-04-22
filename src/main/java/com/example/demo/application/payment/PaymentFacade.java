package com.example.demo.application.payment;

import com.example.demo.domain.order.OrderInfo;
import com.example.demo.domain.order.OrderService;
import com.example.demo.infra.payment.MockPaymentService;
import com.example.demo.infra.payment.PaymentMockResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentFacade {

    private final OrderService orderService;
    private final MockPaymentService mockPaymentService;
    private final PaymentProcessor paymentProcessor;

    public void pay(PaymentCriteria.pay criteria) {
        // 1. 주문 정보 조회
        OrderInfo.GetOrder order = orderService.getOrderById(criteria.getOrderId());

        // 2. 주문 상품 조회
        OrderInfo.GetOrderItems getOrderItems = orderService.getOrderItemByOrderId(order.getOrderId());

        List<PaymentProcessorCriteria.OrderProduct> items = getOrderItems.getItems().stream()
                .map(item -> PaymentProcessorCriteria.OrderProduct.of(item.getProductId(), item.getQuantity()))
                .toList();

        // 3. 결제 금액
        Long finalAmount = BigDecimal.valueOf(order.getProductTotalPrice())
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();


        // 4. 트랜잭션 외부: 결제 API 호출
        PaymentMockResponse.MockPay response = mockPaymentService.callAndValidateMockApi(
                criteria.toPaymentMockRequest(order.getProductTotalPrice())
        );

        if (!"200".equals(response.getStatus())) {
            log.error("결제 실패: orderId={}, status={}", criteria.getOrderId(), response.getStatus());
            throw new RuntimeException("결제 API 실패");
        }

        // 5. 트랜잭션 내부: 실제 결제 처리
        paymentProcessor.confirmPayment(
                criteria.toConfirmPaymentCriteria( finalAmount,items)
               ,criteria.toPaymentMockResponse(response.getTransactionId(),response.getStatus(),response.getMessage())
        );
    }


}
