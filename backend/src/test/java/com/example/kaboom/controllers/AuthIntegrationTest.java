package com.example.kaboom.controllers;

import com.example.kaboom.entities.User;
import com.example.kaboom.repositories.UserRepository;
import com.example.kaboom.services.JwtServices;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.http.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class AuthIntegrationTest extends AbstractIntegrationTest {

    private static final String AUTH_URL = "/api/v1/auth";

    @Autowired
    private TestRestTemplate restTemplate;


    @Container
    static final MySQLContainer mysql =
            new MySQLContainer("mysql:8.4");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name",
                () -> "com.mysql.cj.jdbc.Driver");
    }


    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtServices jwtServices;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;


    // 1. Successful registration

    @Test
    void shouldRegisterUserSuccessfully() {

        String email = uniqueEmail();

        String request = """
                {
                    "name": "Test User",
                    "email": "%s",
                    "password": "SecurePass123"
                }
                """.formatted(email);

        ResponseEntity<String> response = post(
                AUTH_URL + "/register",
                request
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertTrue(response.getBody().contains("User registered successfully"));

        User savedUser = userRepository.findByEmail(email)
                .orElseThrow();

        assertEquals("Test User", savedUser.getName());
        assertEquals(100, savedUser.getCoins());

        assertNotEquals("SecurePass123", savedUser.getPasswordHash());
        assertTrue(passwordEncoder.matches(
                "SecurePass123",
                savedUser.getPasswordHash()
        ));
    }


    // 2. Duplicate email registration

    @Test
    void shouldRejectDuplicateEmail() {

        String email = uniqueEmail();

        registerUser(email);

        String request = """
                {
                    "name": "Another User",
                    "email": "%s",
                    "password": "SecurePass123"
                }
                """.formatted(email);

        ResponseEntity<String> response = post(
                AUTH_URL + "/register",
                request
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }


    // 3. Invalid registration data

    @Test
    void shouldRejectInvalidRegistration() {

        String request = """
                {
                    "name": "",
                    "email": "invalid-email",
                    "password": "123"
                }
                """;

        ResponseEntity<String> response = post(
                AUTH_URL + "/register",
                request
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }


    // 4. Email should be normalized to lowercase

    @Test
    void shouldNormalizeEmailDuringRegistration() {

        String email = uniqueEmail().toUpperCase();

        String request = """
                {
                    "name": "Test User",
                    "email": "%s",
                    "password": "SecurePass123"
                }
                """.formatted(email);

        ResponseEntity<String> response = post(
                AUTH_URL + "/register",
                request
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        assertTrue(userRepository.findByEmail(email.toLowerCase()).isPresent());
    }


    // 5. Successful login

    @Test
    void shouldLoginWithCorrectCredentials() {

        String email = uniqueEmail();

        registerUser(email);

        String request = """
                {
                    "email": "%s",
                    "password": "SecurePass123"
                }
                """.formatted(email);

        ResponseEntity<String> response = post(
                AUTH_URL + "/login",
                request
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());

        String body = response.getBody();

        assertNotNull(body);
        assertTrue(body.contains("accessToken"));
        assertTrue(body.contains("refreshToken"));
        assertTrue(body.contains("Bearer"));

        String accessToken = extractJsonValue(body, "accessToken");
        String refreshToken = extractJsonValue(body, "refreshToken");

        assertTrue(jwtServices.isValidAccessToken(accessToken));
        assertTrue(jwtServices.isValidRefreshToken(refreshToken));
    }


    // 6. Incorrect password

    @Test
    void shouldRejectLoginWithWrongPassword() {

        String email = uniqueEmail();

        registerUser(email);

        String request = """
                {
                    "email": "%s",
                    "password": "WrongPassword123"
                }
                """.formatted(email);

        ResponseEntity<String> response = post(
                AUTH_URL + "/login",
                request
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }


    // 7. Unknown email

    @Test
    void shouldRejectLoginWithUnknownEmail() {

        String request = """
                {
                    "email": "unknown@example.com",
                    "password": "SecurePass123"
                }
                """;

        ResponseEntity<String> response = post(
                AUTH_URL + "/login",
                request
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }


    // 8. Successful refresh

    @Test
    void shouldRefreshAccessToken() {

        String email = uniqueEmail();

        registerUser(email);

        String refreshToken = jwtServices.generateRefreshToken(email);

        String request = """
                {
                    "refreshToken": "%s"
                }
                """.formatted(refreshToken);

        ResponseEntity<String> response = post(
                AUTH_URL + "/refresh",
                request
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertNotNull(response.getBody());

        String newAccessToken =
                extractJsonValue(response.getBody(), "accessToken");

        assertTrue(jwtServices.isValidAccessToken(newAccessToken));
    }


    // 9. Invalid refresh token

    @Test
    void shouldRejectInvalidRefreshToken() {

        String request = """
                {
                    "refreshToken": "invalid-token"
                }
                """;

        ResponseEntity<String> response = post(
                AUTH_URL + "/refresh",
                request
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }


    // 10. Refresh token belonging to nonexistent user

    @Test
    void shouldRejectRefreshTokenForUnknownUser() {

        String refreshToken =
                jwtServices.generateRefreshToken(uniqueEmail());

        String request = """
                {
                    "refreshToken": "%s"
                }
                """.formatted(refreshToken);

        ResponseEntity<String> response = post(
                AUTH_URL + "/refresh",
                request
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }


    // Helper methods

    private void registerUser(String email) {

        String request = """
                {
                    "name": "Test User",
                    "email": "%s",
                    "password": "SecurePass123"
                }
                """.formatted(email);

        ResponseEntity<String> response = post(
                AUTH_URL + "/register",
                request
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    private ResponseEntity<String> post(String url, String body) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        return restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class
        );
    }

    private String uniqueEmail() {
        return "test-" + UUID.randomUUID() + "@example.com";
    }

    private String extractJsonValue(String json, String key) {
        try {
            com.fasterxml.jackson.databind.JsonNode node =
                    new com.fasterxml.jackson.databind.ObjectMapper()
                            .readTree(json);

            return node.get(key).asText();

        } catch (Exception e) {
            throw new RuntimeException("Could not extract " + key, e);
        }
    }

    // 11. Missing password field

    @Test
    void shouldRejectRegistrationWithMissingPassword() {

        String request = """
            {
                "name": "Test User",
                "email": "%s"
            }
            """.formatted(uniqueEmail());

        ResponseEntity<String> response = post(AUTH_URL + "/register", request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }


// 12. Malformed JSON

    @Test
    void shouldRejectMalformedJson() {

        ResponseEntity<String> response = post(AUTH_URL + "/register", "{ not valid json");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }


// 13. Empty body

    @Test
    void shouldRejectEmptyBody() {

        ResponseEntity<String> response = post(AUTH_URL + "/register", "");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }


// 14. Duplicate email differing only by case

    @Test
    void shouldRejectDuplicateEmailWithDifferentCase() {

        String email = uniqueEmail();

        registerUser(email);

        String request = """
            {
                "name": "Another User",
                "email": "%s",
                "password": "SecurePass123"
            }
            """.formatted(email.toUpperCase());

        ResponseEntity<String> response = post(AUTH_URL + "/register", request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }


// 15. Login with uppercase email

    @Test
    void shouldLoginWithUppercaseEmail() {

        String email = uniqueEmail();

        registerUser(email);

        String request = """
            {
                "email": "%s",
                "password": "SecurePass123"
            }
            """.formatted(email.toUpperCase());

        ResponseEntity<String> response = post(AUTH_URL + "/login", request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }


// 16. Access token must not work as a refresh token

    @Test
    void shouldRejectAccessTokenUsedAsRefreshToken() {

        String email = uniqueEmail();

        registerUser(email);

        String loginRequest = """
            {
                "email": "%s",
                "password": "SecurePass123"
            }
            """.formatted(email);

        ResponseEntity<String> loginResponse = post(AUTH_URL + "/login", loginRequest);

        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());

        String accessToken = extractJsonValue(loginResponse.getBody(), "accessToken");

        String refreshRequest = """
            {
                "refreshToken": "%s"
            }
            """.formatted(accessToken);

        ResponseEntity<String> response = post(AUTH_URL + "/refresh", refreshRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }


// 17. Responses must never leak the password hash

    @Test
    void shouldNotLeakPasswordHashInResponses() {

        String email = uniqueEmail();

        String registerRequest = """
            {
                "name": "Test User",
                "email": "%s",
                "password": "SecurePass123"
            }
            """.formatted(email);

        ResponseEntity<String> registerResponse =
                post(AUTH_URL + "/register", registerRequest);

        String loginRequest = """
            {
                "email": "%s",
                "password": "SecurePass123"
            }
            """.formatted(email);

        ResponseEntity<String> loginResponse = post(AUTH_URL + "/login", loginRequest);

        assertFalse(String.valueOf(registerResponse.getBody()).toLowerCase().contains("passwordhash"));
        assertFalse(String.valueOf(loginResponse.getBody()).toLowerCase().contains("passwordhash"));
    }
}
