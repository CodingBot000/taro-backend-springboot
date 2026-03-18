package com.example.springservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QuestionAnalysisValidatorTest {

    private QuestionAnalysisValidator validator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        validator = new QuestionAnalysisValidator(objectMapper, new QuestionCategoryCatalog(objectMapper));
    }

    @Test
    void validatesStructuredPayloadAndDeduplicatesArrays() {
        QuestionAnalysisResult result = validator.validate(
            """
                {
                  "domain": "love",
                  "subtype": "reunion",
                  "primary_intent": "cause",
                  "secondary_intents": ["possibility", "possibility", "timing"],
                  "emotion_state": ["anxious", "anxious"],
                  "target": "ex_partner",
                  "urgency": "medium",
                  "safety_flag": "none",
                  "needs_clarification": false,
                  "confidence": 0.84
                }
                """,
            "openai"
        );

        assertEquals("love", result.domain());
        assertEquals("cause", result.primaryIntent());
        assertEquals(List.of("possibility", "timing"), result.secondaryIntents());
        assertEquals(List.of("anxious"), result.emotionState());
        assertEquals("openai", result.analysisSource());
        assertEquals("v1", result.analysisVersion());
    }

    @Test
    void rejectsUnsupportedEnumValues() {
        assertThrows(IllegalArgumentException.class, () -> validator.validate(
            """
                {
                  "domain": "love",
                  "subtype": "reunion",
                  "primary_intent": "not_allowed",
                  "secondary_intents": [],
                  "emotion_state": [],
                  "target": "ex_partner",
                  "urgency": "medium",
                  "safety_flag": "none",
                  "needs_clarification": false,
                  "confidence": 0.84
                }
                """,
            "openai"
        ));
    }

    @Test
    void rejectsConfidenceOutsideAllowedRange() {
        assertThrows(IllegalArgumentException.class, () -> validator.validate(
            """
                {
                  "domain": "study",
                  "subtype": "exam_result",
                  "primary_intent": "timing",
                  "secondary_intents": [],
                  "emotion_state": [],
                  "target": "self",
                  "urgency": "medium",
                  "safety_flag": "none",
                  "needs_clarification": false,
                  "confidence": 1.2
                }
                """,
            "openai"
        ));
    }

    @Test
    void rejectsMissingConfidence() {
        assertThrows(IllegalArgumentException.class, () -> validator.validate(
            """
                {
                  "domain": "general",
                  "subtype": "unknown",
                  "primary_intent": "overall_guidance",
                  "secondary_intents": [],
                  "emotion_state": [],
                  "target": "unknown",
                  "urgency": "medium",
                  "safety_flag": "none",
                  "needs_clarification": true
                }
                """,
            "openai"
        ));
    }
}
