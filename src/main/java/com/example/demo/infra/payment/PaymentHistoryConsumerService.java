package com.example.demo.infra.payment;

import com.example.demo.domain.payment.PaymentHistoryCommand;
import com.example.demo.domain.payment.PaymentHistoryService;
import com.example.demo.support.util.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("consumer")
@Slf4j
@RequiredArgsConstructor
public class PaymentHistoryConsumerService {

    private final PaymentHistoryService paymentHistoryService;

    @RabbitListener(queues = "queue.payment.history.db.save", concurrency = "1")
    public void save(PaymentHistoryConsumerCommand.Save command) {
        try{
            paymentHistoryService.recordPaymentHistory(
                    PaymentHistoryCommand.Save.of(command.getUserId(),command.getAmount(),command.getOrderId(),command.getTransactionId(),command.getStatus())
            );

        }catch (Exception e) {
            log.error("결제 이력 저장 실패 : {}", Utils.toJson(command));
        }

    }


}
