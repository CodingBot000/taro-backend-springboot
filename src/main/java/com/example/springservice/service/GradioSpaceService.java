package com.example.springservice.service;

import com.example.springservice.config.AppProperties;
import com.example.springservice.dto.SelectedCardResponse;
import com.example.springservice.dto.TarotResponse;
import com.example.springservice.dto.VersionResponse;
import com.example.springservice.exception.ApiException;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class GradioSpaceService {

    private static final Logger log = LoggerFactory.getLogger(GradioSpaceService.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;

    public GradioSpaceService(HttpClient httpClient, ObjectMapper objectMapper, AppProperties appProperties) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.appProperties = appProperties;
    }

    public TarotResponse generateReading(
        TarotRequestValidator.ValidatedTarotRequest request,
        QuestionAnalysisResult questionAnalysis
    ) {
        String resultPayload = callApi(
            appProperties.getHuggingFace().getApi().getGenerateReadingName(),
            List.of(
                request.question(),
                request.gradioReadingType(),
                request.selectedCardsJson(),
                request.categorySelectionJson(),
                request.uiContextJson(),
                serializeQuestionAnalysis(questionAnalysis)
            )
        );

        try {
            GradioReadingResult result = objectMapper.readValue(resultPayload, GradioReadingResult.class);
            if (result.error() != null && !result.error().isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, result.error(), "BACKEND_ERROR");
            }

            return new TarotResponse(result.cards(), result.interpretation(), result.backendVersion());
        } catch (JsonProcessingException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI 응답 형식이 올바르지 않습니다.", "BACKEND_ERROR");
        }
    }

    public VersionResponse getBackendVersion() {
        String resultPayload = callApi(appProperties.getHuggingFace().getApi().getBackendVersionName(), List.of());

        try {
            return objectMapper.readValue(resultPayload, VersionResponse.class);
        } catch (JsonProcessingException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "버전 정보를 불러오지 못했습니다.", "BACKEND_ERROR");
        }
    }

    private String callApi(String apiName, List<Object> data) {
        ensureConfigured();

        try {
            String eventId = createJob(apiName, data);
            return awaitJobResult(apiName, eventId);
        } catch (HttpTimeoutException exception) {
            throw new ApiException(HttpStatus.GATEWAY_TIMEOUT, "AI 응답 시간이 초과되었습니다. 잠시 후 다시 시도해주세요.", "TIMEOUT");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "요청 처리 중 인터럽트가 발생했습니다.", "UNKNOWN");
        } catch (IOException exception) {
            log.error("Gradio API call failed", exception);
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AI 서비스가 준비 중입니다. 잠시 후 다시 시도해주세요.", "SPACE_SLEEPING");
        }
    }

    private String createJob(String apiName, List<Object> data) throws IOException, InterruptedException {
        HttpRequest request = baseRequestBuilder(gradioCallPath(apiName))
            .timeout(appProperties.getHuggingFace().getConnectTimeout().plusSeconds(10))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(new GradioCallRequest(data))))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() >= 400) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AI 서비스가 준비 중입니다. 잠시 후 다시 시도해주세요.", "SPACE_SLEEPING");
        }

        try {
            GradioEventResponse eventResponse = objectMapper.readValue(response.body(), GradioEventResponse.class);
            if (eventResponse.eventId() == null || eventResponse.eventId().isBlank()) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "AI 서비스 응답이 올바르지 않습니다.", "BACKEND_ERROR");
            }
            return eventResponse.eventId();
        } catch (JsonProcessingException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI 서비스 응답이 올바르지 않습니다.", "BACKEND_ERROR");
        }
    }

    private String awaitJobResult(String apiName, String eventId) throws IOException, InterruptedException {
        HttpRequest request = baseRequestBuilder(gradioCallPath(apiName) + "/" + eventId)
            .timeout(appProperties.getHuggingFace().getReadTimeout())
            .GET()
            .build();

        HttpResponse<java.io.InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() >= 400) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "AI 서비스가 준비 중입니다. 잠시 후 다시 시도해주세요.", "SPACE_SLEEPING");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String event = null;
            String data = null;
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    if ("error".equals(event)) {
                        throw new ApiException(HttpStatus.BAD_GATEWAY, "AI 서비스 호출 중 오류가 발생했습니다.", "BACKEND_ERROR");
                    }
                    if ("complete".equals(event)) {
                        return extractResultPayload(data);
                    }
                    event = null;
                    data = null;
                    continue;
                }

                if (line.startsWith("event:")) {
                    event = line.substring("event:".length()).trim();
                } else if (line.startsWith("data:")) {
                    data = line.substring("data:".length()).trim();
                }
            }
        }

        throw new ApiException(HttpStatus.BAD_GATEWAY, "AI 서비스 응답이 중간에 종료되었습니다.", "BACKEND_ERROR");
    }

    private String gradioCallPath(String apiName) {
        String prefix = appProperties.getHuggingFace().getApiPrefix();
        String normalizedPrefix = (prefix == null || prefix.isBlank()) ? "" : prefix.trim();

        if (!normalizedPrefix.startsWith("/")) {
            normalizedPrefix = "/" + normalizedPrefix;
        }
        if (normalizedPrefix.endsWith("/")) {
            normalizedPrefix = normalizedPrefix.substring(0, normalizedPrefix.length() - 1);
        }

        return normalizedPrefix + "/call/" + apiName;
    }

    private String extractResultPayload(String rawData) {
        if (rawData == null || rawData.isBlank()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI 서비스 결과가 비어 있습니다.", "BACKEND_ERROR");
        }

        try {
            JsonNode dataNode = objectMapper.readTree(rawData);
            if (!dataNode.isArray() || dataNode.isEmpty() || !dataNode.get(0).isTextual()) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "AI 서비스 결과 형식이 올바르지 않습니다.", "BACKEND_ERROR");
            }
            return dataNode.get(0).asText();
        } catch (JsonProcessingException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "AI 서비스 결과 형식이 올바르지 않습니다.", "BACKEND_ERROR");
        }
    }

    private String serializeQuestionAnalysis(QuestionAnalysisResult questionAnalysis) {
        try {
            return objectMapper.writeValueAsString(questionAnalysis);
        } catch (JsonProcessingException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "질문 분석 데이터를 직렬화하지 못했습니다.", "SERVER_CONFIG_ERROR");
        }
    }

    private HttpRequest.Builder baseRequestBuilder(String path) {
        URI baseUri = normalizeSpaceUri(appProperties.getHuggingFace().getSpaceUrl());
        HttpRequest.Builder builder = HttpRequest.newBuilder(baseUri.resolve(path));

        String token = appProperties.getHuggingFace().getToken();
        if (token != null && !token.isBlank()) {
            builder.header("Authorization", "Bearer " + token.trim());
        }

        return builder;
    }

    private void ensureConfigured() {
        String spaceUrl = appProperties.getHuggingFace().getSpaceUrl();
        String token = appProperties.getHuggingFace().getToken();

        if (spaceUrl == null || spaceUrl.isBlank() || token == null || token.isBlank()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "서버 설정 오류입니다.", "SERVER_CONFIG_ERROR");
        }
    }

    private URI normalizeSpaceUri(String configuredUrl) {
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

    private record GradioCallRequest(List<Object> data) {
    }

    private record GradioEventResponse(@JsonProperty("event_id") String eventId) {
    }

    private record GradioReadingResult(
        List<SelectedCardResponse> cards,
        String interpretation,
        @JsonProperty("backend_version") String backendVersion,
        String error
    ) {
    }
}
