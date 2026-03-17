package com.example.springservice.service;

import com.example.springservice.config.AppProperties;
import java.io.IOException;
import java.net.http.HttpTimeoutException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ExternalLlmQuestionAnalysisService implements QuestionAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(ExternalLlmQuestionAnalysisService.class);

    private final AppProperties appProperties;
    private final QuestionAnalysisValidator questionAnalysisValidator;
    private final QuestionAnalysisFallbackFactory questionAnalysisFallbackFactory;
    private final QuestionAnalysisPostProcessor questionAnalysisPostProcessor;
    private final Map<String, QuestionAnalysisProvider> providersByName;

    public ExternalLlmQuestionAnalysisService(
        AppProperties appProperties,
        QuestionAnalysisValidator questionAnalysisValidator,
        QuestionAnalysisFallbackFactory questionAnalysisFallbackFactory,
        QuestionAnalysisPostProcessor questionAnalysisPostProcessor,
        List<QuestionAnalysisProvider> providers
    ) {
        this.appProperties = appProperties;
        this.questionAnalysisValidator = questionAnalysisValidator;
        this.questionAnalysisFallbackFactory = questionAnalysisFallbackFactory;
        this.questionAnalysisPostProcessor = questionAnalysisPostProcessor;
        this.providersByName = providers.stream()
            .collect(java.util.stream.Collectors.toUnmodifiableMap(
                provider -> provider.name().toLowerCase(Locale.ROOT),
                Function.identity()
            ));
    }

    @Override
    public QuestionAnalysisResult analyze(TarotRequestValidator.ValidatedTarotRequest request) {
        QuestionAnalysisResult fallback = questionAnalysisFallbackFactory.create(request);
        AppProperties.QuestionAnalysis questionAnalysis = appProperties.getQuestionAnalysis();

        if (!questionAnalysis.isEnabled()) {
            return fallback;
        }

        String configuredProvider = normalizeProvider(questionAnalysis.getProvider());
        if (configuredProvider.isBlank()) {
            log.warn("Question analysis provider is blank. Falling back to UI-derived defaults.");
            return fallback;
        }

        QuestionAnalysisProvider provider = providersByName.get(configuredProvider);
        if (provider == null) {
            log.warn("Unsupported question analysis provider: {}. Falling back to UI-derived defaults.", configuredProvider);
            return fallback;
        }

        try {
            String rawResult = provider.analyze(request);
            QuestionAnalysisResult result = questionAnalysisValidator.validate(rawResult, provider.name());
            QuestionAnalysisResult postProcessed = questionAnalysisPostProcessor.postProcess(result, request);
            if (!postProcessed.equals(result)) {
                log.info(
                    "Question analysis post-processed. source={}, domain={}, subtype={}, primaryIntent={}, secondaryIntents={}, confidence={}, needsClarification={}",
                    postProcessed.analysisSource(),
                    postProcessed.domain(),
                    postProcessed.subtype(),
                    postProcessed.primaryIntent(),
                    postProcessed.secondaryIntents(),
                    postProcessed.confidence(),
                    postProcessed.needsClarification()
                );
            }
            log.info(
                "Question analysis completed. source={}, domain={}, subtype={}, primaryIntent={}, secondaryIntents={}, confidence={}, needsClarification={}",
                postProcessed.analysisSource(),
                postProcessed.domain(),
                postProcessed.subtype(),
                postProcessed.primaryIntent(),
                postProcessed.secondaryIntents(),
                postProcessed.confidence(),
                postProcessed.needsClarification()
            );
            return postProcessed;
        } catch (HttpTimeoutException exception) {
            log.warn("Question analysis timed out. Falling back to UI-derived defaults.");
            return fallback;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Question analysis interrupted. Falling back to UI-derived defaults.");
            return fallback;
        } catch (IOException | RuntimeException exception) {
            log.warn("Question analysis failed. Falling back to UI-derived defaults.", exception);
            return fallback;
        }
    }

    private String normalizeProvider(String provider) {
        if (provider == null) {
            return "";
        }
        return provider.trim().toLowerCase(Locale.ROOT);
    }
}
