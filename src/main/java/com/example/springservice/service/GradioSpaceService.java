package com.example.springservice.service;

import com.example.springservice.config.AppProperties;
import com.example.springservice.dto.SelectedCardResponse;
import com.example.springservice.dto.TarotResponse;
import com.example.springservice.dto.VersionResponse;
import com.example.springservice.exception.ApiException;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class GradioSpaceService {

    private final GradioApiClient gradioApiClient;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;

    public GradioSpaceService(
        GradioApiClient gradioApiClient,
        ObjectMapper objectMapper,
        AppProperties appProperties
    ) {
        this.gradioApiClient = gradioApiClient;
        this.objectMapper = objectMapper;
        this.appProperties = appProperties;
    }

    public TarotResponse generateReading(
        TarotRequestValidator.ValidatedTarotRequest request,
        QuestionAnalysisResult questionAnalysis
    ) {
        String resultPayload = gradioApiClient.callApi(
            appProperties.getHuggingFace().getApi().getGenerateReadingName(),
            List.of(
                request.question(),
                request.gradioReadingType(),
                request.selectedCardsJson(),
                request.categorySelectionJson(),
                request.uiContextJson(),
                request.intakeContextJson(),
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
        String resultPayload = gradioApiClient.callApi(appProperties.getHuggingFace().getApi().getBackendVersionName(), List.of());

        try {
            return objectMapper.readValue(resultPayload, VersionResponse.class);
        } catch (JsonProcessingException exception) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "버전 정보를 불러오지 못했습니다.", "BACKEND_ERROR");
        }
    }

    private String serializeQuestionAnalysis(QuestionAnalysisResult questionAnalysis) {
        try {
            return objectMapper.writeValueAsString(questionAnalysis);
        } catch (JsonProcessingException exception) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "질문 분석 데이터를 직렬화하지 못했습니다.", "SERVER_CONFIG_ERROR");
        }
    }

    private record GradioReadingResult(
        List<SelectedCardResponse> cards,
        String interpretation,
        @JsonProperty("backend_version") String backendVersion,
        String error
    ) {
    }
}
