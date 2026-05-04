package com.example.springservice.dto;

import java.util.List;

public record IntakeContextRequest(
    String flowId,
    String flowVersion,
    List<IntakeAnswerRequest> answers,
    String synthesizedQuestion
) {
}
