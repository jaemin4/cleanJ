package com.example.demo.application.payment;

import com.example.demo.domain.payment.PaymentHistoryCommand;
import com.example.demo.domain.payment.PaymentHistoryService;
import com.example.demo.infra.payment.MockPaymentService;
import com.example.demo.infra.payment.PaymentMockRequest;
import com.example.demo.infra.payment.PaymentMockResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.event.EventListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;
import static com.example.demo.support.constants.RabbitmqConstant.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventHandler {

    private final RabbitTemplate rabbitTemplate;
    private final PaymentEventTransaction paymentEventTransaction;
    private final MockPaymentService mockPaymentService;
    private final PaymentHistoryService paymentHistoryService;

    @Retryable(
            exceptionExpression = "#{exception instanceof T(java.lang.Exception)}",
            maxAttempts = 3,
            backoff = @Backoff(delay = 0)
    )
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void recoveryOrder(PaymentEvent.RecoveryOrder event){
        paymentEventTransaction.recoveryOrder(event);
    }

    @Retryable(
            exceptionExpression = "#{exception instanceof T(java.lang.Exception)}",
            maxAttempts = 3,
            backoff = @Backoff(delay = 0)
    )
    @EventListener
    public void recoveryPayment(PaymentEvent.RecoveryPayment event){
        paymentEventTransaction.recoveryPayment(PaymentEvent.RecoveryPayment.of(event.getOrderId(),
                event.getUserId(), event.getCouponId(), event.getFinalAmount()));
    }

}
