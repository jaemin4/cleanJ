package com.example.demo.interceptor;

import com.example.demo.support.exception.TooManyRequestsException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final long THRESHOLD_MS = 1000;
    private final Map<String, Long> lastRequestTimeMap = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        long now = Instant.now().toEpochMilli();

        Long lastRequestTime = lastRequestTimeMap.get(uri);

        if (lastRequestTime != null && now - lastRequestTime < THRESHOLD_MS) {
            log.warn("너무 빠른 재요청 차단됨! URI: {}", uri);
            throw new TooManyRequestsException("요청이 너무 빠릅니다. 잠시 후 다시 시도해주세요.");
        }

        lastRequestTimeMap.put(uri, now);
        return true;
    }
}
