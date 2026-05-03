package com.example.springservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.springservice.dto.CategorySelectionRequest;
import com.example.springservice.dto.IntakeAnswerRequest;
import com.example.springservice.dto.IntakeContextRequest;
import com.example.springservice.dto.TarotRequest;
import com.example.springservice.dto.UiContextRequest;
import com.example.springservice.exception.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TarotRequestValidatorTest {

    private TarotRequestValidator validator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        QuestionCategoryCatalog questionCategoryCatalog = new QuestionCategoryCatalog(objectMapper);
        validator = new TarotRequestValidator(
            objectMapper,
            questionCategoryCatalog,
            new IntakeContextValidator(objectMapper, questionCategoryCatalog)
        );
    }

    @Test
    void validatesSupportedRequest() {
        TarotRequestValidator.ValidatedTarotRequest validated = validator.validate(
            new TarotRequest(
                "오늘 하루 흐름이 궁금해요.",
                "one-card",
                "[{\"id\":\"major_01\",\"direction\":\"정방향\"}]",
                new CategorySelectionRequest("general", "today"),
                new UiContextRequest("ko", "category-v3"),
                null
            )
        );

        assertEquals("원카드", validated.gradioReadingType());
        assertEquals("general", validated.categorySelection().mainCategoryId());
        assertEquals("category-v3", validated.uiContext().categoryVersion());
        assertTrue(validated.intakeContextJson().isBlank());
    }

    @Test
    void rejectsInvalidCategorySelection() {
        ApiException exception = assertThrows(ApiException.class, () -> validator.validate(
            new TarotRequest(
                "오늘 하루 흐름이 궁금해요.",
                "one-card",
                "[{\"id\":\"major_01\",\"direction\":\"정방향\"}]",
                new CategorySelectionRequest("general", "job_search"),
                new UiContextRequest("ko", "category-v3"),
                null
            )
        ));

        assertEquals("INVALID_CATEGORY_SELECTION", exception.getCode());
    }

    @Test
    void validatesIntakeContextForConfiguredFollowUpFlow() {
        TarotRequestValidator.ValidatedTarotRequest validated = validator.validate(
            new TarotRequest(
                "연애 흐름이 궁금해요.",
                "one-card",
                "[{\"id\":\"major_01\",\"direction\":\"정방향\"}]",
                new CategorySelectionRequest("love", "some"),
                new UiContextRequest("ko", "category-v3"),
                new IntakeContextRequest(
                    "love-some-v1",
                    "followup-v1",
                    List.of(
                        new IntakeAnswerRequest("some_duration", "single_select", "under_one_month", null),
                        new IntakeAnswerRequest("contact_frequency", "single_select", "few_times_week", null)
                    ),
                    null
                )
            )
        );

        assertTrue(validated.intakeContextJson().contains("\"flowId\":\"love-some-v1\""));
        assertTrue(validated.intakeContextJson().contains("\"flowVersion\":\"followup-v1\""));
        assertTrue(validated.intakeContextJson().contains("\"label\":\"1개월 이내\""));
        assertTrue(validated.intakeContextJson().contains("\"label\":\"주 몇 번\""));
    }

    @Test
    void rejectsIntakeContextWhenRequiredAnswerIsMissing() {
        ApiException exception = assertThrows(ApiException.class, () -> validator.validate(
            new TarotRequest(
                "재회 가능성이 궁금해요.",
                "one-card",
                "[{\"id\":\"major_01\",\"direction\":\"정방향\"}]",
                new CategorySelectionRequest("love", "reunion"),
                new UiContextRequest("ko", "category-v3"),
                new IntakeContextRequest(
                    "love-reunion-v1",
                    "followup-v1",
                    List.of(
                        new IntakeAnswerRequest("breakup_duration", "single_select", "under_one_month", null)
                    ),
                    null
                )
            )
        ));

        assertEquals("INVALID_INTAKE_CONTEXT", exception.getCode());
    }
}
