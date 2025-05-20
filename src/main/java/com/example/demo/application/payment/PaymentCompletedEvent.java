package com.example.demo.application.payment;

public record PaymentCompletedEvent(
        PaymentCriteria.Payment criteria,
        PaymentTransactionResult.Payment result
) {}