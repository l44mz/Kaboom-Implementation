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
    private static final String PROTECTED_URL = "/api/v1/users/me";
    private static final String PASSWORD = "SecurePass123!";

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private JwtServices jwtServices;
    @Autowired private UserRepository userRepository;


    @Test
    void shouldReturn401WhenNoTokenProvided() {

        assertStatus(HttpStatus.UNAUTHORIZED, get(PROTECTED_URL, null));
    }

    @Test
    void shouldReturn401WhenTokenIsMalformed() {

        assertStatus(HttpStatus.UNAUTHORIZED, get(PROTECTED_URL, "not-a-real-token"));
    }

    @Test
    void shouldAllowAccessWithValidAccessToken() {

        String email = registerUser();
        String accessToken = loginAndGetAccessToken(email);

        assertStatus(HttpStatus.OK, get(PROTECTED_URL, accessToken));
    }

    @Test
    void shouldRejectRefreshTokenUsedAsAccessToken() {

        String email = registerUser();
        String refreshToken = jwtServices.generateRefreshToken(email);

        assertStatus(HttpStatus.UNAUTHORIZED, get(PROTECTED_URL, refreshToken));
    }

    @Test
    void shouldRejectValidTokenForDeletedUser() {

        String email = registerUser();
        String accessToken = loginAndGetAccessToken(email);

        User user = userRepository.findByEmail(email).orElseThrow();
        userRepository.delete(user);

        assertStatus(HttpStatus.UNAUTHORIZED, get(PROTECTED_URL, accessToken));
    }

    @Test
    void shouldRejectAuthorizationHeaderWithWrongScheme() {

        String email = registerUser();
        String accessToken = loginAndGetAccessToken(email);

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + accessToken);

        ResponseEntity<String> response = restTemplate.exchange(
                PROTECTED_URL, HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertStatus(HttpStatus.UNAUTHORIZED, response);
    }


    // Helpers

    private void assertStatus(HttpStatus expected, ResponseEntity<String> response) {
        assertEquals(expected, response.getStatusCode(), response.getBody());
    }

    private String registerUser() {

        String email = "test-" + UUID.randomUUID() + "@example.com";

        String body = """
                {
                    "name": "Test User",
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(email, PASSWORD);

        assertStatus(HttpStatus.CREATED, post(AUTH_URL + "/register", body));
        return email;
    }

    private String loginAndGetAccessToken(String email) {

        String body = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(email, PASSWORD);

        ResponseEntity<String> response = post(AUTH_URL + "/login", body);

        assertStatus(HttpStatus.OK, response);
        assertNotNull(response.getBody());

        return extractJsonValue(response.getBody(), "accessToken");
    }

    private ResponseEntity<String> post(String url, String body) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        return restTemplate.exchange(
                url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    }

    private ResponseEntity<String> get(String url, String token) {

        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }

        return restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
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