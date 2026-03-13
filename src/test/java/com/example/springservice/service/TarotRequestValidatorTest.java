package com.example.springservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.springservice.dto.CategorySelectionRequest;
import com.example.springservice.dto.TarotRequest;
import com.example.springservice.dto.UiContextRequest;
import com.example.springservice.exception.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TarotRequestValidatorTest {

    private TarotRequestValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TarotRequestValidator(new ObjectMapper());
    }

    @Test
    void validatesSupportedRequest() {
        TarotRequestValidator.ValidatedTarotRequest validated = validator.validate(
            new TarotRequest(
                "오늘 하루 흐름이 궁금해요.",
                "one-card",
                "[{\"id\":\"major_01\",\"direction\":\"정방향\"}]",
                new CategorySelectionRequest("general", "today"),
                new UiContextRequest("ko", "category-v1")
            )
        );

        assertEquals("원카드", validated.gradioReadingType());
    }

    @Test
    void rejectsInvalidCategorySelection() {
        ApiException exception = assertThrows(ApiException.class, () -> validator.validate(
            new TarotRequest(
                "오늘 하루 흐름이 궁금해요.",
                "one-card",
                "[{\"id\":\"major_01\",\"direction\":\"정방향\"}]",
                new CategorySelectionRequest("general", "job_search"),
                new UiContextRequest("ko", "category-v1")
            )
        ));

        assertEquals("INVALID_CATEGORY_SELECTION", exception.getCode());
    }
}
