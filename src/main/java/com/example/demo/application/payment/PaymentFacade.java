package com.example.demo.application.payment;

import com.example.demo.support.comm.aop.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentFacade {

    private final PaymentTransaction paymentTransaction;

    @DistributedLock(key = "'payment:user:' + #criteria.userId", waitTime = 3, leaseTime = 5)
    public void pay(PaymentCriteria.Payment criteria) {
        paymentTransaction.processPaymentWithTransaction(criteria);
    }




}
