package com.example.demo.application.payment;

import com.example.demo.support.comm.aop.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import static com.example.demo.support.constants.RabbitmqConstant.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentFacade {

    private final PaymentTransaction paymentTransaction;
    private final RabbitTemplate rabbitTemplate;

    @DistributedLock(key = "'payment:user:' + #criteria.userId", waitTime = 3, leaseTime = 5)
    public void pay(PaymentCriteria.Payment criteria) {
        PaymentTransactionResult.Payment result = paymentTransaction.processPaymentWithTransaction(criteria);

        try {
            rabbitTemplate.convertAndSend(
                    EXCHANGE_PAYMENT_HISTORY,
                    ROUTE_PAYMENT_HISTORY_DB_SAVE,
                    criteria.toPaymentHistoryConsumerCommand(result.getFinalAmount(), result.getTransactionId(), result.getStatus())
            );
            log.info("결제내역 저장 메시지 전송 성공: orderId={}", criteria.getOrderId());
        } catch (Exception e) {
            log.error("[RabbitMQ 전송 실패] 결제내역 저장 실패: orderId={}, error={}", criteria.getOrderId(), e.getMessage(), e);
        }

        // Redis 랭킹 업데이트
        try {
            rabbitTemplate.convertAndSend(
                    EXCHANGE_PAYMENT_HISTORY,
                    ROUTE_PAYMENT_HISTORY_REDIS_UPDATE,
                    criteria.getOrderId()
            );
        } catch (Exception e) {
            log.error("[RabbitMQ 전송 실패] Redis 랭킹 업데이트 실패: orderId={}, error={}", criteria.getOrderId(), e.getMessage(), e);
        }

    }




}
