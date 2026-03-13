package com.example.springservice.security;

import java.util.Map;

public record GoogleUserInfo(
    String subject,
    String email,
    String name,
    String picture
) {
    public static GoogleUserInfo fromAttributes(Map<String, Object> attributes) {
        return new GoogleUserInfo(
            value(attributes.get("sub")),
            value(attributes.get("email")),
            value(attributes.get("name")),
            value(attributes.get("picture"))
        );
    }

    private static String value(Object rawValue) {
        return rawValue == null ? null : rawValue.toString();
    }
}
