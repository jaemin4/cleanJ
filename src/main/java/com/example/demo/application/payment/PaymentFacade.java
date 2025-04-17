package com.example.demo.application.payment;

import com.example.demo.domain.balance.BalanceService;
import com.example.demo.domain.order.Order;
import com.example.demo.domain.order.OrderService;
import com.example.demo.domain.payment.PaymentHistoryService;
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

    @Transactional
    public void pay(PaymentCriteria.Payment criteria) {
        // 1. 주문 조회
        Order order = orderService.getOrderById(criteria.getOrderId());

        // 2. 결제 API 호출 및 검증
        PaymentMockResponse mockPaymentResponse = mockPaymentService.callAndValidateMockApi(
                criteria.toPaymentMockRequest((long) order.getFinalTotalPrice())
        );
        String transactionId = mockPaymentResponse.getTransactionId();
        String status = mockPaymentResponse.getStatus();
        double finalPrice = order.getFinalTotalPrice();
        Long amount = BigDecimal.valueOf(finalPrice)
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();

        // 3. 잔액 차감
        balanceService.use(criteria.toBalanceUseCommand(amount));

        // 4. 결제 이력 저장
        paymentHistoryService.recordPaymentHistory(
                criteria.toPaymentHistoryCommand(transactionId, status, order.getId())
        );

        // 5. 주문 결제 처리
        orderService.completeOrder(criteria.getOrderId());
    }


}
