package com.example.demo.interfaces.payment;

import com.example.demo.application.payment.PaymentFacade;
import com.example.demo.support.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {
    private final PaymentFacade paymentFacade;

    @PostMapping(value = "/pay")
    public ApiResponse<Void> payment(@RequestBody PaymentRequest.Payment request) {
        log.info("[PaymentController] 결제 요청 수신: userId={}, orderId={}", request.getUserId(), request.getOrderId());

        paymentFacade.pay(request.toCriteria());

        return ApiResponse.success();
    }
}
