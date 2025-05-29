package com.example.demo.application.payment;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentAppDlq {

    @KafkaListener(
            topics = "order.recovery.DLT",
            groupId = "dlq-order-recovery",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleOrderDLT(ConsumerRecord<String, String> record) {
        log.error("[DLQ] order.recovery 실패 메시지 수신 - key: {}, value: {}", record.key(), record.value());
        // TODO: DB에 저장하거나 Slack 알림, 관리자 대시보드에 표시 등
    }

    @KafkaListener(
            topics = "payment.recovery.DLT",
            groupId = "dlq-payment-recovery",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentDLT(ConsumerRecord<String, String> record) {
        log.error("[DLQ] payment.recovery 실패 메시지 수신 - key: {}, value: {}", record.key(), record.value());
        // TODO: DB에 저장하거나 Slack 알림, 관리자 대시보드에 표시 등
    }
}
