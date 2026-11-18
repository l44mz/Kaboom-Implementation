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
    private static final String PASSWORD = "SecurePass123";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;


    // 1. GET /users/{id} requires authentication

    @Test
    void shouldRequireAuthenticationForGetUserById() {

        ResponseEntity<String> response =
                send(HttpMethod.GET, USERS_URL + "/" + UUID.randomUUID(), null, null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }


    // 2. GET /users/{id} must never leak the password hash (even for your own id)

    @Test
    void shouldNotLeakPasswordHashFromGetUserById() {

        String email = registerUser();
        String token = login(email, PASSWORD);
        UUID id = userRepository.findByEmail(email).orElseThrow().getId();

        ResponseEntity<String> response =
                send(HttpMethod.GET, USERS_URL + "/" + id, null, token);

        assertFalse(String.valueOf(response.getBody()).toLowerCase().contains("passwordhash"));
    }


    // 3. A user must not be able to read another user's account

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
                "Expected 403 or 404 but got " + response.getStatusCode()
        );
        assertFalse(String.valueOf(response.getBody()).contains(emailB));
    }


    // 4. Update profile: email already taken

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

        ResponseEntity<String> response =
                send(HttpMethod.PUT, USERS_URL + "/me", request, tokenB);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    }


    // 5. Update profile: invalid data

    @Test
    void shouldRejectInvalidProfileUpdate() {

        String email = registerUser();
        String token = login(email, PASSWORD);

        String request = """
                {
                    "name": "",
                    "email": "not-an-email"
                }
                """;

        ResponseEntity<String> response =
                send(HttpMethod.PUT, USERS_URL + "/me", request, token);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }


    // 6. Change password: wrong current password

    @Test
    void shouldRejectPasswordChangeWithWrongCurrentPassword() {

        String email = registerUser();
        String token = login(email, PASSWORD);

        String request = """
                {
                    "currentPassword": "WrongPassword123",
                    "newPassword": "NewSecurePass456"
                }
                """;

        ResponseEntity<String> response =
                send(HttpMethod.PUT, USERS_URL + "/me/password", request, token);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }


    // 7. Change password: success, new password works and old one does not

    @Test
    void shouldChangePasswordSuccessfully() {

        String email = registerUser();
        String token = login(email, PASSWORD);

        String request = """
                {
                    "currentPassword": "%s",
                    "newPassword": "NewSecurePass456"
                }
                """.formatted(PASSWORD);

        ResponseEntity<String> response =
                send(HttpMethod.PUT, USERS_URL + "/me/password", request, token);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        assertEquals(HttpStatus.OK, loginRaw(email, "NewSecurePass456").getStatusCode());
        assertEquals(HttpStatus.UNAUTHORIZED, loginRaw(email, PASSWORD).getStatusCode());
    }


    // 8. Delete account: user is gone and cannot log in again

    @Test
    void shouldDeleteOwnAccount() {

        String email = registerUser();
        String token = login(email, PASSWORD);

        ResponseEntity<String> response =
                send(HttpMethod.DELETE, USERS_URL + "/me", null, token);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(userRepository.findByEmail(email).isEmpty());
        assertEquals(HttpStatus.UNAUTHORIZED, loginRaw(email, PASSWORD).getStatusCode());
    }


    // 9. A new user owns no items

    @Test
    void shouldReturnEmptyItemListForNewUser() {

        String email = registerUser();
        String token = login(email, PASSWORD);

        ResponseEntity<String> response =
                send(HttpMethod.GET, USERS_URL + "/me/items", null, token);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("[]", response.getBody().trim());
    }


    // 10. Equipping an item the user does not own

    @Test
    void shouldReturn404WhenEquippingItemNotOwned() {

        String email = registerUser();
        String token = login(email, PASSWORD);

        ResponseEntity<String> response = send(
                HttpMethod.PATCH,
                USERS_URL + "/me/items/" + UUID.randomUUID() + "?equipped=true",
                null,
                token
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }


    // Helpers

    private String registerUser() {

        String email = "test-" + UUID.randomUUID() + "@example.com";

        String body = """
                {
                    "name": "Test User",
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(email, PASSWORD);

        ResponseEntity<String> response =
                send(HttpMethod.POST, AUTH_URL + "/register", body, null);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
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

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        return extractJsonValue(response.getBody(), "accessToken");
    }

    private ResponseEntity<String> send(HttpMethod method, String url, String body, String token) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (token != null) {
            headers.setBearerAuth(token);
        }

        return restTemplate.exchange(
                url,
                method,
                new HttpEntity<>(body, headers),
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