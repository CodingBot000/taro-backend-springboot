package com.example.springservice.service;

import com.example.springservice.dto.CategorySelectionRequest;
import com.example.springservice.dto.TarotRequest;
import com.example.springservice.dto.UiContextRequest;
import com.example.springservice.exception.ApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class TarotRequestValidator {

    private static final Map<String, String> READING_TYPE_MAP = Map.of(
        "one-card", "원카드",
        "three-card", "쓰리카드"
    );

    private static final Map<String, Set<String>> CATEGORY_SELECTION_TREE = Map.of(
        "love", Set.of("some", "reunion", "blind_date", "crush_confession", "relationship_conflict", "after_breakup", "unknown"),
        "career", Set.of("job_search", "job_change", "work_relationship", "promotion", "startup_sidejob", "unknown"),
        "finance", Set.of("investment", "debt_loan", "spending", "saving_asset", "income_salary", "unknown"),
        "relationship", Set.of("friends", "family", "coworker_boss", "distance_conflict", "reconciliation", "unknown"),
        "study", Set.of("exam_result", "focus", "major_admission", "study_abroad_move", "career_path", "unknown"),
        "general", Set.of("today", "week_month", "important_choice", "mental_state", "overall_flow", "unknown")
    );

    private final ObjectMapper objectMapper;

    public TarotRequestValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
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
            uiContextJson
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

        Set<String> subCategories = CATEGORY_SELECTION_TREE.get(categorySelection.mainCategoryId());
        if (subCategories == null || !subCategories.contains(categorySelection.subCategoryId())) {
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
        String uiContextJson
    ) {
    }
}
