package com.example.springservice.dto;

public record UiContextRequest(
    String locale,
    String categoryVersion
) {
}
