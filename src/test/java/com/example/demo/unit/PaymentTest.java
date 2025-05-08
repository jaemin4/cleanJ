package com.example.demo.unit;

import com.example.demo.domain.payment.*;
import com.example.demo.infra.payment.MockPaymentService;
import com.example.demo.infra.payment.PaymentMockRequest;
import com.example.demo.infra.payment.PaymentMockResponse;
import com.example.demo.infra.payment.ResTopOrderFive;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

public class PaymentTest {

    private PaymentHistoryRepository paymentHistoryRepository;
    private PaymentHistoryService paymentHistoryService;
    private RedisTemplate<String,Object> redisTemplate;
    private ObjectMapper objectMapper;
    @BeforeEach
    void setUp() {
        paymentHistoryRepository = mock(PaymentHistoryRepository.class);
        paymentHistoryService = new PaymentHistoryService(paymentHistoryRepository,redisTemplate,objectMapper);
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
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
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


    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock lock;

    private static final String POPULAR_PRODUCTS_KEY = "popular:top5";

    @Test
    @DisplayName("캐시 hit 시 바로 반환")
    void getPopularProducts_cacheHit() throws Exception {
        String json = "[{\"orderId\":101,\"count\":20}]";
        List<PaymentHistoryInfo.Top5OrdersForCaching> expected = List.of(
                new PaymentHistoryInfo.Top5OrdersForCaching(101L, 20L)
        );

        given(redisTemplate.opsForValue().get(POPULAR_PRODUCTS_KEY)).willReturn(json);
        given(objectMapper.readValue(eq(json), any(TypeReference.class))).willReturn(expected);

        List<PaymentHistoryInfo.Top5OrdersForCaching> result = paymentPopularService.getPopularProducts();

        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("캐시 miss → 락 획득 성공 → DB 조회 및 캐시 저장")
    void getPopularProducts_cacheMissWithLock() throws Exception {
        given(redisTemplate.opsForValue().get(POPULAR_PRODUCTS_KEY)).willReturn(null);
        given(redissonClient.getLock(anyString())).willReturn(lock);
        given(lock.tryLock(anyLong(), anyLong(), any())).willReturn(true);

        List<ResTopOrderFive> dbList = List.of(new ResTopOrderFive(101L, 20L));
        given(paymentHistoryRepository.findTop5OrdersByPaidStatus()).willReturn(dbList);

        List<PaymentHistoryInfo.Top5OrdersForCaching> expected = PaymentHistoryInfo.Top5OrdersForCaching.fromResTopList(dbList);
        given(objectMapper.writeValueAsString(any())).willReturn("cached-json");

        List<PaymentHistoryInfo.Top5OrdersForCaching> result = paymentHistoryService.getPopularProducts();

        assertThat(result).isEqualTo(expected);
        verify(redisTemplate.opsForValue()).set(eq(POPULAR_PRODUCTS_KEY), eq("cached-json"), any(Duration.class));
    }

}
