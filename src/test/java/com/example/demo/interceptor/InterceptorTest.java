package com.example.demo.interceptor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class InterceptorTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("1초 이내 중복 요청 시 예외 429 응답 반환한다.")
    void testRateLimitInterceptor_throwsExceptionAndReturns429() throws Exception {
        String url = "/test-endpoint";

        // 첫 요청 - 정상 처리
        mockMvc.perform(get(url))
                .andExpect(status().isOk());

        //Thread.sleep(500);

        // 두 번째 요청 - 429 예외 발생
        mockMvc.perform(get(url))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value(429))
                .andExpect(jsonPath("$.message").value("요청이 너무 빠릅니다. 잠시 후 다시 시도해주세요."));
    }

    @Test
    @DisplayName("1초 이상 후 재요청은 허용된다.")
    void testRateLimitInterceptor_afterDelay_passes() throws Exception {
        String url = "/test-endpoint";

        // 첫 요청 - 정상 처리
        mockMvc.perform(get(url))
                .andExpect(status().isOk());

        Thread.sleep(1100);

        // 두번째 요청 - 정상 처리
        mockMvc.perform(get(url))
                .andExpect(status().isOk());
    }
}
