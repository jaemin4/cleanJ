package com.example.demo.unit;

import com.example.demo.domain.payment.*;
import com.example.demo.infra.payment.MockPaymentService;
import com.example.demo.infra.payment.PaymentMockRequest;
import com.example.demo.infra.payment.PaymentMockResponse;
import com.example.demo.infra.payment.ResTopOrderFive;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class PaymentTest {

    private PaymentHistoryRepository paymentHistoryRepository;
    private PaymentHistoryService paymentHistoryService;

    @BeforeEach
    void setUp() {
        paymentHistoryRepository = mock(PaymentHistoryRepository.class);
        paymentHistoryService = new PaymentHistoryService(paymentHistoryRepository);
    }

    @Test
    @DisplayName("결제 기록 저장이 정상적으로 호출된다")
    void recordPaymentHistory_success() {
        // given
        Long userId = 1L;
        Long amount = 5000L;
        Long orderId = 10L;
        String transactionId = "TX123456789";
        String status = "SUCCESS";

        PaymentHistoryCommand.Save command = PaymentHistoryCommand.Save.of(
                userId, amount, orderId, transactionId, status
        );

        // when
        paymentHistoryService.recordPaymentHistory(command);

        // then
        ArgumentCaptor<PaymentHistory> captor = ArgumentCaptor.forClass(PaymentHistory.class);
        verify(paymentHistoryRepository, times(1)).save(captor.capture());

        PaymentHistory saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getAmount()).isEqualTo(amount);
        assertThat(saved.getOrderId()).isEqualTo(orderId);
        assertThat(saved.getTransactionId()).isEqualTo(transactionId);
        assertThat(saved.getStatus()).isEqualTo(status);
    }

    @Test
    @DisplayName("Mock 결제 API가 정상 호출되고 고정 응답을 반환한다")
    void callAndValidateMockApi_success() {
        // given
        RestTemplate restTemplate = mock(RestTemplate.class);
        MockPaymentService mockPaymentService = new MockPaymentService(restTemplate);

        PaymentMockRequest.Mock request = PaymentMockRequest.Mock.of(
                1L, 10L, 5000L
        );

        when(restTemplate.postForEntity(
                anyString(),
                any(),
                eq(String.class))
        ).thenReturn(ResponseEntity.ok("ok"));

        // when
        PaymentMockResponse.MockPay response = mockPaymentService.callAndValidateMockApi(request);

        // then
        assertThat(response.getTransactionId()).isEqualTo("fixed-transaction-id");
        assertThat(response.getStatus()).isEqualTo("200");
        assertThat(response.getMessage()).contains("결제 성공");
        verify(restTemplate, times(1)).postForEntity(anyString(), any(), eq(String.class));
    }

    @DisplayName("인기 상품이 정상 조회된다")
    @Test
    void getTop5Orders() {
        List<ResTopOrderFive> mockList = List.of(
                new ResTopOrderFive(101L, 20L),
                new ResTopOrderFive(102L, 18L),
                new ResTopOrderFive(103L, 16L),
                new ResTopOrderFive(104L, 15L),
                new ResTopOrderFive(105L, 14L)
        );
        when(paymentHistoryRepository.findTop5OrdersByPaidStatus()).thenReturn(mockList);

        // when
        List<PaymentHistoryInfo.Top5Orders> result = paymentHistoryService.getTop5Orders();

        // then
        assertThat(result).hasSize(5);
        assertThat(result.get(0).getOrderId()).isEqualTo(101L);
        assertThat(result.get(4).getCount()).isEqualTo(14L);
    }

}
