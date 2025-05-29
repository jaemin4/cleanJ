package com.example.demo.infra.payment;

import com.example.demo.application.payment.PaymentEventCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PaymentKafkaConsumer {

    @KafkaListener(
            topics = "external.payment.request",
            groupId = "payment-service",
            containerFactory = "kafkaListenerContainerFactory",
            concurrency = "1"
    )
    public void handlePaymentRequest(PaymentEventCommand.RequestPaymentApi event) {
        log.info("Kafka 수신됨: [external.payment.request] {}", event);

        try {
            log.info("외부 결제 처리 시작 → orderId={}, userId={}, amount={}",
                    event.getOrderId(), event.getUserId(), event.getFinalAmount());

            log.info("외부 결제 완료");

        } catch (Exception e) {
            log.error("외부 결제 처리 실패", e);
        }
    }

}
