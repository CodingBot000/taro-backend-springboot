package com.example.springservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "jwt.secret=test-jwt-secret-must-be-32-bytes!!",
    "spring.security.oauth2.client.registration.google.client-id=test-client-id",
    "spring.security.oauth2.client.registration.google.client-secret=test-client-secret"
})
class SpringServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
