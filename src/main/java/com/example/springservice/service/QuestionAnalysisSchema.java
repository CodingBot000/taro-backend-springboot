package com.example.springservice.service;

import com.example.springservice.dto.CategorySelectionRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class QuestionAnalysisSchema {

    static final String ANALYSIS_SOURCE_OPENAI = "openai";
    static final String ANALYSIS_SOURCE_FALLBACK = "fallback";
    static final String ANALYSIS_VERSION = "v1";

    static final Set<String> DOMAINS = Set.of(
        "love",
        "career",
        "finance",
        "relationship",
        "study",
        "general"
    );

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

    static final Set<String> SUBTYPES = Set.of(
        "some",
        "reunion",
        "blind_date",
        "crush_confession",
        "relationship_conflict",
        "after_breakup",
        "job_search",
        "job_change",
        "work_relationship",
        "promotion",
        "startup_sidejob",
        "investment",
        "debt_loan",
        "spending",
        "saving_asset",
        "income_salary",
        "friends",
        "family",
        "coworker_boss",
        "distance_conflict",
        "reconciliation",
        "exam_result",
        "focus",
        "major_admission",
        "study_abroad_move",
        "career_path",
        "today",
        "week_month",
        "important_choice",
        "mental_state",
        "overall_flow",
        "unknown"
    );

    private QuestionAnalysisSchema() {
    }

    static String fallbackDomain(CategorySelectionRequest categorySelection) {
        if (categorySelection == null || categorySelection.mainCategoryId() == null) {
            return "general";
        }

        String mainCategoryId = categorySelection.mainCategoryId().trim();
        return DOMAINS.contains(mainCategoryId) ? mainCategoryId : "general";
    }

    static String fallbackSubtype(CategorySelectionRequest categorySelection) {
        if (categorySelection == null || categorySelection.subCategoryId() == null) {
            return "unknown";
        }

        String subCategoryId = categorySelection.subCategoryId().trim();
        return SUBTYPES.contains(subCategoryId) ? subCategoryId : "unknown";
    }

    static Map<String, Object> openAiResponseFormat() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("domain", enumProperty(DOMAINS));
        properties.put("subtype", enumProperty(SUBTYPES));
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
