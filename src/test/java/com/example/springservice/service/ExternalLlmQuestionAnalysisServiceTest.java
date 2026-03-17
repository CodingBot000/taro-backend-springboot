package com.example.springservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.springservice.config.AppProperties;
import com.example.springservice.dto.CategorySelectionRequest;
import com.example.springservice.dto.UiContextRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpTimeoutException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ExternalLlmQuestionAnalysisServiceTest {

    private final QuestionAnalysisValidator validator = new QuestionAnalysisValidator(new ObjectMapper());
    private final QuestionAnalysisFallbackFactory fallbackFactory = new QuestionAnalysisFallbackFactory();
    private final QuestionAnalysisPostProcessor postProcessor = new QuestionAnalysisPostProcessor();

    @Test
    void acceptsFixtureCasesThroughValidationPipeline() {
        Map<String, String> fixtureResponses = Map.ofEntries(
            Map.entry(
                "전남친이 왜 그렇게 차갑게 끝냈는지, 다시 연락할 가능성이 있는지 궁금해요.",
                """
                    {
                      "domain": "love",
                      "subtype": "reunion",
                      "primary_intent": "cause",
                      "secondary_intents": ["possibility"],
                      "emotion_state": ["anxious"],
                      "target": "ex_partner",
                      "urgency": "medium",
                      "safety_flag": "none",
                      "needs_clarification": false,
                      "confidence": 0.84
                    }
                    """
            ),
            Map.entry(
                "이 회사에 계속 남아야 할지, 이직해야 할지 고민돼요.",
                """
                    {
                      "domain": "career",
                      "subtype": "job_change",
                      "primary_intent": "comparison",
                      "secondary_intents": [],
                      "emotion_state": ["confused"],
                      "target": "self",
                      "urgency": "medium",
                      "safety_flag": "none",
                      "needs_clarification": false,
                      "confidence": 0.79
                    }
                    """
            ),
            Map.entry(
                "투자를 시작해도 될까요, 아니면 지금은 보류해야 할까요?",
                """
                    {
                      "domain": "finance",
                      "subtype": "investment",
                      "primary_intent": "comparison",
                      "secondary_intents": ["advice"],
                      "emotion_state": [],
                      "target": "self",
                      "urgency": "medium",
                      "safety_flag": "none",
                      "needs_clarification": false,
                      "confidence": 0.77
                    }
                    """
            ),
            Map.entry(
                "친구와 멀어진 이유가 뭘까요? 다시 가까워질 수 있을까요?",
                """
                    {
                      "domain": "relationship",
                      "subtype": "distance_conflict",
                      "primary_intent": "cause",
                      "secondary_intents": ["possibility"],
                      "emotion_state": ["sad"],
                      "target": "friend",
                      "urgency": "medium",
                      "safety_flag": "none",
                      "needs_clarification": false,
                      "confidence": 0.81
                    }
                    """
            ),
            Map.entry(
                "시험 결과가 언제쯤 윤곽이 보일까요?",
                """
                    {
                      "domain": "study",
                      "subtype": "exam_result",
                      "primary_intent": "timing",
                      "secondary_intents": [],
                      "emotion_state": ["anxious"],
                      "target": "self",
                      "urgency": "medium",
                      "safety_flag": "none",
                      "needs_clarification": false,
                      "confidence": 0.75
                    }
                    """
            ),
            Map.entry(
                "어떻게 될까요?",
                """
                    {
                      "domain": "general",
                      "subtype": "unknown",
                      "primary_intent": "overall_guidance",
                      "secondary_intents": [],
                      "emotion_state": [],
                      "target": "unknown",
                      "urgency": "medium",
                      "safety_flag": "none",
                      "needs_clarification": true,
                      "confidence": 0.21
                    }
                    """
            )
        );

        ExternalLlmQuestionAnalysisService service = newService(true, "openai", new FixtureProvider(fixtureResponses));

        assertAnalysis(
            service.analyze(validatedRequest(
                "전남친이 왜 그렇게 차갑게 끝냈는지, 다시 연락할 가능성이 있는지 궁금해요.",
                "love",
                "reunion"
            )),
            "love",
            "reunion",
            "cause"
        );

        assertAnalysis(
            service.analyze(validatedRequest(
                "이 회사에 계속 남아야 할지, 이직해야 할지 고민돼요.",
                "career",
                "job_change"
            )),
            "career",
            "job_change",
            "comparison"
        );

        assertAnalysis(
            service.analyze(validatedRequest(
                "투자를 시작해도 될까요, 아니면 지금은 보류해야 할까요?",
                "finance",
                "investment"
            )),
            "finance",
            "investment",
            "comparison"
        );

        assertAnalysis(
            service.analyze(validatedRequest(
                "친구와 멀어진 이유가 뭘까요? 다시 가까워질 수 있을까요?",
                "relationship",
                "distance_conflict"
            )),
            "relationship",
            "distance_conflict",
            "cause"
        );

        assertAnalysis(
            service.analyze(validatedRequest(
                "시험 결과가 언제쯤 윤곽이 보일까요?",
                "study",
                "exam_result"
            )),
            "study",
            "exam_result",
            "timing"
        );

        QuestionAnalysisResult ambiguous = service.analyze(validatedRequest(
            "어떻게 될까요?",
            "general",
            "overall_flow"
        ));
        assertEquals("general", ambiguous.domain());
        assertEquals("unknown", ambiguous.subtype());
        assertEquals("overall_guidance", ambiguous.primaryIntent());
        assertEquals(true, ambiguous.needsClarification());
    }

    @Test
    void keepsValidExternalResultEvenWhenItConflictsWithUiSelection() {
        ExternalLlmQuestionAnalysisService service = newService(
            true,
            "openai",
            new FixtureProvider(Map.of(
                "질문",
                """
                    {
                      "domain": "love",
                      "subtype": "reunion",
                      "primary_intent": "cause",
                      "secondary_intents": ["possibility"],
                      "emotion_state": ["anxious"],
                      "target": "ex_partner",
                      "urgency": "medium",
                      "safety_flag": "none",
                      "needs_clarification": false,
                      "confidence": 0.84
                    }
                    """
            ))
        );

        QuestionAnalysisResult result = service.analyze(validatedRequest("질문", "general", "today"));

        assertEquals("love", result.domain());
        assertEquals("reunion", result.subtype());
        assertEquals("openai", result.analysisSource());
    }

    @Test
    void fallsBackWhenFeatureIsDisabled() {
        ExternalLlmQuestionAnalysisService service = newService(
            false,
            "openai",
            new FixtureProvider(Map.of())
        );

        QuestionAnalysisResult result = service.analyze(validatedRequest("질문", "love", "reunion"));

        assertEquals("love", result.domain());
        assertEquals("reunion", result.subtype());
        assertEquals("fallback", result.analysisSource());
    }

    @Test
    void fallsBackWhenProviderIsUnsupported() {
        ExternalLlmQuestionAnalysisService service = newService(
            true,
            "anthropic",
            new FixtureProvider(Map.of())
        );

        QuestionAnalysisResult result = service.analyze(validatedRequest("질문", "finance", "investment"));

        assertEquals("finance", result.domain());
        assertEquals("investment", result.subtype());
        assertEquals("fallback", result.analysisSource());
    }

    @Test
    void fallsBackWhenProviderTimesOut() {
        ExternalLlmQuestionAnalysisService service = newService(
            true,
            "openai",
            new ThrowingProvider(new HttpTimeoutException("timeout"))
        );

        QuestionAnalysisResult result = service.analyze(validatedRequest("질문", "study", "exam_result"));

        assertEquals("study", result.domain());
        assertEquals("exam_result", result.subtype());
        assertEquals("fallback", result.analysisSource());
    }

    @Test
    void fallsBackWhenProviderReturnsInvalidPayload() {
        ExternalLlmQuestionAnalysisService service = newService(
            true,
            "openai",
            new FixtureProvider(Map.of(
                "질문",
                """
                    {
                      "domain": "love",
                      "subtype": "reunion",
                      "primary_intent": "cause",
                      "secondary_intents": [],
                      "emotion_state": [],
                      "target": "ex_partner",
                      "urgency": "medium",
                      "safety_flag": "none",
                      "needs_clarification": false
                    }
                    """
            ))
        );

        QuestionAnalysisResult result = service.analyze(validatedRequest("질문", "relationship", "friends"));

        assertEquals("relationship", result.domain());
        assertEquals("friends", result.subtype());
        assertEquals("fallback", result.analysisSource());
    }

    @Test
    void fallsBackWhenProviderThrowsRuntimeException() {
        ExternalLlmQuestionAnalysisService service = newService(
            true,
            "openai",
            new ThrowingProvider(new IllegalStateException("missing config"))
        );

        QuestionAnalysisResult result = service.analyze(validatedRequest("질문", "general", "today"));

        assertEquals("general", result.domain());
        assertEquals("today", result.subtype());
        assertEquals("fallback", result.analysisSource());
    }

    @Test
    void normalizesInvestmentSubtypeWhenQuestionIsExplicitButModelReturnsUnknown() {
        ExternalLlmQuestionAnalysisService service = newService(
            true,
            "openai",
            new FixtureProvider(Map.of(
                "투자를 시작해도 될까요, 아니면 지금은 보류해야 할까요?",
                """
                    {
                      "domain": "finance",
                      "subtype": "unknown",
                      "primary_intent": "comparison",
                      "secondary_intents": ["timing"],
                      "emotion_state": [],
                      "target": "self",
                      "urgency": "medium",
                      "safety_flag": "none",
                      "needs_clarification": true,
                      "confidence": 0.5
                    }
                    """
            ))
        );

        QuestionAnalysisResult result = service.analyze(validatedRequest(
            "투자를 시작해도 될까요, 아니면 지금은 보류해야 할까요?",
            "finance",
            "investment"
        ));

        assertEquals("finance", result.domain());
        assertEquals("investment", result.subtype());
        assertEquals("comparison", result.primaryIntent());
        assertEquals(false, result.needsClarification());
        assertEquals("openai", result.analysisSource());
    }

    @Test
    void normalizesExamResultSubtypeWhenQuestionIsExplicitButModelReturnsUnknown() {
        ExternalLlmQuestionAnalysisService service = newService(
            true,
            "openai",
            new FixtureProvider(Map.of(
                "시험 결과가 언제쯤 윤곽이 보일까요?",
                """
                    {
                      "domain": "study",
                      "subtype": "unknown",
                      "primary_intent": "timing",
                      "secondary_intents": [],
                      "emotion_state": [],
                      "target": "self",
                      "urgency": "medium",
                      "safety_flag": "none",
                      "needs_clarification": true,
                      "confidence": 0.5
                    }
                    """
            ))
        );

        QuestionAnalysisResult result = service.analyze(validatedRequest(
            "시험 결과가 언제쯤 윤곽이 보일까요?",
            "study",
            "exam_result"
        ));

        assertEquals("study", result.domain());
        assertEquals("exam_result", result.subtype());
        assertEquals("timing", result.primaryIntent());
        assertEquals(false, result.needsClarification());
        assertEquals("openai", result.analysisSource());
    }

    @Test
    void normalizesWeekMonthSubtypeWhenQuestionIsExplicitButModelReturnsUnknown() {
        ExternalLlmQuestionAnalysisService service = newService(
            true,
            "openai",
            new FixtureProvider(Map.of(
                "이번 달 전체 흐름이 어떻게 갈지 궁금해요.",
                """
                    {
                      "domain": "general",
                      "subtype": "unknown",
                      "primary_intent": "overall_guidance",
                      "secondary_intents": [],
                      "emotion_state": [],
                      "target": "unknown",
                      "urgency": "medium",
                      "safety_flag": "none",
                      "needs_clarification": true,
                      "confidence": 0.5
                    }
                    """
            ))
        );

        QuestionAnalysisResult result = service.analyze(validatedRequest(
            "이번 달 전체 흐름이 어떻게 갈지 궁금해요.",
            "general",
            "week_month"
        ));

        assertEquals("general", result.domain());
        assertEquals("week_month", result.subtype());
        assertEquals("overall_guidance", result.primaryIntent());
        assertEquals(false, result.needsClarification());
        assertEquals("openai", result.analysisSource());
    }

    @Test
    void normalizesVeryGenericQuestionBackToGeneralUnknown() {
        ExternalLlmQuestionAnalysisService service = newService(
            true,
            "openai",
            new FixtureProvider(Map.of(
                "어떻게 될까요?",
                """
                    {
                      "domain": "study",
                      "subtype": "exam_result",
                      "primary_intent": "future_flow",
                      "secondary_intents": [],
                      "emotion_state": [],
                      "target": "self",
                      "urgency": "medium",
                      "safety_flag": "none",
                      "needs_clarification": false,
                      "confidence": 0.8
                    }
                    """
            ))
        );

        QuestionAnalysisResult result = service.analyze(validatedRequest(
            "어떻게 될까요?",
            "study",
            "exam_result"
        ));

        assertEquals("general", result.domain());
        assertEquals("unknown", result.subtype());
        assertEquals("overall_guidance", result.primaryIntent());
        assertEquals(true, result.needsClarification());
        assertEquals("openai", result.analysisSource());
    }

    private ExternalLlmQuestionAnalysisService newService(
        boolean enabled,
        String provider,
        QuestionAnalysisProvider questionAnalysisProvider
    ) {
        AppProperties appProperties = new AppProperties();
        appProperties.getQuestionAnalysis().setEnabled(enabled);
        appProperties.getQuestionAnalysis().setProvider(provider);

        return new ExternalLlmQuestionAnalysisService(
            appProperties,
            validator,
            fallbackFactory,
            postProcessor,
            java.util.List.of(questionAnalysisProvider)
        );
    }

    private TarotRequestValidator.ValidatedTarotRequest validatedRequest(
        String question,
        String mainCategoryId,
        String subCategoryId
    ) {
        CategorySelectionRequest categorySelection = new CategorySelectionRequest(mainCategoryId, subCategoryId);
        UiContextRequest uiContext = new UiContextRequest("ko", "category-v1");

        return new TarotRequestValidator.ValidatedTarotRequest(
            question,
            "원카드",
            "[{\"id\":\"major_01\",\"direction\":\"정방향\"}]",
            "{\"mainCategoryId\":\"" + mainCategoryId + "\",\"subCategoryId\":\"" + subCategoryId + "\"}",
            "{\"locale\":\"ko\",\"categoryVersion\":\"category-v1\"}",
            categorySelection,
            uiContext
        );
    }

    private void assertAnalysis(QuestionAnalysisResult result, String domain, String subtype, String primaryIntent) {
        assertEquals(domain, result.domain());
        assertEquals(subtype, result.subtype());
        assertEquals(primaryIntent, result.primaryIntent());
        assertEquals("openai", result.analysisSource());
    }

    private static final class FixtureProvider implements QuestionAnalysisProvider {

        private final Map<String, String> responsesByQuestion;

        private FixtureProvider(Map<String, String> responsesByQuestion) {
            this.responsesByQuestion = responsesByQuestion;
        }

        @Override
        public String name() {
            return "openai";
        }

        @Override
        public String analyze(TarotRequestValidator.ValidatedTarotRequest request) {
            return responsesByQuestion.getOrDefault(request.question(), "");
        }
    }

    private static final class ThrowingProvider implements QuestionAnalysisProvider {

        private final Exception exception;

        private ThrowingProvider(Exception exception) {
            this.exception = exception;
        }

        @Override
        public String name() {
            return "openai";
        }

        @Override
        public String analyze(TarotRequestValidator.ValidatedTarotRequest request) throws HttpTimeoutException {
            if (exception instanceof HttpTimeoutException timeoutException) {
                throw timeoutException;
            }
            if (exception instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException(exception);
        }
    }
}
