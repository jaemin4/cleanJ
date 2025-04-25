package com.example.demo.filter;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
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
}