package com.example.springservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record QuestionCategoryManifestResponse(
    String version,
    List<MainCategory> categories
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MainCategory(
        String id,
        String label,
        String description,
        MainCategoryMetadata metadata,
        String defaultPlaceholder,
        List<SubCategory> subcategories
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MainCategoryMetadata(
        String questionDomain
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SubCategory(
        String id,
        String label,
        String shortLabel,
        String placeholder,
        List<String> examples,
        SubCategoryMetadata metadata
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SubCategoryMetadata(
        String questionSubtypeOneCard,
        String questionSubtypeThreeCard
    ) {
    }
}
