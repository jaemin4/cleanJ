package com.example.demo.application.payment;

import com.example.demo.infra.coupon.CouponConsumerCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventHandler {

    private final KafkaTemplate<String, Object> kafkaTemplate;


    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void recoveryOrder(PaymentEventCommand.RecoveryOrder event){
        kafkaTemplate.send("order.recovery", event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void recoveryPayment(PaymentEventCommand.RecoveryPayment event){
        kafkaTemplate.send("payment.recovery", event);
    }

}
