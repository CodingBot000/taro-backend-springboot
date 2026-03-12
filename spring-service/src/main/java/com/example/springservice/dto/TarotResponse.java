package com.example.springservice.dto;

import java.util.List;

public record TarotResponse(
    List<SelectedCardResponse> cards,
    String interpretation,
    String backendVersion
) {
}
