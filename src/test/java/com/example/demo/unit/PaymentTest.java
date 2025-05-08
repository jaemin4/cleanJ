package com.example.demo.unit;

import com.example.demo.domain.payment.*;
import com.example.demo.infra.payment.MockPaymentService;
import com.example.demo.infra.payment.PaymentMockRequest;
import com.example.demo.infra.payment.PaymentMockResponse;
import com.example.demo.infra.payment.ResTopOrderFive;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

public class PaymentTest {

    private RedisTemplate<String, Object> redisTemplate;
    private ValueOperations<String, Object> valueOperations;
    private ObjectMapper objectMapper;
    private RedissonClient redissonClient;
    private RLock lock;
    private PaymentHistoryRepository paymentHistoryRepository;
    private PaymentHistoryService paymentHistoryService;

    private static final String POPULAR_PRODUCTS_KEY = "popular:top5";

    @BeforeEach
    void setUp() {
        redisTemplate = mock(RedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations); // 필수 설정

        objectMapper = mock(ObjectMapper.class);
        redissonClient = mock(RedissonClient.class);
        lock = mock(RLock.class);
        paymentHistoryRepository = mock(PaymentHistoryRepository.class);

        paymentHistoryService = new PaymentHistoryService(
                paymentHistoryRepository, redisTemplate, objectMapper, redissonClient
        );
    }

    @Test
    @DisplayName("결제 기록 저장이 정상적으로 호출된다")
    void recordPaymentHistory_success() {
        Long userId = 1L;
        Long amount = 5000L;
        Long orderId = 10L;
        String transactionId = "TX123456789";
        String status = "SUCCESS";

        PaymentHistoryCommand.Save command = PaymentHistoryCommand.Save.of(userId, amount, orderId, transactionId, status);

        paymentHistoryService.recordPaymentHistory(command);

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
        RestTemplate restTemplate = mock(RestTemplate.class);
        MockPaymentService mockPaymentService = new MockPaymentService(restTemplate);

        PaymentMockRequest.Mock request = PaymentMockRequest.Mock.of(1L, 10L, 5000L);

        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("ok"));

        PaymentMockResponse.MockPay response = mockPaymentService.callAndValidateMockApi(request);

        assertThat(response.getTransactionId()).isEqualTo("fixed-transaction-id");
        assertThat(response.getStatus()).isEqualTo("SUCCESS");
        assertThat(response.getMessage()).contains("결제 성공");
    }

    @Test
    @DisplayName("인기 상품이 정상 조회된다")
    void getTop5Orders() {
        List<ResTopOrderFive> mockList = List.of(
                new ResTopOrderFive(101L, 20L),
                new ResTopOrderFive(102L, 18L),
                new ResTopOrderFive(103L, 16L),
                new ResTopOrderFive(104L, 15L),
                new ResTopOrderFive(105L, 14L)
        );
        when(paymentHistoryRepository.findTop5OrdersByPaidStatus()).thenReturn(mockList);

        List<PaymentHistoryInfo.Top5Orders> result = paymentHistoryService.getTop5Orders();

        assertThat(result).hasSize(5);
        assertThat(result.get(0).getOrderId()).isEqualTo(101L);
        assertThat(result.get(4).getCount()).isEqualTo(14L);
    }

    @Test
    @DisplayName("캐시 hit 시 바로 반환")
    void getPopularProducts_cacheHit() throws Exception {
        String json = "[{\"orderId\":101,\"count\":20}]";
        List<PaymentHistoryInfo.Top5OrdersForCaching> expected = List.of(
                new PaymentHistoryInfo.Top5OrdersForCaching(101L, 20L)
        );

        given(valueOperations.get(POPULAR_PRODUCTS_KEY)).willReturn(json);
        given(objectMapper.readValue(eq(json), any(TypeReference.class))).willReturn(expected);

        List<PaymentHistoryInfo.Top5OrdersForCaching> result = paymentHistoryService.getPopularProducts();

        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("캐시 miss → 락 획득 실패 → 재시도 후 캐시 hit")
    void getPopularProducts_lockFailThenRetryHit() throws Exception {
        // given
        given(valueOperations.get(POPULAR_PRODUCTS_KEY))
                .willReturn(null)
                .willReturn("[{\"orderId\":101,\"count\":20}]");

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(redissonClient.getLock(anyString())).willReturn(lock);
        given(lock.tryLock(anyLong(), anyLong(), any())).willReturn(false);

        List<PaymentHistoryInfo.Top5OrdersForCaching> expected = List.of(
                new PaymentHistoryInfo.Top5OrdersForCaching(101L, 20L)
        );
        given(objectMapper.readValue(anyString(), any(TypeReference.class))).willReturn(expected);

        List<PaymentHistoryInfo.Top5OrdersForCaching> result = paymentHistoryService.getPopularProducts();

        assertThat(result)
                .usingRecursiveFieldByFieldElementComparator()
                .isEqualTo(expected);
    }


    @Test
    @DisplayName("캐시 miss → 락 획득 성공 → DB 조회 및 캐시 저장")
    void getPopularProducts_cacheMissWithLock() throws Exception {
        given(valueOperations.get(POPULAR_PRODUCTS_KEY)).willReturn(null);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(redissonClient.getLock(anyString())).willReturn(lock);
        given(lock.tryLock(anyLong(), anyLong(), any())).willReturn(true);

        List<ResTopOrderFive> dbList = List.of(new ResTopOrderFive(101L, 20L));
        given(paymentHistoryRepository.findTop5OrdersByPaidStatus()).willReturn(dbList);

        given(objectMapper.writeValueAsString(any())).willReturn("cached-json");

        List<PaymentHistoryInfo.Top5OrdersForCaching> expected =
                PaymentHistoryInfo.Top5OrdersForCaching.fromResTopList(dbList);

        List<PaymentHistoryInfo.Top5OrdersForCaching> result = paymentHistoryService.getPopularProducts();

        assertThat(result)
                .usingRecursiveFieldByFieldElementComparator()
                .isEqualTo(expected);

        verify(valueOperations).set(eq(POPULAR_PRODUCTS_KEY), eq("cached-json"), any(Duration.class));
    }

    @Test
    @DisplayName("캐시 miss → 락 획득 실패 → 재시도 후에도 캐시 없음 → 예외 발생")
    void getPopularProducts_lockFailThenRetryFail() throws InterruptedException {
        // given
        given(valueOperations.get(POPULAR_PRODUCTS_KEY)).willReturn(null);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(redissonClient.getLock(anyString())).willReturn(lock);
        given(lock.tryLock(anyLong(), anyLong(), any())).willReturn(false);

        // when & then
        assertThatThrownBy(() -> paymentHistoryService.getPopularProducts())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("인기 상품 조회 실패");
    }



}
