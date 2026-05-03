package com.example.springservice.service;

import com.example.springservice.dto.CategorySelectionRequest;
import com.example.springservice.dto.IntakeAnswerRequest;
import com.example.springservice.dto.IntakeContextRequest;
import com.example.springservice.dto.QuestionCategoryManifestResponse.FollowUpFlow;
import com.example.springservice.dto.QuestionCategoryManifestResponse.FollowUpOption;
import com.example.springservice.dto.QuestionCategoryManifestResponse.FollowUpQuestion;
import com.example.springservice.exception.ApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class IntakeContextValidator {

    private final ObjectMapper objectMapper;
    private final QuestionCategoryCatalog questionCategoryCatalog;

    public IntakeContextValidator(ObjectMapper objectMapper, QuestionCategoryCatalog questionCategoryCatalog) {
        this.objectMapper = objectMapper;
        this.questionCategoryCatalog = questionCategoryCatalog;
    }

    public ValidationResult validate(
        IntakeContextRequest intakeContext,
        CategorySelectionRequest categorySelection
    ) {
        if (intakeContext == null) {
            return new ValidationResult("", null);
        }

        FollowUpFlow followUpFlow = questionCategoryCatalog.followUpFlow(categorySelection);
        if (followUpFlow == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "심화 질문 정보가 올바르지 않습니다.", "INVALID_INTAKE_CONTEXT");
        }

        String flowId = requireNonBlank(intakeContext.flowId(), "심화 질문 flow id");
        String flowVersion = requireNonBlank(intakeContext.flowVersion(), "심화 질문 flow version");
        if (!followUpFlow.flowId().equals(flowId) || !followUpFlow.version().equals(flowVersion)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "심화 질문 정보가 올바르지 않습니다.", "INVALID_INTAKE_CONTEXT");
        }

        List<IntakeAnswerRequest> answers = intakeContext.answers();
        if (answers == null) {
            answers = List.of();
        }

        Map<String, FollowUpQuestion> questionsById = new LinkedHashMap<>();
        for (FollowUpQuestion question : followUpFlow.questions()) {
            questionsById.put(question.id(), question);
        }

        LinkedHashSet<String> seenQuestionIds = new LinkedHashSet<>();
        List<IntakeAnswerRequest> sanitizedAnswers = new ArrayList<>();

        for (IntakeAnswerRequest answer : answers) {
            sanitizedAnswers.add(validateAnswer(answer, questionsById, seenQuestionIds));
        }

        for (FollowUpQuestion question : followUpFlow.questions()) {
            if (Boolean.TRUE.equals(question.required()) && !seenQuestionIds.contains(question.id())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "필수 심화 질문 응답이 누락되었습니다.", "INVALID_INTAKE_CONTEXT");
            }
        }

        IntakeContextRequest sanitizedIntakeContext = new IntakeContextRequest(
            followUpFlow.flowId(),
            followUpFlow.version(),
            List.copyOf(sanitizedAnswers),
            validateSynthesizedQuestion(intakeContext.synthesizedQuestion())
        );

        try {
            return new ValidationResult(
                objectMapper.writeValueAsString(sanitizedIntakeContext),
                sanitizedIntakeContext
            );
        } catch (JsonProcessingException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "심화 질문 정보가 올바르지 않습니다.", "INVALID_INTAKE_CONTEXT");
        }
    }

    private IntakeAnswerRequest validateAnswer(
        IntakeAnswerRequest answer,
        Map<String, FollowUpQuestion> questionsById,
        LinkedHashSet<String> seenQuestionIds
    ) {
        if (answer == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "심화 질문 응답 형식이 올바르지 않습니다.", "INVALID_INTAKE_CONTEXT");
        }

        String questionId = requireNonBlank(answer.questionId(), "심화 질문 question id");
        if (!seenQuestionIds.add(questionId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "심화 질문 응답이 중복되었습니다.", "INVALID_INTAKE_CONTEXT");
        }

        FollowUpQuestion followUpQuestion = questionsById.get(questionId);
        if (followUpQuestion == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "심화 질문 응답 형식이 올바르지 않습니다.", "INVALID_INTAKE_CONTEXT");
        }

        String type = requireNonBlank(answer.type(), "심화 질문 answer type");
        if (!followUpQuestion.type().equals(type)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "심화 질문 응답 형식이 올바르지 않습니다.", "INVALID_INTAKE_CONTEXT");
        }

        String normalizedValue = requireNonBlank(answer.value(), "심화 질문 answer value").trim();
        String resolvedLabel = null;

        if ("single_select".equals(type)) {
            resolvedLabel = resolveSingleSelectLabel(followUpQuestion, normalizedValue);
            if (resolvedLabel == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "심화 질문 응답 값이 올바르지 않습니다.", "INVALID_INTAKE_CONTEXT");
            }
        } else if ("free_text_short".equals(type)) {
            int maxLength = followUpQuestion.maxLength() == null ? 120 : followUpQuestion.maxLength();
            if (normalizedValue.length() > maxLength) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "심화 질문 응답이 너무 깁니다.", "INVALID_INTAKE_CONTEXT");
            }
        } else {
            throw new ApiException(HttpStatus.BAD_REQUEST, "심화 질문 응답 형식이 올바르지 않습니다.", "INVALID_INTAKE_CONTEXT");
        }

        return new IntakeAnswerRequest(questionId, type, normalizedValue, resolvedLabel);
    }

    private String validateSynthesizedQuestion(String synthesizedQuestion) {
        if (synthesizedQuestion == null || synthesizedQuestion.isBlank()) {
            return null;
        }

        String normalized = synthesizedQuestion.trim();
        if (normalized.length() > 500) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "심화 질문 기반 보조 문장이 너무 깁니다.", "INVALID_INTAKE_CONTEXT");
        }
        return normalized;
    }

    private String resolveSingleSelectLabel(FollowUpQuestion followUpQuestion, String value) {
        if (followUpQuestion.options() == null) {
            return null;
        }

        for (FollowUpOption option : followUpQuestion.options()) {
            if (option.id().equals(value)) {
                return option.label();
            }
        }

        return null;
    }

    private String requireNonBlank(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, label + " 값이 올바르지 않습니다.", "INVALID_INTAKE_CONTEXT");
        }
        return value.trim();
    }

    public record ValidationResult(
        String intakeContextJson,
        IntakeContextRequest intakeContext
    ) {
    }
}
