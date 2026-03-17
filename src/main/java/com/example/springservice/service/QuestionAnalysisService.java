package com.example.springservice.service;

public interface QuestionAnalysisService {

    QuestionAnalysisResult analyze(TarotRequestValidator.ValidatedTarotRequest request);
}
