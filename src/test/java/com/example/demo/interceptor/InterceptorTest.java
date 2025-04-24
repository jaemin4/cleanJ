package com.example.demo.interceptor;

import com.example.demo.support.exception.TooManyRequestsException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class InterceptorTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void 같은_세션_1초_이내_두_번_요청시_TooManyRequestsException_발생() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(get("/test-endpoint")
                        .session(session))
                .andExpect(status().isOk());

        mockMvc.perform(get("/test-endpoint")
                        .session(session))
                .andExpect(status().isTooManyRequests())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof TooManyRequestsException));
    }

    @Test
    public void 다른_세션은_동시요청_가능() throws Exception {
        MockHttpSession session1 = new MockHttpSession();
        MockHttpSession session2 = new MockHttpSession();

        mockMvc.perform(get("/test-endpoint")
                        .session(session1))
                .andExpect(status().isOk());

        mockMvc.perform(get("/test-endpoint")
                        .session(session2))
                .andExpect(status().isOk());
    }


}
