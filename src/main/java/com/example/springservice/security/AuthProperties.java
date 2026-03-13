package com.example.springservice.security;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    private String frontendBaseUrl = "https://taro-ai-mu.vercel.app";
    private List<String> allowedFrontendOrigins = new ArrayList<>(List.of(
        "http://localhost:3000",
        "http://localhost:3100",
        "https://taro-ai-mu.vercel.app"
    ));
    private String oauth2CallbackPath = "/auth/callback";
    private String refreshCookieName = "refresh_token";
    private String refreshCookiePath = "/api/auth";
    private String refreshCookieDomain = "";
    private boolean refreshCookieSecure = true;
    private String refreshCookieSameSite = "None";

    public String getFrontendBaseUrl() {
        return frontendBaseUrl;
    }

    public void setFrontendBaseUrl(String frontendBaseUrl) {
        this.frontendBaseUrl = frontendBaseUrl;
    }

    public List<String> getAllowedFrontendOrigins() {
        return allowedFrontendOrigins;
    }

    public void setAllowedFrontendOrigins(List<String> allowedFrontendOrigins) {
        this.allowedFrontendOrigins = allowedFrontendOrigins;
    }

    public String getOauth2CallbackPath() {
        return oauth2CallbackPath;
    }

    public void setOauth2CallbackPath(String oauth2CallbackPath) {
        this.oauth2CallbackPath = oauth2CallbackPath;
    }

    public String getRefreshCookieName() {
        return refreshCookieName;
    }

    public void setRefreshCookieName(String refreshCookieName) {
        this.refreshCookieName = refreshCookieName;
    }

    public String getRefreshCookiePath() {
        return refreshCookiePath;
    }

    public void setRefreshCookiePath(String refreshCookiePath) {
        this.refreshCookiePath = refreshCookiePath;
    }

    public String getRefreshCookieDomain() {
        return refreshCookieDomain;
    }

    public void setRefreshCookieDomain(String refreshCookieDomain) {
        this.refreshCookieDomain = refreshCookieDomain;
    }

    public boolean isRefreshCookieSecure() {
        return refreshCookieSecure;
    }

    public void setRefreshCookieSecure(boolean refreshCookieSecure) {
        this.refreshCookieSecure = refreshCookieSecure;
    }

    public String getRefreshCookieSameSite() {
        return refreshCookieSameSite;
    }

    public void setRefreshCookieSameSite(String refreshCookieSameSite) {
        this.refreshCookieSameSite = refreshCookieSameSite;
    }
}
