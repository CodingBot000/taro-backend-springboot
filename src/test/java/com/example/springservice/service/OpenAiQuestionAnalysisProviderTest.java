package com.example.springservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.springservice.config.AppProperties;
import com.example.springservice.dto.CategorySelectionRequest;
import com.example.springservice.dto.UiContextRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OpenAiQuestionAnalysisProviderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void sendsStructuredOutputRequestAndReadsNestedOutputText() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();

        server.createContext("/v1/responses", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));

            byte[] responseBytes = """
                {
                  "output": [
                    {
                      "type": "message",
                      "content": [
                        {
                          "type": "output_text",
                          "text": "{\\"domain\\":\\"love\\",\\"subtype\\":\\"reunion\\",\\"primary_intent\\":\\"cause\\",\\"secondary_intents\\":[\\"possibility\\"],\\"emotion_state\\":[\\"anxious\\"],\\"target\\":\\"ex_partner\\",\\"urgency\\":\\"medium\\",\\"safety_flag\\":\\"none\\",\\"needs_clarification\\":false,\\"confidence\\":0.84}"
                        }
                      ]
                    }
                  ]
                }
                """.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBytes.length);
            exchange.getResponseBody().write(responseBytes);
            exchange.close();
        });

        OpenAiQuestionAnalysisProvider provider = new OpenAiQuestionAnalysisProvider(
            HttpClient.newHttpClient(),
            objectMapper,
            configuredProperties(serverBaseUrl())
        );

        String result = provider.analyze(validatedRequest("전남친이 왜 차가운지 궁금해요.", "love", "reunion"));

        assertTrue(result.contains("\"domain\":\"love\""));
        assertEquals("Bearer test-api-key", authorization.get());

        JsonNode payload = objectMapper.readTree(requestBody.get());
        assertEquals("gpt-4o-mini", payload.path("model").asText());
        assertEquals(0.1, payload.path("temperature").asDouble());
        assertEquals(220, payload.path("max_output_tokens").asInt());
        assertEquals("json_schema", payload.path("text").path("format").path("type").asText());
        assertEquals(true, payload.path("text").path("format").path("strict").asBoolean());
        assertEquals(false, payload.path("text").path("format").path("schema").path("additionalProperties").asBoolean());
        assertTrue(payload.path("input").get(1).path("content").asText().contains("UI sub category: reunion"));
    }

    @Test
    void prefersTopLevelOutputTextWhenPresent() throws Exception {
        server.createContext("/v1/responses", exchange -> {
            byte[] responseBytes = """
                {
                  "output_text": "{\\"domain\\":\\"study\\",\\"subtype\\":\\"exam_result\\",\\"primary_intent\\":\\"timing\\",\\"secondary_intents\\":[],\\"emotion_state\\":[\\"anxious\\"],\\"target\\":\\"self\\",\\"urgency\\":\\"medium\\",\\"safety_flag\\":\\"none\\",\\"needs_clarification\\":false,\\"confidence\\":0.74}"
                }
                """.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBytes.length);
            exchange.getResponseBody().write(responseBytes);
            exchange.close();
        });

        OpenAiQuestionAnalysisProvider provider = new OpenAiQuestionAnalysisProvider(
            HttpClient.newHttpClient(),
            objectMapper,
            configuredProperties(serverBaseUrl())
        );

        String result = provider.analyze(validatedRequest("시험 결과가 언제 나올까요?", "study", "exam_result"));

        assertTrue(result.contains("\"subtype\":\"exam_result\""));
    }

    @Test
    void throwsWhenHttpStatusIsError() {
        server.createContext("/v1/responses", exchange -> {
            byte[] responseBytes = "{\"error\":\"boom\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(500, responseBytes.length);
            exchange.getResponseBody().write(responseBytes);
            exchange.close();
        });

        OpenAiQuestionAnalysisProvider provider = new OpenAiQuestionAnalysisProvider(
            HttpClient.newHttpClient(),
            objectMapper,
            configuredProperties(serverBaseUrl())
        );

        assertThrows(IOException.class, () -> provider.analyze(validatedRequest("질문", "general", "today")));
    }

    private AppProperties configuredProperties(String baseUrl) {
        AppProperties appProperties = new AppProperties();
        appProperties.getQuestionAnalysis().getOpenai().setBaseUrl(baseUrl);
        appProperties.getQuestionAnalysis().getOpenai().setApiKey("test-api-key");
        appProperties.getQuestionAnalysis().getOpenai().setModel("gpt-4o-mini");
        return appProperties;
    }

    private String serverBaseUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    private TarotRequestValidator.ValidatedTarotRequest validatedRequest(
        String question,
        String mainCategoryId,
        String subCategoryId
    ) {
        CategorySelectionRequest categorySelection = new CategorySelectionRequest(mainCategoryId, subCategoryId);
        UiContextRequest uiContext = new UiContextRequest("ko", "category-v1");

        return new TarotRequestValidator.ValidatedTarotRequest(
            question,
            "원카드",
            "[{\"id\":\"major_01\",\"direction\":\"정방향\"}]",
            "{\"mainCategoryId\":\"" + mainCategoryId + "\",\"subCategoryId\":\"" + subCategoryId + "\"}",
            "{\"locale\":\"ko\",\"categoryVersion\":\"category-v1\"}",
            categorySelection,
            uiContext
        );
    }
}
