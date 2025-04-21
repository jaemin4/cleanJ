package com.example.demo.interfaces.balance;

import com.example.demo.domain.balance.BalanceService;
import com.example.demo.support.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/balance")
@RequiredArgsConstructor
@Slf4j
public class BalanceController {

    private final BalanceService balanceService;

    @PostMapping("/charge")
    public ApiResponse<Void> charge(@Valid @RequestBody BalanceRequest.Charge request) {
        log.info("[BalanceController] 충전 요청 수신: userId={}, amount={}",
                request.getUserId(), request.getAmount());

        balanceService.charge(request.toCommand());

        log.info("[BalanceController] 충전 요청 처리 완료: userId={}", request.getUserId());
        return ApiResponse.success();
    }

}
