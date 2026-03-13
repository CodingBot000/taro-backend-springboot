package com.example.springservice.dto;

public record TarotRequest(
    String question,
    String readingType,
    String selectedCardsJson,
    CategorySelectionRequest categorySelection,
    UiContextRequest uiContext
) {
}
