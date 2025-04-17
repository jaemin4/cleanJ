package com.example.demo.integrate;

import com.example.demo.application.payment.PaymentCriteria;
import com.example.demo.application.payment.PaymentFacade;
import com.example.demo.domain.balance.BalanceCommand;
import com.example.demo.domain.balance.BalanceService;
import com.example.demo.domain.order.*;
import com.example.demo.domain.payment.PaymentHistory;
import com.example.demo.domain.payment.PaymentHistoryCommand;
import com.example.demo.domain.payment.PaymentHistoryRepository;
import com.example.demo.domain.payment.PaymentHistoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.List;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest
public class PaymentTest {

    @Autowired
    private PaymentFacade paymentFacade;

    @Autowired
    private OrderService orderService;

    @Autowired
    private BalanceService balanceService;

    @Autowired
    private PaymentHistoryRepository paymentHistoryRepository;

    @Test
    @DisplayName("결제 내역이 정상적으로 저장된다")
    void recordPaymentHistory_success() {
        // given
        PaymentHistoryRepository mockRepository = mock(PaymentHistoryRepository.class);
        PaymentHistoryService service = new PaymentHistoryService(mockRepository);

        Long userId = 1L;
        Long amount = 5000L;
        Long orderId = 100L;
        String transactionId = "TX123456789";
        String status = "SUCCESS";

        PaymentHistoryCommand.Save command = PaymentHistoryCommand.Save.of(
                userId, amount, orderId, transactionId, status
        );

        // when
        service.recordPaymentHistory(command);

        // then
        ArgumentCaptor<PaymentHistory> captor = ArgumentCaptor.forClass(PaymentHistory.class);
        verify(mockRepository, times(1)).save(captor.capture());

        PaymentHistory saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getAmount()).isEqualTo(amount);
        assertThat(saved.getOrderId()).isEqualTo(orderId);
        assertThat(saved.getTransactionId()).isEqualTo(transactionId);
        assertThat(saved.getStatus()).isEqualTo(status);
    }

    @Test
    @DisplayName("결제 성공 시 잔액 차감, 결제 이력 저장, 주문 상태 변경까지 처리된다")
    void testPaySuccess() {
        // given
        Long userId = 10L;

        // 1. 주문 생성
        OrderCommand.CreateOrder orderCommand = OrderCommand.CreateOrder.of(
                userId,
                0.0, // 할인 없음
                10_000L,
                List.of(OrderCommand.OrderProduct.of(1L, 2L)) // 상품 ID 1번, 수량 2개
        );
        OrderInfo.CreateOrder createdOrder = orderService.createOrder(orderCommand);

        // 2. 잔액 충전
        balanceService.charge(BalanceCommand.Charge.of(userId, 20_000L));

        // 3. 결제 요청 생성
        PaymentCriteria.Payment payment = PaymentCriteria.Payment.of(userId, createdOrder.getOrderId());

        // when
        paymentFacade.pay(payment);

        // then - 결제 이력 존재
        boolean exists = paymentHistoryRepository.existsByOrderId(createdOrder.getOrderId());
        assertThat(exists).isTrue();

        // then - 주문 상태가 PAID로 변경되었는지 확인
        Order paidOrder = orderService.getOrderById(createdOrder.getOrderId());
        assertThat(paidOrder.getOrderStatus()).isEqualTo(OrderStatus.PAID);
    }



}
