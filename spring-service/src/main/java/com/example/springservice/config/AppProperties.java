package com.example.springservice.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String version = "spring-local";
    private List<String> corsAllowedOrigins = new ArrayList<>(List.of("http://localhost:3000"));
    private List<String> corsAllowedOriginPatterns = new ArrayList<>();
    private final RateLimit rateLimit = new RateLimit();
    private final RequestSourceValidation requestSourceValidation = new RequestSourceValidation();
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

    public List<String> getCorsAllowedOriginPatterns() {
        return corsAllowedOriginPatterns;
    }

    public void setCorsAllowedOriginPatterns(List<String> corsAllowedOriginPatterns) {
        this.corsAllowedOriginPatterns = corsAllowedOriginPatterns;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public RequestSourceValidation getRequestSourceValidation() {
        return requestSourceValidation;
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

    public static class RequestSourceValidation {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class HuggingFace {
        private String spaceUrl = "";
        private String token = "";
        private String apiPrefix = "/gradio_api";
        private Duration connectTimeout = Duration.ofSeconds(5);
        private Duration readTimeout = Duration.ofSeconds(130);
        private final Api api = new Api();

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

        public String getApiPrefix() {
            return apiPrefix;
        }

        public void setApiPrefix(String apiPrefix) {
            this.apiPrefix = apiPrefix;
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

        public Api getApi() {
            return api;
        }
    }

    public static class Api {
        private String generateReadingName = "generate_reading";
        private String backendVersionName = "backend_version";

        public String getGenerateReadingName() {
            return generateReadingName;
        }

        public void setGenerateReadingName(String generateReadingName) {
            this.generateReadingName = generateReadingName;
        }

        public String getBackendVersionName() {
            return backendVersionName;
        }

        public void setBackendVersionName(String backendVersionName) {
            this.backendVersionName = backendVersionName;
        }
    }
}
