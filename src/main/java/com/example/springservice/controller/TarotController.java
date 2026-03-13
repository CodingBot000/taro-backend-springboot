package com.example.springservice.controller;

import com.example.springservice.dto.TarotRequest;
import com.example.springservice.dto.TarotResponse;
import com.example.springservice.dto.VersionResponse;
import com.example.springservice.exception.ApiException;
import com.example.springservice.service.RequestRateLimiter;
import com.example.springservice.service.TarotService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TarotController {

    private final TarotService tarotService;
    private final RequestRateLimiter requestRateLimiter;

    public TarotController(TarotService tarotService, RequestRateLimiter requestRateLimiter) {
        this.tarotService = tarotService;
        this.requestRateLimiter = requestRateLimiter;
    }

    @GetMapping("/version")
    public VersionResponse getVersion() {
        return tarotService.getBackendVersion();
    }

    @PostMapping("/tarot")
    public TarotResponse createReading(@RequestBody TarotRequest request, HttpServletRequest httpRequest) {
        String clientIp = resolveClientIp(httpRequest);
        if (requestRateLimiter.isRateLimited(clientIp)) {
            throw new ApiException(
                HttpStatus.TOO_MANY_REQUESTS,
                "요청이 너무 많습니다. 잠시 후 다시 시도해주세요.",
                "RATE_LIMITED"
            );
        }

        return tarotService.createReading(request);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String cfConnectingIp = request.getHeader("CF-Connecting-IP");
        if (cfConnectingIp != null && !cfConnectingIp.isBlank()) {
            return cfConnectingIp.trim();
        }

        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }
}
