package com.example.springservice.service;

import com.example.springservice.dto.CategorySelectionRequest;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class QuestionAnalysisFallbackFactory {

    public QuestionAnalysisResult create(TarotRequestValidator.ValidatedTarotRequest request) {
        return create(request.categorySelection());
    }

    public QuestionAnalysisResult create(CategorySelectionRequest categorySelection) {
        return new QuestionAnalysisResult(
            QuestionAnalysisSchema.fallbackDomain(categorySelection),
            QuestionAnalysisSchema.fallbackSubtype(categorySelection),
            "overall_guidance",
            List.of(),
            List.of(),
            "unknown",
            "medium",
            "none",
            false,
            0.2,
            QuestionAnalysisSchema.ANALYSIS_SOURCE_FALLBACK,
            QuestionAnalysisSchema.ANALYSIS_VERSION
        );
    }
}
