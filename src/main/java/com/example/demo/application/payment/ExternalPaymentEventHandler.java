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
import org.springframework.context.event.EventListener;
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

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void requestPaymentApi(PaymentEvent.RequestPaymentApi event){
        PaymentMockResponse.MockPay result = mockPaymentService.callAndValidateMockApi(
        PaymentMockRequest.Mock.of(event.getOrderId(),event.getUserId(),event.getFinalAmount()));

        if (!"SUCCESS".equals(result.getStatus())) {
            eventPublisher.publishEvent(PaymentEvent.RecoveryOrder.of(event.getOrderId()));
            eventPublisher.publishEvent(PaymentEvent.RecoveryPayment.of(
            event.getOrderId(), event.getUserId(), event.getCouponId(), event.getFinalAmount()));
            throw new RuntimeException("결제 API 실패");
        }

        try{
            paymentHistoryService.recordPaymentHistory(PaymentHistoryCommand.Save.of(event.getUserId(),
                    event.getFinalAmount(),event.getOrderId(),result.getTransactionId(),result.getStatus()));
        }catch (Exception e){
            log.info("결제내역 저장 실패 : {}", e.getMessage());
            //TODO 추가 조치 필요
        }

        try {
            rabbitTemplate.convertAndSend(
                    EXCHANGE_PAYMENT_HISTORY,
                    ROUTE_PAYMENT_HISTORY_REDIS_UPDATE,
                    event.getOrderId()
            );
        } catch (Exception e) {
            log.error("Redis 랭킹 업데이트 실패: orderId={}, error={}", event.getOrderId(), e.getMessage(), e);
            //TODO 추가 조치 필요
        }


    }
}
