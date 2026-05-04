package com.example.springservice.service;

import com.example.springservice.exception.ApiException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class GradioSpaceUriResolver {

    public URI resolveBaseUri(String configuredUrl) {
        String trimmed = configuredUrl == null ? "" : configuredUrl.trim();
        if (trimmed.isBlank()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "서버 설정 오류입니다.", "SERVER_CONFIG_ERROR");
        }

        try {
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                return URI.create(trimmed.endsWith("/") ? trimmed : trimmed + "/");
            }

            String[] parts = trimmed.split("/");
            if (parts.length != 2) {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "HF_SPACE_URL 형식이 올바르지 않습니다.", "SERVER_CONFIG_ERROR");
            }

            String host = (parts[0] + "-" + parts[1]).toLowerCase(Locale.ROOT);
            return new URI("https://" + host + ".hf.space/");
        } catch (URISyntaxException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "HF_SPACE_URL 형식이 올바르지 않습니다.", "SERVER_CONFIG_ERROR");
        }
    }

    public String resolveCallPath(String apiPrefix, String apiName) {
        String normalizedPrefix = (apiPrefix == null || apiPrefix.isBlank()) ? "" : apiPrefix.trim();

        if (!normalizedPrefix.startsWith("/")) {
            normalizedPrefix = "/" + normalizedPrefix;
        }
        if (normalizedPrefix.endsWith("/")) {
            normalizedPrefix = normalizedPrefix.substring(0, normalizedPrefix.length() - 1);
        }

        return normalizedPrefix + "/call/" + apiName;
    }
}
