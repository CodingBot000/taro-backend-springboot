package com.example.springservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class QuestionAnalysisValidator {

    private final ObjectMapper objectMapper;
    private final QuestionCategoryCatalog questionCategoryCatalog;

    public QuestionAnalysisValidator(ObjectMapper objectMapper, QuestionCategoryCatalog questionCategoryCatalog) {
        this.objectMapper = objectMapper;
        this.questionCategoryCatalog = questionCategoryCatalog;
    }

    public QuestionAnalysisResult validate(String rawJson, String analysisSource) {
        if (rawJson == null || rawJson.isBlank()) {
            throw new IllegalArgumentException("Question analysis payload is empty");
        }

        JsonNode root = readObject(rawJson);

        String domain = readRequiredEnum(root, "domain", questionCategoryCatalog.domainIds());
        String subtype = readRequiredEnum(root, "subtype", questionCategoryCatalog.subCategoryIds());
        String primaryIntent = readRequiredEnum(root, "primary_intent", QuestionAnalysisSchema.INTENTS);
        List<String> secondaryIntents = readRequiredEnumArray(root, "secondary_intents", QuestionAnalysisSchema.INTENTS);
        List<String> emotionState = readRequiredEnumArray(root, "emotion_state", QuestionAnalysisSchema.EMOTION_STATES);
        String target = readRequiredEnum(root, "target", QuestionAnalysisSchema.TARGETS);
        String urgency = readRequiredEnum(root, "urgency", QuestionAnalysisSchema.URGENCY_LEVELS);
        String safetyFlag = readRequiredEnum(root, "safety_flag", QuestionAnalysisSchema.SAFETY_FLAGS);
        boolean needsClarification = readRequiredBoolean(root, "needs_clarification");
        double confidence = readRequiredConfidence(root, "confidence");

        return new QuestionAnalysisResult(
            domain,
            subtype,
            primaryIntent,
            secondaryIntents,
            emotionState,
            target,
            urgency,
            safetyFlag,
            needsClarification,
            confidence,
            normalizeAnalysisSource(analysisSource),
            QuestionAnalysisSchema.ANALYSIS_VERSION
        );
    }

    private JsonNode readObject(String rawJson) {
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            if (!root.isObject()) {
                throw new IllegalArgumentException("Question analysis payload must be a JSON object");
            }
            return root;
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Question analysis payload is not valid JSON", exception);
        }
    }

    private String readRequiredEnum(JsonNode root, String fieldName, Set<String> allowedValues) {
        JsonNode node = root.get(fieldName);
        if (node == null || !node.isTextual()) {
            throw new IllegalArgumentException("Missing or invalid field: " + fieldName);
        }

        String value = node.asText().trim();
        if (!allowedValues.contains(value)) {
            throw new IllegalArgumentException("Unsupported value for field: " + fieldName);
        }
        return value;
    }

    private List<String> readRequiredEnumArray(JsonNode root, String fieldName, Set<String> allowedValues) {
        JsonNode node = root.get(fieldName);
        if (node == null || !node.isArray()) {
            throw new IllegalArgumentException("Missing or invalid array field: " + fieldName);
        }

        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (JsonNode item : node) {
            if (!item.isTextual()) {
                throw new IllegalArgumentException("Invalid array item in field: " + fieldName);
            }

            String value = item.asText().trim();
            if (!allowedValues.contains(value)) {
                throw new IllegalArgumentException("Unsupported array value in field: " + fieldName);
            }
            values.add(value);
        }

        return List.copyOf(values);
    }

    private boolean readRequiredBoolean(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        if (node == null || !node.isBoolean()) {
            throw new IllegalArgumentException("Missing or invalid field: " + fieldName);
        }
        return node.asBoolean();
    }

    private double readRequiredConfidence(JsonNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        if (node == null || !node.isNumber()) {
            throw new IllegalArgumentException("Missing or invalid field: " + fieldName);
        }

        double confidence = node.asDouble();
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("Confidence must be between 0.0 and 1.0");
        }
        return confidence;
    }

    private String normalizeAnalysisSource(String analysisSource) {
        if (analysisSource == null || analysisSource.isBlank()) {
            return QuestionAnalysisSchema.ANALYSIS_SOURCE_OPENAI;
        }

        String normalized = analysisSource.trim().toLowerCase();
        return Set.of(
            QuestionAnalysisSchema.ANALYSIS_SOURCE_OPENAI,
            QuestionAnalysisSchema.ANALYSIS_SOURCE_FALLBACK
        ).contains(normalized)
            ? normalized
            : QuestionAnalysisSchema.ANALYSIS_SOURCE_OPENAI;
    }
}
