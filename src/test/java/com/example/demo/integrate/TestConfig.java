package com.example.demo.integrate;

import com.example.demo.domain.payment.PaymentHistoryCommand;
import com.example.demo.domain.payment.PaymentHistoryService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestConfig {
    @Bean
    @Primary
    public PaymentHistoryService mockFailingPaymentHistoryService() {
        return new PaymentHistoryService(null,null,null) {
            @Override
            public void recordPaymentHistory(PaymentHistoryCommand.Save command) {
                throw new IllegalStateException("강제 결제 이력 저장 실패");
            }
        };
    }

}
