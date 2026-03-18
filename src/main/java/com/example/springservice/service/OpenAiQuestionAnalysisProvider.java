package com.example.springservice.service;

import com.example.springservice.config.AppProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class OpenAiQuestionAnalysisProvider implements QuestionAnalysisProvider {

    private static final String SYSTEM_PROMPT = """
        You are a classification engine for a Korean tarot service.
        Analyze the user's question and return only JSON that exactly matches the provided schema.
        Do not perform tarot reading, advice writing, or card interpretation.
        Use only the allowed enum values.
        The user's question semantics are more important than the UI category hints. Use UI category only as a weak hint.
        Select exactly one primary_intent that best matches the user's main ask, not a generic tarot framing.
        Put any additional asks into secondary_intents instead of changing primary_intent.
        Intent selection rules:
        - Choose cause when the user asks why something happened or what caused distance, conflict, or a breakup.
        - Choose comparison when the user compares two or more options, such as staying vs leaving, investing vs waiting, or yes vs no.
        - Choose timing when the user asks when, how soon, or at what point something will become clear.
        - Choose possibility when the user mainly asks whether something can happen or whether contact/reunion is possible.
        - Choose advice when the user mainly asks what they should do.
        - Choose future_flow when the user mainly asks how a situation will unfold.
        - Choose overall_guidance only when the question is broad and none of the more specific intents clearly dominate.
        For multi-intent questions, choose the first and most emphasized ask as primary_intent and place the rest in secondary_intents.
        Subtype rules:
        - Choose reunion when the question is about an ex-partner, renewed contact, getting back together, or whether reconciliation is possible.
        - Choose after_breakup when the focus is the breakup aftermath or breakup reason without a clear renewed-contact angle.
        - Choose distance_conflict when the question is about drifting apart, conflict, or emotional distance in a friendship or non-romantic relationship.
        - Do not use subtype unknown when the relationship/person and event are already explicit in the question.
        Clarification rules:
        - Set needs_clarification to false when the question already identifies the target person or situation and a concrete ask can be inferred.
        - Do not set needs_clarification to true just because the question has multiple intents.
        - Use needs_clarification=true only when the domain or subtype genuinely cannot be inferred from the wording.
        If the question is ambiguous, use subtype "unknown", set needs_clarification to true, and lower confidence.
        If no secondary intent or emotion applies, return an empty array.
        """;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;
    private final QuestionCategoryCatalog questionCategoryCatalog;

    public OpenAiQuestionAnalysisProvider(
        HttpClient httpClient,
        ObjectMapper objectMapper,
        AppProperties appProperties,
        QuestionCategoryCatalog questionCategoryCatalog
    ) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.appProperties = appProperties;
        this.questionCategoryCatalog = questionCategoryCatalog;
    }

    @Override
    public String name() {
        return QuestionAnalysisSchema.ANALYSIS_SOURCE_OPENAI;
    }

    @Override
    public String analyze(TarotRequestValidator.ValidatedTarotRequest request) throws IOException, InterruptedException {
        AppProperties.OpenAi openAi = appProperties.getQuestionAnalysis().getOpenai();
        ensureConfigured(openAi);

        HttpRequest httpRequest = HttpRequest.newBuilder(responsesUri(openAi.getBaseUrl()))
            .timeout(appProperties.getQuestionAnalysis().getTimeout())
            .header("Authorization", "Bearer " + openAi.getApiKey().trim())
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(buildRequestBody(request, openAi.getModel().trim()), StandardCharsets.UTF_8))
            .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() >= 400) {
            throw new IOException("OpenAI question analysis request failed with status " + response.statusCode());
        }

        return extractStructuredOutput(response.body());
    }

    private String buildRequestBody(TarotRequestValidator.ValidatedTarotRequest request, String model) throws JsonProcessingException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("input", List.of(
            Map.of("role", "system", "content", SYSTEM_PROMPT),
            Map.of("role", "user", "content", buildUserPrompt(request))
        ));
        payload.put("temperature", 0.1);
        payload.put("max_output_tokens", 220);
        payload.put("text", Map.of(
            "format",
            QuestionAnalysisSchema.openAiResponseFormat(questionCategoryCatalog.domainIds(), questionCategoryCatalog.subCategoryIds())
        ));
        return objectMapper.writeValueAsString(payload);
    }

    private String buildUserPrompt(TarotRequestValidator.ValidatedTarotRequest request) {
        return """
            User question: %s
            Reading type: %s
            UI main category: %s
            UI sub category: %s
            """.formatted(
            request.question(),
            request.gradioReadingType(),
            request.categorySelection().mainCategoryId(),
            request.categorySelection().subCategoryId()
        ).trim();
    }

    private String extractStructuredOutput(String responseBody) throws IOException {
        try {
            JsonNode root = objectMapper.readTree(responseBody);

            JsonNode outputText = root.get("output_text");
            if (outputText != null && outputText.isTextual() && !outputText.asText().isBlank()) {
                return outputText.asText();
            }

            JsonNode output = root.get("output");
            if (output != null && output.isArray()) {
                for (JsonNode item : output) {
                    JsonNode content = item.get("content");
                    if (content == null || !content.isArray()) {
                        continue;
                    }

                    for (JsonNode contentItem : content) {
                        if (!"output_text".equals(contentItem.path("type").asText())) {
                            continue;
                        }

                        JsonNode text = contentItem.get("text");
                        if (text != null && text.isTextual() && !text.asText().isBlank()) {
                            return text.asText();
                        }
                    }
                }
            }
        } catch (JsonProcessingException exception) {
            throw new IOException("OpenAI question analysis response is not valid JSON", exception);
        }

        throw new IOException("OpenAI question analysis response did not contain output text");
    }

    private URI responsesUri(String baseUrl) {
        String normalizedBaseUrl = normalizeBaseUrl(baseUrl);
        return URI.create(normalizedBaseUrl + "/v1/responses");
    }

    private String normalizeBaseUrl(String baseUrl) {
        String trimmed = baseUrl == null ? "" : baseUrl.trim();
        if (trimmed.isBlank()) {
            throw new IllegalStateException("Question analysis OpenAI base URL is not configured");
        }

        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private void ensureConfigured(AppProperties.OpenAi openAi) {
        if (openAi.getApiKey() == null || openAi.getApiKey().isBlank()) {
            throw new IllegalStateException("Question analysis OpenAI API key is not configured");
        }
        if (openAi.getModel() == null || openAi.getModel().isBlank()) {
            throw new IllegalStateException("Question analysis OpenAI model is not configured");
        }
    }
}
