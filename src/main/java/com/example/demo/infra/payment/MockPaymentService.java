package com.example.demo.infra.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class MockPaymentService {

    private final RestTemplate restTemplate;

    private static final String MOCK_API_URL = "https://67ed717f4387d9117bbda6b1.mockapi.io/api/pay/test";

    public PaymentMockResponse.MockPay callAndValidateMockApi(PaymentMockRequest.Mock request) {
        try {
            Long orderId = request.getOrderId();
            Long userId = request.getUserId();
            Long amount = request.getAmount();

            log.info("[MOCK 결제사] POST 호출: {}", MOCK_API_URL);
            log.info("요청 바디: orderId={}, userId={}, amount={}", orderId, userId, amount);

            // 실제로 POST 요청 전송 (응답은 무시)
            restTemplate.postForEntity(
                    MOCK_API_URL,
                    request,
                    String.class
            );

            log.info("고정 결제 성공 응답 반환");
            return PaymentMockResponse.MockPay.of(
                    "fixed-transaction-id",
                    "SUCCESS",
                    "결제 성공 (Mock 고정 응답)"
            );

        } catch (Exception e) {
            log.error("Mock 결제 API 호출 중 예외 발생", e);
            throw new RuntimeException("Mock 결제 API 호출 실패", e);
        }
    }
}
