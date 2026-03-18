package com.example.springservice.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class QuestionAnalysisSchema {

    static final String ANALYSIS_SOURCE_OPENAI = "openai";
    static final String ANALYSIS_SOURCE_FALLBACK = "fallback";
    static final String ANALYSIS_VERSION = "v1";

    static final Set<String> INTENTS = Set.of(
        "possibility",
        "cause",
        "future_flow",
        "advice",
        "comparison",
        "timing",
        "overall_guidance"
    );

    static final Set<String> EMOTION_STATES = Set.of(
        "anxious",
        "attached",
        "confused",
        "hopeful",
        "angry",
        "sad"
    );

    static final Set<String> TARGETS = Set.of(
        "self",
        "ex_partner",
        "current_partner",
        "crush",
        "boss",
        "coworker",
        "friend",
        "family",
        "unknown"
    );

    static final Set<String> URGENCY_LEVELS = Set.of(
        "low",
        "medium",
        "high"
    );

    static final Set<String> SAFETY_FLAGS = Set.of(
        "none",
        "self_harm",
        "stalking",
        "abuse",
        "coercion",
        "minor_sensitive"
    );

    private QuestionAnalysisSchema() {
    }

    static Map<String, Object> openAiResponseFormat(Set<String> domainIds, Set<String> subCategoryIds) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("domain", enumProperty(domainIds));
        properties.put("subtype", enumProperty(subCategoryIds));
        properties.put("primary_intent", enumProperty(INTENTS));
        properties.put("secondary_intents", enumArrayProperty(INTENTS));
        properties.put("emotion_state", enumArrayProperty(EMOTION_STATES));
        properties.put("target", enumProperty(TARGETS));
        properties.put("urgency", enumProperty(URGENCY_LEVELS));
        properties.put("safety_flag", enumProperty(SAFETY_FLAGS));
        properties.put("needs_clarification", Map.of("type", "boolean"));
        properties.put("confidence", Map.of("type", "number", "minimum", 0.0, "maximum", 1.0));

        return Map.of(
            "type", "json_schema",
            "name", "question_analysis",
            "strict", true,
            "schema", Map.of(
                "type", "object",
                "properties", properties,
                "required", List.of(
                    "domain",
                    "subtype",
                    "primary_intent",
                    "secondary_intents",
                    "emotion_state",
                    "target",
                    "urgency",
                    "safety_flag",
                    "needs_clarification",
                    "confidence"
                ),
                "additionalProperties", false
            )
        );
    }

    private static Map<String, Object> enumProperty(Set<String> values) {
        return Map.of(
            "type", "string",
            "enum", values.stream().sorted().toList()
        );
    }

    private static Map<String, Object> enumArrayProperty(Set<String> values) {
        return Map.of(
            "type", "array",
            "items", enumProperty(values)
        );
    }
}
