package com.example.demo.e2e;

import com.example.demo.domain.coupon.Coupon;
import com.example.demo.domain.coupon.CouponRepository;
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
import java.util.Map;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@AutoConfigureMockMvc
class CouponTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CouponRepository couponRepository;

    @BeforeEach
    void setUp(){
        couponRepository.save(Coupon.create("test",10,10L));
    }

    @Test
    @DisplayName("쿠폰 발급 성공 시 200 OK 반환")
    void issueCoupon_success() throws Exception {
        Map<String, Object> request = Map.of(
                "userId", 1L,
                "couponId", 1L
        );

        mockMvc.perform(post("/coupon/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("userId 누락 시 400 BadRequest")
    void issueCoupon_missingUserId() throws Exception {
        Map<String, Object> request = Map.of(
                "couponId", 10L
        );

        mockMvc.perform(post("/coupon/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("couponId가 음수일 경우 400 BadRequest")
    void issueCoupon_invalidCouponId() throws Exception {
        Map<String, Object> request = Map.of(
                "userId", 1L,
                "couponId", -5L
        );

        mockMvc.perform(post("/coupon/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").exists());
    }
}
