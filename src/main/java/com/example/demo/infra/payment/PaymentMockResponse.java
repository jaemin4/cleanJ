package com.example.demo.infra.payment;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PaymentMockResponse {
    private String transactionId;
    private String status;
    private String message;
}
