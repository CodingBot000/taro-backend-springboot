package com.example.springservice.service;

import com.example.springservice.dto.CategorySelectionRequest;
import com.example.springservice.dto.QuestionCategoryManifestResponse;
import com.example.springservice.dto.QuestionCategoryManifestResponse.MainCategory;
import com.example.springservice.dto.QuestionCategoryManifestResponse.SubCategory;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class QuestionCategoryCatalog {

    private static final String MANIFEST_RESOURCE_PATH = "question-categories/category-v1.json";

    private final QuestionCategoryManifestResponse manifest;
    private final Map<String, Set<String>> categorySelectionTree;
    private final Map<String, String> mainCategoryDomains;
    private final Set<String> domainIds;
    private final Set<String> subCategoryIds;

    public QuestionCategoryCatalog(ObjectMapper objectMapper) {
        this.manifest = loadManifest(objectMapper);
        this.categorySelectionTree = buildCategorySelectionTree(manifest);
        this.mainCategoryDomains = buildMainCategoryDomains(manifest);
        this.domainIds = Collections.unmodifiableSet(new LinkedHashSet<>(mainCategoryDomains.values()));
        this.subCategoryIds = buildSubCategoryIds(manifest);
    }

    public QuestionCategoryManifestResponse manifest() {
        return manifest;
    }

    public String version() {
        return manifest.version();
    }

    public Set<String> domainIds() {
        return domainIds;
    }

    public Set<String> subCategoryIds() {
        return subCategoryIds;
    }

    public boolean isValidSelection(String mainCategoryId, String subCategoryId) {
        if (mainCategoryId == null || subCategoryId == null) {
            return false;
        }

        Set<String> subCategories = categorySelectionTree.get(mainCategoryId.trim());
        return subCategories != null && subCategories.contains(subCategoryId.trim());
    }

    public String fallbackDomain(CategorySelectionRequest categorySelection) {
        if (categorySelection == null || categorySelection.mainCategoryId() == null) {
            return "general";
        }

        return mainCategoryDomains.getOrDefault(categorySelection.mainCategoryId().trim(), "general");
    }

    public String fallbackSubtype(CategorySelectionRequest categorySelection) {
        if (categorySelection == null || categorySelection.subCategoryId() == null) {
            return "unknown";
        }

        String subCategoryId = categorySelection.subCategoryId().trim();
        return subCategoryIds.contains(subCategoryId) ? subCategoryId : "unknown";
    }

    private QuestionCategoryManifestResponse loadManifest(ObjectMapper objectMapper) {
        ClassPathResource resource = new ClassPathResource(MANIFEST_RESOURCE_PATH);
        try (InputStream inputStream = resource.getInputStream()) {
            QuestionCategoryManifestResponse loaded = objectMapper.readValue(inputStream, QuestionCategoryManifestResponse.class);
            validateManifest(loaded);
            return loaded;
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to load question category manifest: " + MANIFEST_RESOURCE_PATH, exception);
        }
    }

    private void validateManifest(QuestionCategoryManifestResponse loaded) {
        if (loaded == null || loaded.version() == null || loaded.version().isBlank()) {
            throw new IllegalStateException("Question category manifest version is missing");
        }
        if (loaded.categories() == null || loaded.categories().isEmpty()) {
            throw new IllegalStateException("Question category manifest categories are missing");
        }
    }

    private Map<String, Set<String>> buildCategorySelectionTree(QuestionCategoryManifestResponse loaded) {
        LinkedHashMap<String, Set<String>> result = new LinkedHashMap<>();
        for (MainCategory category : loaded.categories()) {
            String mainCategoryId = requireNonBlank(category.id(), "main category id");
            if (result.containsKey(mainCategoryId)) {
                throw new IllegalStateException("Duplicate main category id in manifest: " + mainCategoryId);
            }
            result.put(mainCategoryId, buildSubCategorySet(category));
        }
        return Collections.unmodifiableMap(result);
    }

    private Map<String, String> buildMainCategoryDomains(QuestionCategoryManifestResponse loaded) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        for (MainCategory category : loaded.categories()) {
            String mainCategoryId = requireNonBlank(category.id(), "main category id");
            if (category.metadata() == null) {
                throw new IllegalStateException("Missing metadata for main category: " + mainCategoryId);
            }
            result.put(mainCategoryId, requireNonBlank(category.metadata().questionDomain(), "questionDomain"));
        }
        return Collections.unmodifiableMap(result);
    }

    private Set<String> buildSubCategoryIds(QuestionCategoryManifestResponse loaded) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (MainCategory category : loaded.categories()) {
            for (SubCategory subCategory : category.subcategories()) {
                String subCategoryId = requireNonBlank(subCategory.id(), "sub category id");
                result.add(subCategoryId);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    private Set<String> buildSubCategorySet(MainCategory category) {
        if (category.subcategories() == null || category.subcategories().isEmpty()) {
            throw new IllegalStateException("Subcategories are missing for main category: " + category.id());
        }

        LinkedHashSet<String> subCategoryIdsForMain = new LinkedHashSet<>();
        for (SubCategory subCategory : category.subcategories()) {
            String subCategoryId = requireNonBlank(subCategory.id(), "sub category id");
            if (!subCategoryIdsForMain.add(subCategoryId)) {
                throw new IllegalStateException(
                    "Duplicate sub category id for main category %s: %s".formatted(category.id(), subCategoryId)
                );
            }
        }
        return Collections.unmodifiableSet(subCategoryIdsForMain);
    }

    private String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Question category manifest field is missing: " + fieldName);
        }
        return value;
    }
}
