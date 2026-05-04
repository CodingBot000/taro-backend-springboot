package com.example.springservice.dto;

public record IntakeAnswerRequest(
    String questionId,
    String type,
    String value,
    String label
) {
}
