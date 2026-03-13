package com.example.springservice.config;

import com.example.springservice.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.PatternMatchUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AllowedOriginFilter extends OncePerRequestFilter {

    private static final String TAROT_API_PATH = "/api/tarot";

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    public AllowedOriginFilter(AppProperties appProperties, ObjectMapper objectMapper) {
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !appProperties.getRequestSourceValidation().isEnabled()
            || !"POST".equalsIgnoreCase(request.getMethod())
            || !TAROT_API_PATH.equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String requestOrigin = extractRequestOrigin(request);
        if (requestOrigin == null || !isAllowedOrigin(requestOrigin)) {
            writeForbiddenResponse(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractRequestOrigin(HttpServletRequest request) {
        String originHeader = normalizeOrigin(request.getHeader("Origin"));
        if (originHeader != null) {
            return originHeader;
        }

        return normalizeOrigin(request.getHeader("Referer"));
    }

    private boolean isAllowedOrigin(String requestOrigin) {
        List<String> allowedOrigins = appProperties.getCorsAllowedOrigins();
        for (String allowedOrigin : allowedOrigins) {
            if (requestOrigin.equals(normalizeOrigin(allowedOrigin))) {
                return true;
            }
        }

        List<String> allowedOriginPatterns = appProperties.getCorsAllowedOriginPatterns();
        for (String allowedOriginPattern : allowedOriginPatterns) {
            if (PatternMatchUtils.simpleMatch(allowedOriginPattern, requestOrigin)) {
                return true;
            }
        }

        return false;
    }

    private String normalizeOrigin(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            URI uri = new URI(value.trim());
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null) {
                return null;
            }

            int port = uri.getPort();
            return port >= 0
                ? String.format("%s://%s:%d", scheme, host, port)
                : String.format("%s://%s", scheme, host);
        } catch (URISyntaxException exception) {
            return null;
        }
    }

    private void writeForbiddenResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(
            response.getWriter(),
            new ErrorResponse("허용된 프론트엔드 출처에서만 요청할 수 있습니다.", "FORBIDDEN_ORIGIN")
        );
    }
}
