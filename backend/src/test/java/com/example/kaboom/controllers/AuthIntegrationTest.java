package com.example.kaboom.controllers;

import com.example.kaboom.entities.User;
import com.example.kaboom.repositories.UserRepository;
import com.example.kaboom.services.JwtServices;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class AuthIntegrationTest extends AbstractIntegrationTest {

    private static final String AUTH_URL = "/api/v1/auth";

    // Must satisfy isValidPassword: 12-64 chars, upper, lower, digit, special, no spaces
    private static final String PASSWORD = "SecurePass123!";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtServices jwtServices;

    @Autowired
    private PasswordEncoder passwordEncoder;


    // ---------- Registration ----------

    // 1. Successful registration

    @Test
    void shouldRegisterUserSuccessfully() {

        String email = uniqueEmail();

        ResponseEntity<String> response = register("Test User", email, PASSWORD);

        assertEquals(HttpStatus.CREATED, response.getStatusCode(), response.getBody());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("User registered successfully"),
                "register body was: " + response.getBody());

        User savedUser = userRepository.findByEmail(email).orElseThrow();

        assertEquals("Test User", savedUser.getName());
        assertEquals(100, savedUser.getCoins());

        assertNotEquals(PASSWORD, savedUser.getPasswordHash());
        assertTrue(passwordEncoder.matches(PASSWORD, savedUser.getPasswordHash()),
                "stored hash: " + savedUser.getPasswordHash());
    }


    // 2. Duplicate email

    @Test
    void shouldRejectDuplicateEmail() {

        String email = uniqueEmail();
        registerUser(email);

        ResponseEntity<String> response = register("Another User", email, PASSWORD);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode(), response.getBody());
    }


    // 3. Duplicate email differing only by case

    @Test
    void shouldRejectDuplicateEmailWithDifferentCase() {

        String email = uniqueEmail();
        registerUser(email);

        ResponseEntity<String> response =
                register("Another User", email.toUpperCase(), PASSWORD);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode(), response.getBody());
    }


    // 4. Invalid registration data

    @Test
    void shouldRejectInvalidRegistration() {

        ResponseEntity<String> response = register("", "invalid-email", "123");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), response.getBody());
    }


    // 5. Email normalized to lowercase

    @Test
    void shouldNormalizeEmailDuringRegistration() {

        String email = uniqueEmail().toUpperCase();

        ResponseEntity<String> response = register("Test User", email, PASSWORD);

        assertEquals(HttpStatus.CREATED, response.getStatusCode(), response.getBody());
        assertTrue(userRepository.findByEmail(email.toLowerCase()).isPresent());
    }


    // 6. Missing password field

    @Test
    void shouldRejectRegistrationWithMissingPassword() {

        String request = """
                {
                    "name": "Test User",
                    "email": "%s"
                }
                """.formatted(uniqueEmail());

        ResponseEntity<String> response = post(AUTH_URL + "/register", request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), response.getBody());
    }


    // 7. Malformed JSON

    @Test
    void shouldRejectMalformedJson() {

        ResponseEntity<String> response = post(AUTH_URL + "/register", "{ not valid json");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), response.getBody());
    }


    // 8. Empty body

    @Test
    void shouldRejectEmptyBody() {

        ResponseEntity<String> response = post(AUTH_URL + "/register", "");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), response.getBody());
    }


    // ---------- Password rules (12-64 chars, upper, lower, digit, special, no spaces) ----------

    @ParameterizedTest
    @ValueSource(strings = {
            "Abcdef1!xyz",       // 11 characters
            "securepass123!",    // no uppercase
            "SECUREPASS123!",    // no lowercase
            "SecurePassword!!",  // no digit
            "SecurePass1234",    // no special symbol
            "Secure Pass123!"    // contains a space
    })
    void shouldRejectPasswordsThatBreakTheRules(String badPassword) {

        ResponseEntity<String> response = register("Test User", uniqueEmail(), badPassword);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(),
                "password [" + badPassword + "] should be rejected: " + response.getBody());
    }

    @Test
    void shouldAcceptPasswordOfExactlyMinimumLength() {

        String password = "Abcdefgh1!xy"; // 12 characters

        assertEquals(12, password.length());
        assertEquals(HttpStatus.CREATED,
                register("Test User", uniqueEmail(), password).getStatusCode());
    }

    @Test
    void shouldAcceptPasswordOfExactlyMaximumLength() {

        String password = "Aa1!" + "a".repeat(60); // 64 characters

        assertEquals(64, password.length());
        assertEquals(HttpStatus.CREATED,
                register("Test User", uniqueEmail(), password).getStatusCode());
    }

    @Test
    void shouldRejectPasswordLongerThanMaximum() {

        String password = "Aa1!" + "a".repeat(61); // 65 characters

        assertEquals(65, password.length());
        assertEquals(HttpStatus.BAD_REQUEST,
                register("Test User", uniqueEmail(), password).getStatusCode());
    }


    // ---------- Login ----------

    // 9. Successful login

    @Test
    void shouldLoginWithCorrectCredentials() {

        String email = uniqueEmail();
        registerUser(email);

        ResponseEntity<String> response = loginRaw(email, PASSWORD);

        assertEquals(HttpStatus.OK, response.getStatusCode(), response.getBody());

        String body = response.getBody();

        assertNotNull(body);
        assertTrue(body.contains("accessToken"));
        assertTrue(body.contains("refreshToken"));
        assertTrue(body.contains("Bearer"));

        assertTrue(jwtServices.isValidAccessToken(extractJsonValue(body, "accessToken")));
        assertTrue(jwtServices.isValidRefreshToken(extractJsonValue(body, "refreshToken")));
    }


    // 10. Login with uppercase email

    @Test
    void shouldLoginWithUppercaseEmail() {

        String email = uniqueEmail();
        registerUser(email);

        ResponseEntity<String> response = loginRaw(email.toUpperCase(), PASSWORD);

        assertEquals(HttpStatus.OK, response.getStatusCode(), response.getBody());
    }


    // 11. Wrong password

    @Test
    void shouldRejectLoginWithWrongPassword() {

        String email = uniqueEmail();
        registerUser(email);

        ResponseEntity<String> response = loginRaw(email, "WrongPassword123!");

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode(), response.getBody());
    }


    // 12. Unknown email

    @Test
    void shouldRejectLoginWithUnknownEmail() {

        ResponseEntity<String> response = loginRaw("unknown@example.com", PASSWORD);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode(), response.getBody());
    }


    // ---------- Refresh ----------

    // 13. Successful refresh

    @Test
    void shouldRefreshAccessToken() {

        String email = uniqueEmail();
        registerUser(email);

        String refreshToken = jwtServices.generateRefreshToken(email);

        ResponseEntity<String> response = refresh(refreshToken);

        assertEquals(HttpStatus.OK, response.getStatusCode(), response.getBody());
        assertNotNull(response.getBody());

        String newAccessToken = extractJsonValue(response.getBody(), "accessToken");

        assertTrue(jwtServices.isValidAccessToken(newAccessToken));
    }


    // 14. Invalid refresh token

    @Test
    void shouldRejectInvalidRefreshToken() {

        ResponseEntity<String> response = refresh("invalid-token");

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode(), response.getBody());
    }


    // 15. Refresh token for a nonexistent user

    @Test
    void shouldRejectRefreshTokenForUnknownUser() {

        String refreshToken = jwtServices.generateRefreshToken(uniqueEmail());

        ResponseEntity<String> response = refresh(refreshToken);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode(), response.getBody());
    }


    // 16. Access token must not work as a refresh token

    @Test
    void shouldRejectAccessTokenUsedAsRefreshToken() {

        String email = uniqueEmail();
        registerUser(email);

        ResponseEntity<String> loginResponse = loginRaw(email, PASSWORD);

        assertEquals(HttpStatus.OK, loginResponse.getStatusCode(), loginResponse.getBody());

        String accessToken = extractJsonValue(loginResponse.getBody(), "accessToken");

        ResponseEntity<String> response = refresh(accessToken);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode(), response.getBody());
    }


    // ---------- Response hygiene ----------

    // 17. Responses must never leak the password hash

    @Test
    void shouldNotLeakPasswordHashInResponses() {

        String email = uniqueEmail();

        ResponseEntity<String> registerResponse = register("Test User", email, PASSWORD);
        ResponseEntity<String> loginResponse = loginRaw(email, PASSWORD);

        assertFalse(String.valueOf(registerResponse.getBody()).toLowerCase().contains("passwordhash"));
        assertFalse(String.valueOf(loginResponse.getBody()).toLowerCase().contains("passwordhash"));
    }


    // ---------- Helpers ----------

    private ResponseEntity<String> register(String name, String email, String password) {

        String request = """
                {
                    "name": "%s",
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(name, email, password);

        return post(AUTH_URL + "/register", request);
    }

    private void registerUser(String email) {

        ResponseEntity<String> response = register("Test User", email, PASSWORD);

        assertEquals(HttpStatus.CREATED, response.getStatusCode(), response.getBody());
    }

    private ResponseEntity<String> loginRaw(String email, String password) {

        String request = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(email, password);

        return post(AUTH_URL + "/login", request);
    }

    private ResponseEntity<String> refresh(String refreshToken) {

        String request = """
                {
                    "refreshToken": "%s"
                }
                """.formatted(refreshToken);

        return post(AUTH_URL + "/refresh", request);
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

    private String uniqueEmail() {
        return "test-" + UUID.randomUUID() + "@example.com";
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