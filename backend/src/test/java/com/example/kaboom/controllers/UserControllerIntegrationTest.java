package com.example.kaboom.controllers;

import com.example.kaboom.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class UserControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String AUTH_URL = "/api/v1/auth";
    private static final String USERS_URL = "/api/v1/users";
    private static final String PASSWORD = "SecurePass123!";
    private static final String NEW_PASSWORD = "NewSecurePass456!";

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;


    // GET /users/me

    @Test
    void shouldReturnOwnProfile() {

        String email = registerUser();
        String token = login(email, PASSWORD);

        ResponseEntity<String> response = send(HttpMethod.GET, USERS_URL + "/me", null, token);

        assertStatus(HttpStatus.OK, response);
        assertTrue(response.getBody().contains(email));
        assertFalse(response.getBody().toLowerCase().contains("passwordhash"));
    }


    // GET /users/{id}

    @Test
    void shouldRequireAuthenticationForGetUserById() {

        ResponseEntity<String> response =
                send(HttpMethod.GET, USERS_URL + "/" + UUID.randomUUID(), null, null);

        assertStatus(HttpStatus.UNAUTHORIZED, response);
    }

    @Test
    void shouldNotLeakPasswordHashFromGetUserById() {

        String email = registerUser();
        String token = login(email, PASSWORD);
        UUID id = userRepository.findByEmail(email).orElseThrow().getId();

        ResponseEntity<String> response =
                send(HttpMethod.GET, USERS_URL + "/" + id, null, token);

        assertFalse(String.valueOf(response.getBody()).toLowerCase().contains("passwordhash"),
                "response leaked the hash: " + response.getBody());
    }

    @Test
    void shouldNotAllowUserToReadAnotherUser() {

        String emailA = registerUser();
        String emailB = registerUser();
        String tokenA = login(emailA, PASSWORD);
        UUID idB = userRepository.findByEmail(emailB).orElseThrow().getId();

        ResponseEntity<String> response =
                send(HttpMethod.GET, USERS_URL + "/" + idB, null, tokenA);

        assertTrue(
                response.getStatusCode() == HttpStatus.FORBIDDEN
                        || response.getStatusCode() == HttpStatus.NOT_FOUND,
                "Expected 403 or 404 but got " + response.getStatusCode() + ": " + response.getBody()
        );
        assertFalse(String.valueOf(response.getBody()).contains(emailB));
    }


    // PUT /users/me

    @Test
    void shouldUpdateOwnProfile() {

        String email = registerUser();
        String token = login(email, PASSWORD);

        String request = """
                {
                    "name": "Updated Name",
                    "email": "%s"
                }
                """.formatted(email);

        ResponseEntity<String> response =
                send(HttpMethod.PUT, USERS_URL + "/me", request, token);

        assertStatus(HttpStatus.OK, response);
        assertEquals("Updated Name",
                userRepository.findByEmail(email).orElseThrow().getName());
    }

    @Test
    void shouldRejectProfileUpdateWithTakenEmail() {

        String emailA = registerUser();
        String emailB = registerUser();
        String tokenB = login(emailB, PASSWORD);

        String request = """
                {
                    "name": "New Name",
                    "email": "%s"
                }
                """.formatted(emailA);

        assertStatus(HttpStatus.CONFLICT,
                send(HttpMethod.PUT, USERS_URL + "/me", request, tokenB));
    }

    @Test
    void shouldRejectInvalidProfileUpdate() {

        String token = login(registerUser(), PASSWORD);

        String request = """
                {
                    "name": "",
                    "email": "not-an-email"
                }
                """;

        assertStatus(HttpStatus.BAD_REQUEST,
                send(HttpMethod.PUT, USERS_URL + "/me", request, token));
    }

    @Test
    void shouldRequireAuthenticationToUpdateProfile() {

        String request = """
                {
                    "name": "Name",
                    "email": "someone@example.com"
                }
                """;

        assertStatus(HttpStatus.UNAUTHORIZED,
                send(HttpMethod.PUT, USERS_URL + "/me", request, null));
    }


    // PUT /users/me/password

    @Test
    void shouldRejectPasswordChangeWithWrongCurrentPassword() {

        String token = login(registerUser(), PASSWORD);

        assertStatus(HttpStatus.BAD_REQUEST,
                changePassword(token, "WrongPassword123!", NEW_PASSWORD));
    }

    @Test
    void shouldRejectPasswordChangeToWeakPassword() {

        String token = login(registerUser(), PASSWORD);

        // violates the 12-character minimum used at registration
        assertStatus(HttpStatus.BAD_REQUEST,
                changePassword(token, PASSWORD, "short1!A"));
    }

    @Test
    void shouldChangePasswordSuccessfully() {

        String email = registerUser();
        String token = login(email, PASSWORD);

        assertStatus(HttpStatus.OK, changePassword(token, PASSWORD, NEW_PASSWORD));

        assertEquals(HttpStatus.OK, loginRaw(email, NEW_PASSWORD).getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, loginRaw(email, PASSWORD).getStatusCode());
    }


    // DELETE /users/me

    @Test
    void shouldDeleteOwnAccount() {

        String email = registerUser();
        String token = login(email, PASSWORD);

        assertStatus(HttpStatus.OK, send(HttpMethod.DELETE, USERS_URL + "/me", null, token));

        assertTrue(userRepository.findByEmail(email).isEmpty());
        assertEquals(HttpStatus.UNAUTHORIZED, loginRaw(email, PASSWORD).getStatusCode());
    }


    // GET /users/me/items and PATCH /users/me/items/{id}

    @Test
    void shouldReturnEmptyItemListForNewUser() {

        String token = login(registerUser(), PASSWORD);

        ResponseEntity<String> response =
                send(HttpMethod.GET, USERS_URL + "/me/items", null, token);

        assertStatus(HttpStatus.OK, response);
        assertEquals("[]", response.getBody().trim());
    }

    @Test
    void shouldReturn404WhenEquippingItemNotOwned() {

        String token = login(registerUser(), PASSWORD);

        ResponseEntity<String> response = send(
                HttpMethod.PATCH,
                USERS_URL + "/me/items/" + UUID.randomUUID() + "?equipped=true",
                null,
                token
        );

        assertStatus(HttpStatus.NOT_FOUND, response);
    }


    // Helpers

    private void assertStatus(HttpStatus expected, ResponseEntity<String> response) {
        assertEquals(expected, response.getStatusCode(), response.getBody());
    }

    private ResponseEntity<String> changePassword(String token, String current, String next) {

        String request = """
                {
                    "currentPassword": "%s",
                    "newPassword": "%s"
                }
                """.formatted(current, next);

        return send(HttpMethod.PUT, USERS_URL + "/me/password", request, token);
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

        assertStatus(HttpStatus.CREATED, send(HttpMethod.POST, AUTH_URL + "/register", body, null));
        return email;
    }

    private ResponseEntity<String> loginRaw(String email, String password) {

        String body = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(email, password);

        return send(HttpMethod.POST, AUTH_URL + "/login", body, null);
    }

    private String login(String email, String password) {

        ResponseEntity<String> response = loginRaw(email, password);

        assertStatus(HttpStatus.OK, response);
        assertNotNull(response.getBody());

        return extractJsonValue(response.getBody(), "accessToken");
    }

    private ResponseEntity<String> send(HttpMethod method, String url, String body, String token) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (token != null) {
            headers.setBearerAuth(token);
        }

        return restTemplate.exchange(url, method, new HttpEntity<>(body, headers), String.class);
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