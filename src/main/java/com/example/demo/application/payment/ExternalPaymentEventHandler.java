package com.example.demo.application.payment;

import com.example.demo.domain.payment.PaymentHistoryCommand;
import com.example.demo.domain.payment.PaymentHistoryService;
import com.example.demo.infra.payment.MockPaymentService;
import com.example.demo.infra.payment.PaymentMockRequest;
import com.example.demo.infra.payment.PaymentMockResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import static com.example.demo.support.constants.RabbitmqConstant.EXCHANGE_PAYMENT_HISTORY;
import static com.example.demo.support.constants.RabbitmqConstant.ROUTE_PAYMENT_HISTORY_REDIS_UPDATE;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExternalPaymentEventHandler {

    private final MockPaymentService mockPaymentService;
    private final ApplicationEventPublisher eventPublisher;
    private final PaymentHistoryService paymentHistoryService;
    private final RabbitTemplate rabbitTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void requestPaymentApi(PaymentEventCommand.RequestPaymentApi event){
        PaymentMockResponse.MockPay result = mockPaymentService.callAndValidateMockApi(
        PaymentMockRequest.Mock.of(event.getOrderId(),event.getUserId(),event.getFinalAmount()));

        if (!"SUCCESS".equals(result.getStatus())) {
            eventPublisher.publishEvent(PaymentEventCommand.RecoveryOrder.of(event.getOrderId()));
            eventPublisher.publishEvent(PaymentEventCommand.RecoveryPayment.of(
            event.getOrderId(), event.getUserId(), event.getCouponId(), event.getFinalAmount()));
            throw new RuntimeException("결제 API 실패");
        }

        paymentHistoryService.recordPaymentHistory(PaymentHistoryCommand.Save.of(event.getUserId(),
                event.getFinalAmount(),event.getOrderId(),result.getTransactionId(),result.getStatus()));

        rabbitTemplate.convertAndSend(
                EXCHANGE_PAYMENT_HISTORY,
                ROUTE_PAYMENT_HISTORY_REDIS_UPDATE,
                event.getOrderId());
    }



}
