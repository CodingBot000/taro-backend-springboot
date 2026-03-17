package com.example.springservice.service;

import java.io.IOException;

public interface QuestionAnalysisProvider {

    String name();

    String analyze(TarotRequestValidator.ValidatedTarotRequest request) throws IOException, InterruptedException;
}
