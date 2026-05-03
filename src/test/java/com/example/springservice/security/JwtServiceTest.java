package com.example.springservice.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class JwtServiceTest {

    @Test
    void rejectsBlankSecret() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("");

        assertThrows(IllegalStateException.class, () -> new JwtService(properties));
    }

    @Test
    void rejectsDefaultDevelopmentSecret() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("change-this-to-a-long-random-secret");

        assertThrows(IllegalStateException.class, () -> new JwtService(properties));
    }

    @Test
    void acceptsConfiguredSecretWithEnoughEntropy() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-jwt-secret-must-be-32-bytes!!");

        assertDoesNotThrow(() -> new JwtService(properties));
    }
}
