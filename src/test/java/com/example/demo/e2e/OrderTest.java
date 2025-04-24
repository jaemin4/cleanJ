package com.example.demo.e2e;

import com.example.demo.domain.product.Product;
import com.example.demo.domain.product.ProductRepository;
import com.example.demo.domain.product.ProductSellingStatus;
import com.example.demo.domain.stock.Stock;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureMockMvc
class OrderTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired ProductRepository productRepository;
    @Autowired StockJpaRepository stockRepository;

    Long productId1;
    Long productId2;

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
    }

    @Test
    @DisplayName("주문 요청 성공")
    void createOrderWithCoupon_success() throws Exception {

        Map<String, Object> request = Map.of(
                "userId", 1L,
                "couponId", 10L,
                "items", List.of(
                        Map.of("productId", productId1, "quantity", 2L)
                )
        );

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("items가 비어있을 경우 400 예외 발생")
    void createOrder_withEmptyItems_badRequest() throws Exception {
        Map<String, Object> request = Map.of(
                "userId", 1L,
                "items", List.of() // 빈 리스트
        );

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("userId 없이 주문 시 400 예외 발생")
    void createOrder_withNullUserId_badRequest() throws Exception {
        Map<String, Object> request = Map.of(
                "items", List.of(
                        Map.of("productId", 101L, "quantity", 2L)
                )
        );

        mockMvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").exists());
    }
}
