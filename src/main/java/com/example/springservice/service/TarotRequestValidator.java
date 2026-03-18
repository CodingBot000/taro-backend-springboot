package com.example.springservice.service;

import com.example.springservice.dto.CategorySelectionRequest;
import com.example.springservice.dto.TarotRequest;
import com.example.springservice.dto.UiContextRequest;
import com.example.springservice.exception.ApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class TarotRequestValidator {

    private static final Map<String, String> READING_TYPE_MAP = Map.of(
        "one-card", "원카드",
        "three-card", "쓰리카드"
    );

    private final ObjectMapper objectMapper;
    private final QuestionCategoryCatalog questionCategoryCatalog;

    public TarotRequestValidator(ObjectMapper objectMapper, QuestionCategoryCatalog questionCategoryCatalog) {
        this.objectMapper = objectMapper;
        this.questionCategoryCatalog = questionCategoryCatalog;
    }

    public ValidatedTarotRequest validate(TarotRequest request) {
        if (request == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "요청 본문 형식이 올바르지 않습니다.", "INVALID_REQUEST");
        }

        String question = validateQuestion(request.question());
        String gradioReadingType = validateReadingType(request.readingType());
        String selectedCardsJson = validateSelectedCards(request.selectedCardsJson(), request.readingType());
        String categorySelectionJson = validateCategorySelection(request.categorySelection());
        String uiContextJson = validateUiContext(request.uiContext());

        return new ValidatedTarotRequest(
            question,
            gradioReadingType,
            selectedCardsJson,
            categorySelectionJson,
            uiContextJson,
            request.categorySelection(),
            request.uiContext()
        );
    }

    private String validateQuestion(String question) {
        if (question == null || question.trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "질문을 입력해주세요.", "INVALID_QUESTION");
        }

        String trimmedQuestion = question.trim();
        if (trimmedQuestion.length() > 500) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "질문은 500자 이내로 입력해주세요.", "QUESTION_TOO_LONG");
        }

        return trimmedQuestion;
    }

    private String validateReadingType(String readingType) {
        String mapped = READING_TYPE_MAP.get(readingType);
        if (mapped == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "올바른 리딩 타입을 선택해주세요.", "INVALID_READING_TYPE");
        }
        return mapped;
    }

    private String validateSelectedCards(String selectedCardsJson, String readingType) {
        if (selectedCardsJson == null || selectedCardsJson.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "카드를 선택해주세요.", "INVALID_CARDS");
        }

        JsonNode cardsNode;
        try {
            cardsNode = objectMapper.readTree(selectedCardsJson);
        } catch (JsonProcessingException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "카드 데이터가 올바른 JSON 형식이 아닙니다.", "INVALID_CARDS_JSON");
        }

        if (!cardsNode.isArray()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "카드를 선택해주세요.", "INVALID_CARDS");
        }

        int expectedCardCount = "one-card".equals(readingType) ? 1 : 3;
        if (cardsNode.size() != expectedCardCount) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "리딩 타입과 카드 수가 일치하지 않습니다.", "CARD_COUNT_MISMATCH");
        }

        return selectedCardsJson;
    }

    private String validateCategorySelection(CategorySelectionRequest categorySelection) {
        if (categorySelection == null ||
            categorySelection.mainCategoryId() == null ||
            categorySelection.subCategoryId() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "질문 카테고리 선택이 올바르지 않습니다.", "INVALID_CATEGORY_SELECTION");
        }

        if (!questionCategoryCatalog.isValidSelection(categorySelection.mainCategoryId(), categorySelection.subCategoryId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "질문 카테고리 선택이 올바르지 않습니다.", "INVALID_CATEGORY_SELECTION");
        }

        try {
            return objectMapper.writeValueAsString(categorySelection);
        } catch (JsonProcessingException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "질문 카테고리 선택이 올바르지 않습니다.", "INVALID_CATEGORY_SELECTION");
        }
    }

    private String validateUiContext(UiContextRequest uiContext) {
        if (uiContext == null ||
            uiContext.locale() == null ||
            uiContext.locale().trim().isEmpty() ||
            uiContext.categoryVersion() == null ||
            uiContext.categoryVersion().trim().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "UI 컨텍스트 정보가 올바르지 않습니다.", "INVALID_UI_CONTEXT");
        }

        try {
            return objectMapper.writeValueAsString(uiContext);
        } catch (JsonProcessingException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "UI 컨텍스트 정보가 올바르지 않습니다.", "INVALID_UI_CONTEXT");
        }
    }

    public record ValidatedTarotRequest(
        String question,
        String gradioReadingType,
        String selectedCardsJson,
        String categorySelectionJson,
        String uiContextJson,
        CategorySelectionRequest categorySelection,
        UiContextRequest uiContext
    ) {
    }
}
