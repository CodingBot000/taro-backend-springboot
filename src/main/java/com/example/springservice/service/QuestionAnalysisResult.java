package com.example.springservice.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record QuestionAnalysisResult(
    @JsonProperty("domain") String domain,
    @JsonProperty("subtype") String subtype,
    @JsonProperty("primary_intent") String primaryIntent,
    @JsonProperty("secondary_intents") List<String> secondaryIntents,
    @JsonProperty("emotion_state") List<String> emotionState,
    @JsonProperty("target") String target,
    @JsonProperty("urgency") String urgency,
    @JsonProperty("safety_flag") String safetyFlag,
    @JsonProperty("needs_clarification") boolean needsClarification,
    @JsonProperty("confidence") double confidence,
    @JsonProperty("analysis_source") String analysisSource,
    @JsonProperty("analysis_version") String analysisVersion
) {
}
