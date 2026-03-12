package com.example.springservice.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String version = "spring-local";
    private List<String> corsAllowedOrigins = new ArrayList<>(List.of("http://localhost:3000"));
    private final RateLimit rateLimit = new RateLimit();
    private final HuggingFace huggingFace = new HuggingFace();

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<String> getCorsAllowedOrigins() {
        return corsAllowedOrigins;
    }

    public void setCorsAllowedOrigins(List<String> corsAllowedOrigins) {
        this.corsAllowedOrigins = corsAllowedOrigins;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public HuggingFace getHuggingFace() {
        return huggingFace;
    }

    public static class RateLimit {
        private int requestsPerMinute = 10;

        public int getRequestsPerMinute() {
            return requestsPerMinute;
        }

        public void setRequestsPerMinute(int requestsPerMinute) {
            this.requestsPerMinute = requestsPerMinute;
        }
    }

    public static class HuggingFace {
        private String spaceUrl = "";
        private String token = "";
        private Duration connectTimeout = Duration.ofSeconds(5);
        private Duration readTimeout = Duration.ofSeconds(130);

        public String getSpaceUrl() {
            return spaceUrl;
        }

        public void setSpaceUrl(String spaceUrl) {
            this.spaceUrl = spaceUrl;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public Duration getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
        }
    }
}
