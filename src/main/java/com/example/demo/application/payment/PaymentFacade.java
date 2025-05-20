package com.example.demo.application.payment;

import com.example.demo.support.comm.aop.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

        //결제내역 저장
        rabbitTemplate.convertAndSend(
                EXCHANGE_PAYMENT_HISTORY, ROUTE_PAYMENT_HISTORY_DB_SAVE,
                criteria.toPaymentHistoryConsumerCommand(result.getFinalAmount(), result.getTransactionId(), result.getStatus())
        );

        //Redis 랭킹 업데이트
        rabbitTemplate.convertAndSend(
                EXCHANGE_PAYMENT_HISTORY, ROUTE_PAYMENT_HISTORY_REDIS_UPDATE,
                criteria.getOrderId()
        );

    }




}
