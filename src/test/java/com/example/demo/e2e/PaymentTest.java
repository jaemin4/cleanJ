package com.example.demo.e2e;

import com.example.demo.application.order.OrderCriteria;
import com.example.demo.application.order.OrderFacade;
import com.example.demo.application.order.OrderResult;
import com.example.demo.domain.balance.BalanceCommand;
import com.example.demo.domain.balance.BalanceService;
import com.example.demo.domain.coupon.*;
import com.example.demo.domain.order.Order;
import com.example.demo.domain.order.OrderRepository;
import com.example.demo.domain.order.OrderStatus;
import com.example.demo.domain.payment.PaymentHistory;
import com.example.demo.domain.payment.PaymentHistoryInfo;
import com.example.demo.domain.payment.PaymentHistoryRepository;
import com.example.demo.domain.payment.PaymentHistoryService;
import com.example.demo.domain.product.Product;
import com.example.demo.domain.product.ProductRepository;
import com.example.demo.domain.product.ProductSellingStatus;
import com.example.demo.domain.stock.Stock;
import com.example.demo.infra.order.OrderJpaRepository;
import com.example.demo.infra.stock.StockJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureMockMvc
public class PaymentTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired ProductRepository productRepository;
    @Autowired StockJpaRepository stockRepository;
    @Autowired OrderFacade orderFacade;
    @Autowired BalanceService balanceService;
    @Autowired CouponService couponService;
    @Autowired CouponRepository couponRepository;
    @Autowired PaymentHistoryService paymentHistoryService;
    @Autowired
    PaymentHistoryRepository paymentHistoryRepository;

    Long productId1;
    Long productId2;
    Long userId = 1L;
    Long orderId;

    @Autowired
    private UserCouponRepository userCouponRepository;

    @BeforeEach
    void setUp() {
        // 1. TODO  상품 등록
        Product p1 = Product.create("딸기케이크", 3000L, ProductSellingStatus.SELLING);
        Product p2 = Product.create("마카롱", 1500L, ProductSellingStatus.SELLING);
        productRepository.saveAll(List.of(p1, p2));

        productId1 = p1.getId();
        productId2 = p2.getId();

        // 2. TODO 재고 등록
        stockRepository.saveAll(List.of(
                new Stock(productId1, 10),
                new Stock(productId2, 10)
        ));

        // 3. TODO 잔액 충전
        balanceService.charge(BalanceCommand.Charge.of(1L,50000L));

        // 4. TODO 주문 진행
        List<OrderCriteria.OrderProduct> items = List.of(
                OrderCriteria.OrderProduct.of(productId1, 2L),
                OrderCriteria.OrderProduct.of(productId2, 3L)
        );

        OrderCriteria.Order criteria = OrderCriteria.Order.of(userId, items);
        OrderResult.Order result = orderFacade.order(criteria);
        orderId = result.getOrderId();
    }

    @Test
    @DisplayName("couponId 없이 결제 요청 성공")
    void paymentWithoutCoupon_success() throws Exception {
        Map<String, Object> request = Map.of(
                "userId", userId,
                "orderId", orderId
        );

        mockMvc.perform(post("/payments/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("couponId 포함 결제 요청 성공")
    void paymentWithCoupon_success() throws Exception {
        Coupon coupon =  couponRepository.save(Coupon.create("test",10,10L));
        couponService.issue(CouponCommand.Issue.of(userId,coupon.getId()));
        long userCouponId = userCouponRepository.findByCouponId(coupon.getId()).get().getId();

        Map<String, Object> request = Map.of(
                "userId", userId,
                "orderId",orderId ,
                "couponId", userCouponId
        );

        mockMvc.perform(post("/payments/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("userId가 누락된 경우 400 BadRequest")
    void paymentMissingUserId_shouldFail() throws Exception {
        Map<String, Object> request = Map.of(
                "orderId", 101L
        );

        mockMvc.perform(post("/payments/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("orderId가 음수일 경우 400 BadRequest")
    void paymentNegativeOrderId_shouldFail() throws Exception {
        Map<String, Object> request = Map.of(
                "userId", 1L,
                "orderId", -99L
        );

        mockMvc.perform(post("/payments/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @DisplayName("상위 5개의 결제 주문이 정상 조회된다")
    @Test
    void getPop_success() throws Exception {
        LongStream.rangeClosed(1, 10).forEach(orderId -> {
            long repeat = 11 - orderId;
            for (int j = 0; j < repeat; j++) {
                PaymentHistory history = PaymentHistory.create(
                        orderId,
                        1000L,
                        5000L,
                        "TX-" + orderId + "-" + j,
                        "SUCCESS"
                );
                paymentHistoryRepository.save(history);
            }
        });

        // when & then: 가장 많이 결제된 순서대로 5개만 응답으로 와야 함
        mockMvc.perform(get("/payments/get/pop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(5))
                .andExpect(jsonPath("$.data[0].orderId").value(1))
                .andExpect(jsonPath("$.data[0].count").value(10))
                .andExpect(jsonPath("$.data[1].orderId").value(2))
                .andExpect(jsonPath("$.data[1].count").value(9))
                .andExpect(jsonPath("$.data[4].orderId").value(5))
                .andExpect(jsonPath("$.data[4].count").value(6));
    }



}
