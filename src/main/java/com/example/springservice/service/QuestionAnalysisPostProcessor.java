package com.example.springservice.service;

import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class QuestionAnalysisPostProcessor {

    public QuestionAnalysisResult postProcess(
        QuestionAnalysisResult result,
        TarotRequestValidator.ValidatedTarotRequest request
    ) {
        if (result == null || request == null || request.question() == null) {
            return result;
        }

        if (shouldNormalizeAmbiguousGenericQuestion(request.question())) {
            return new QuestionAnalysisResult(
                "general",
                "unknown",
                "overall_guidance",
                java.util.List.of(),
                java.util.List.of(),
                "unknown",
                result.urgency(),
                result.safetyFlag(),
                true,
                Math.min(result.confidence(), 0.5),
                result.analysisSource(),
                result.analysisVersion()
            );
        }

        if (shouldNormalizeInvestmentSubtype(result, request.question())) {
            return withSubtype(result, "investment");
        }

        if (shouldNormalizeExamResultSubtype(result, request.question())) {
            return withSubtype(result, "exam_result");
        }

        if (shouldNormalizeWeekMonthSubtype(result, request.question())) {
            return withSubtype(result, "week_month");
        }

        return result;
    }

    private boolean shouldNormalizeAmbiguousGenericQuestion(String question) {
        String normalizedQuestion = normalizeQuestion(question);
        return normalizedQuestion.equals("어떻게될까요?")
            || normalizedQuestion.equals("어떻게될까요")
            || normalizedQuestion.equals("어떻게될까")
            || normalizedQuestion.equals("어떻게되나요")
            || normalizedQuestion.equals("잘될까요");
    }

    private boolean shouldNormalizeInvestmentSubtype(QuestionAnalysisResult result, String question) {
        if (!"finance".equals(result.domain()) || !"unknown".equals(result.subtype())) {
            return false;
        }

        String normalizedQuestion = normalizeQuestion(question);
        return normalizedQuestion.contains("투자")
            || normalizedQuestion.contains("주식")
            || normalizedQuestion.contains("코인")
            || normalizedQuestion.contains("가상화폐")
            || normalizedQuestion.contains("암호화폐");
    }

    private boolean shouldNormalizeExamResultSubtype(QuestionAnalysisResult result, String question) {
        if (!"study".equals(result.domain()) || !"unknown".equals(result.subtype())) {
            return false;
        }

        String normalizedQuestion = normalizeQuestion(question);
        return normalizedQuestion.contains("시험")
            || normalizedQuestion.contains("결과")
            || normalizedQuestion.contains("합격")
            || normalizedQuestion.contains("성적");
    }

    private boolean shouldNormalizeWeekMonthSubtype(QuestionAnalysisResult result, String question) {
        if (!"general".equals(result.domain()) || !"unknown".equals(result.subtype())) {
            return false;
        }

        String normalizedQuestion = normalizeQuestion(question);
        return normalizedQuestion.contains("이번달")
            || normalizedQuestion.contains("이번달전체흐름")
            || normalizedQuestion.contains("이번주")
            || normalizedQuestion.contains("한달")
            || normalizedQuestion.contains("전체흐름");
    }

    private QuestionAnalysisResult withSubtype(QuestionAnalysisResult result, String subtype) {
        return new QuestionAnalysisResult(
            result.domain(),
            subtype,
            result.primaryIntent(),
            result.secondaryIntents(),
            result.emotionState(),
            result.target(),
            result.urgency(),
            result.safetyFlag(),
            false,
            Math.max(result.confidence(), 0.7),
            result.analysisSource(),
            result.analysisVersion()
        );
    }

    private String normalizeQuestion(String question) {
        return question.toLowerCase(Locale.ROOT).replace(" ", "");
    }
}
