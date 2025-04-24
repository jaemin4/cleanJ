package com.example.demo.interfaces.payment;

import com.example.demo.application.payment.PaymentFacade;
import com.example.demo.domain.payment.PaymentHistoryService;
import com.example.demo.support.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {
    private final PaymentFacade paymentFacade;
    private final PaymentHistoryService paymentHistoryService;

    @PostMapping(value = "/pay")
    public ApiResponse<Void> payment(@Valid @RequestBody PaymentRequest.Payment request) {
        log.info("[PaymentController] 결제 요청 수신: userId={}, orderId={}", request.getUserId(), request.getOrderId());
        paymentFacade.pay(request.toCriteria());
        return ApiResponse.success();
    }

    @GetMapping(value = "/get/pop")
    public ApiResponse<List<PaymentResponse.Top5Orders>> getPop(){
        return ApiResponse.success(PaymentResponse.Top5Orders.toResponseList(paymentHistoryService.getTop5Orders()));
    }
}
