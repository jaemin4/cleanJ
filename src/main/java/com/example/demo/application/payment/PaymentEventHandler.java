package com.example.demo.application.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import static com.example.demo.support.constants.RabbitmqConstant.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventHandler {

    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        var criteria = event.criteria();
        var result = event.result();

        try {
            rabbitTemplate.convertAndSend(
                    EXCHANGE_PAYMENT_HISTORY,
                    ROUTE_PAYMENT_HISTORY_DB_SAVE,
                    criteria.toPaymentHistoryConsumerCommand(result.getFinalAmount(), result.getTransactionId(), result.getStatus())
            );
            log.info("결제내역 저장 메시지 전송 성공: orderId={}", criteria.getOrderId());
        } catch (Exception e) {
            log.error("결제내역 저장 메시지 전송 실패: {}", e.getMessage(), e);
        }

        try {
            rabbitTemplate.convertAndSend(
                    EXCHANGE_PAYMENT_HISTORY,
                    ROUTE_PAYMENT_HISTORY_REDIS_UPDATE,
                    criteria.getOrderId()
            );
        } catch (Exception e) {
            log.error("Redis 랭킹 업데이트 실패: orderId={}, error={}", criteria.getOrderId(), e.getMessage(), e);
        }
    }
}
