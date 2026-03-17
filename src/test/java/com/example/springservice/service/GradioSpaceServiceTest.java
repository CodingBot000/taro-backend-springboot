package com.example.springservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.springservice.config.AppProperties;
import com.example.springservice.dto.CategorySelectionRequest;
import com.example.springservice.dto.TarotResponse;
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

class GradioSpaceServiceTest {

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
    void sendsQuestionAnalysisJsonAsSixthArgument() throws Exception {
        AtomicReference<String> createJobBody = new AtomicReference<>();

        server.createContext("/gradio_api/call/generate_reading", exchange -> {
            createJobBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] responseBytes = "{\"event_id\":\"event-1\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, responseBytes.length);
            exchange.getResponseBody().write(responseBytes);
            exchange.close();
        });

        server.createContext("/gradio_api/call/generate_reading/event-1", exchange -> {
            String resultPayload = objectMapper.writeValueAsString(
                java.util.Map.of(
                    "cards", java.util.List.of(
                        java.util.Map.of("id", "major_01", "name", "마법사", "direction", "정방향")
                    ),
                    "interpretation", "테스트 해석입니다.",
                    "backend_version", "hf-test"
                )
            );
            String sseBody = "event: complete\n" +
                "data: " + objectMapper.writeValueAsString(java.util.List.of(resultPayload)) + "\n\n";
            byte[] responseBytes = sseBody.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, responseBytes.length);
            exchange.getResponseBody().write(responseBytes);
            exchange.close();
        });

        GradioSpaceService service = new GradioSpaceService(
            HttpClient.newHttpClient(),
            objectMapper,
            configuredProperties()
        );

        TarotResponse response = service.generateReading(validatedRequest(), questionAnalysisResult());

        JsonNode payload = objectMapper.readTree(createJobBody.get());
        JsonNode data = payload.path("data");
        assertEquals(6, data.size());
        assertEquals("원카드", data.get(1).asText());
        assertEquals("relationship_conflict", objectMapper.readTree(data.get(5).asText()).path("subtype").asText());
        assertEquals("openai", objectMapper.readTree(data.get(5).asText()).path("analysis_source").asText());
        assertEquals("테스트 해석입니다.", response.interpretation());
        assertEquals("hf-test", response.backendVersion());
    }

    private AppProperties configuredProperties() {
        AppProperties appProperties = new AppProperties();
        appProperties.getHuggingFace().setSpaceUrl("http://localhost:" + server.getAddress().getPort());
        appProperties.getHuggingFace().setToken("hf-test-token");
        appProperties.getHuggingFace().setApiPrefix("/gradio_api");
        appProperties.getHuggingFace().getApi().setGenerateReadingName("generate_reading");
        return appProperties;
    }

    private TarotRequestValidator.ValidatedTarotRequest validatedRequest() {
        CategorySelectionRequest categorySelection = new CategorySelectionRequest("love", "relationship_conflict");
        UiContextRequest uiContext = new UiContextRequest("ko", "category-v1");

        return new TarotRequestValidator.ValidatedTarotRequest(
            "갈등이 어떻게 풀릴까요?",
            "원카드",
            "[{\"id\":\"major_01\",\"direction\":\"정방향\"}]",
            "{\"mainCategoryId\":\"love\",\"subCategoryId\":\"relationship_conflict\"}",
            "{\"locale\":\"ko\",\"categoryVersion\":\"category-v1\"}",
            categorySelection,
            uiContext
        );
    }

    private QuestionAnalysisResult questionAnalysisResult() {
        return new QuestionAnalysisResult(
            "love",
            "relationship_conflict",
            "future_flow",
            java.util.List.of("advice"),
            java.util.List.of("confused"),
            "current_partner",
            "medium",
            "none",
            false,
            0.72,
            "openai",
            "v1"
        );
    }
}
