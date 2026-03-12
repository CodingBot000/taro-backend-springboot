package com.example.springservice.dto;

public record CategorySelectionRequest(
    String mainCategoryId,
    String subCategoryId
) {
}
