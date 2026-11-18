package com.example.kaboom.controllers;

import com.example.kaboom.entities.User;
import com.example.kaboom.repositories.UserRepository;
import com.example.kaboom.services.JwtServices;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.*;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SecurityIntegrationTest extends AbstractIntegrationTest {

    private static final String AUTH_URL = "/api/v1/auth";
    private static final String PROTECTED_URL = "/api/v1/users/me"; // TODO: use a real protected endpoint

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JwtServices jwtServices;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldReturn401WhenNoTokenProvided() {
        ResponseEntity<String> response = get(PROTECTED_URL, null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void shouldReturn401WhenTokenIsMalformed() {
        ResponseEntity<String> response = get(PROTECTED_URL, "not-a-real-token");

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void shouldAllowAccessWithValidAccessToken() {
        String email = registerUser();
        String accessToken = loginAndGetAccessToken(email);

        ResponseEntity<String> response = get(PROTECTED_URL, accessToken);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void shouldRejectRefreshTokenUsedAsAccessToken() {
        String email = registerUser();
        String refreshToken = jwtServices.generateRefreshToken(email);

        ResponseEntity<String> response = get(PROTECTED_URL, refreshToken);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void shouldRejectValidTokenForDeletedUser() {
        String email = registerUser();
        String accessToken = loginAndGetAccessToken(email);

        User user = userRepository.findByEmail(email).orElseThrow();
        userRepository.delete(user);

        ResponseEntity<String> response = get(PROTECTED_URL, accessToken);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    // Helpers

    private String registerUser() {
        String email = "test-" + UUID.randomUUID() + "@example.com";

        String body = """
                {
                    "name": "Test User",
                    "email": "%s",
                    "password": "SecurePass123"
                }
                """.formatted(email);

        ResponseEntity<String> response = post(AUTH_URL + "/register", body);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        return email;
    }

    private String loginAndGetAccessToken(String email) {
        String body = """
                {
                    "email": "%s",
                    "password": "SecurePass123"
                }
                """.formatted(email);

        ResponseEntity<String> response = post(AUTH_URL + "/login", body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        return extractJsonValue(response.getBody(), "accessToken");
    }

    private ResponseEntity<String> post(String url, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return restTemplate.exchange(
                url,
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                String.class
        );
    }

    private ResponseEntity<String> get(String url, String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
        );
    }

    private String extractJsonValue(String json, String key) {
        try {
            com.fasterxml.jackson.databind.JsonNode node =
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);

            return node.get(key).asText();

        } catch (Exception e) {
            throw new RuntimeException("Could not extract " + key, e);
        }
    }
}