package com.example.springservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.springservice.dto.CategorySelectionRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class QuestionCategoryCatalogTest {

    private final QuestionCategoryCatalog catalog = new QuestionCategoryCatalog(new ObjectMapper());

    @Test
    void loadsManifestAndExposesSelectionTree() {
        assertEquals("category-v1", catalog.version());
        assertTrue(catalog.isValidSelection("love", "reunion"));
        assertTrue(catalog.domainIds().contains("finance"));
        assertTrue(catalog.subCategoryIds().contains("overall_flow"));
        assertEquals("general", catalog.fallbackDomain(new CategorySelectionRequest("unknown", "today")));
    }
}
