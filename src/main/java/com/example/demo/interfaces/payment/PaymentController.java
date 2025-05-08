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
@RequestMapping("/pay")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {
    private final PaymentFacade paymentFacade;
    private final PaymentHistoryService paymentHistoryService;

    @PostMapping
    public ApiResponse<Void> payment(@Valid @RequestBody PaymentRequest.Payment request) {
        log.info("[PaymentController] 결제 요청 수신: userId={}, orderId={}", request.getUserId(), request.getOrderId());
        paymentFacade.pay(request.toCriteria());
        return ApiResponse.success();
    }

    @GetMapping(value = "/getPopular")
    public ApiResponse<List<PaymentResponse.Top5OrdersCaching>> getPop(){
        return ApiResponse.success(PaymentResponse.Top5OrdersCaching.toResponseList(paymentHistoryService.getPopularProductsFromCache()));
    }
}
