package com.example.springservice.controller;

import com.example.springservice.dto.QuestionCategoryManifestResponse;
import com.example.springservice.service.QuestionCategoryCatalog;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class QuestionCategoryController {

    private final QuestionCategoryCatalog questionCategoryCatalog;

    public QuestionCategoryController(QuestionCategoryCatalog questionCategoryCatalog) {
        this.questionCategoryCatalog = questionCategoryCatalog;
    }

    @GetMapping("/question-categories")
    public QuestionCategoryManifestResponse getQuestionCategories() {
        return questionCategoryCatalog.manifest();
    }
}
