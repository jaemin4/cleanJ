package com.example.demo.filter;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;

@Getter
@Builder
public class AccessLogRequest {
    private String method;
    private String uri;
    private String query;
    private String requestBody;
    private String responseBody;
    private String headers;
    private String userAgent;
    private String remoteIp;
    private int status;
    private String threadName;
    private LocalDateTime requestAt;
    private LocalDateTime responseAt;
    private long durationMs;

    public static String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

}
