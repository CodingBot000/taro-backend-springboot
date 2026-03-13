package com.example.springservice.service;

import com.example.springservice.config.AppProperties;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class RequestRateLimiter {

    private static final long RATE_WINDOW_MILLIS = 60_000L;

    private final Map<String, Deque<Long>> requestLog = new ConcurrentHashMap<>();
    private final int requestsPerMinute;

    public RequestRateLimiter(AppProperties appProperties) {
        this.requestsPerMinute = appProperties.getRateLimit().getRequestsPerMinute();
    }

    public boolean isRateLimited(String clientIp) {
        String key = (clientIp == null || clientIp.isBlank()) ? "unknown" : clientIp;
        Deque<Long> timestamps = requestLog.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        long now = Instant.now().toEpochMilli();

        synchronized (timestamps) {
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() >= RATE_WINDOW_MILLIS) {
                timestamps.removeFirst();
            }

            if (timestamps.size() >= requestsPerMinute) {
                return true;
            }

            timestamps.addLast(now);
            return false;
        }
    }
}
