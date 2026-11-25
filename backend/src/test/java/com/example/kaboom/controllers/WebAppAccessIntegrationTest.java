package com.example.kaboom.controllers;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * The Flutter web app is served by this backend (see the Dockerfile).
 * A browser opening the site has no token yet, so the app's files must not need one,
 * while the API still does.
 */
class WebAppAccessIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    // In tests the Flutter files are not built, so these answer 404 (not found).
    // What matters is that they are not 401 (login required).
    @ParameterizedTest
    @ValueSource(strings = {
            "/",
            "/index.html",
            "/main.dart.js",
            "/flutter_bootstrap.js",
            "/manifest.json",
            "/favicon.png",
            "/assets/AssetManifest.bin.json",
            "/canvaskit/canvaskit.wasm",
            "/icons/Icon-192.png"
    })
    void webAppFilesDoNotNeedALogin(String path) {
        ResponseEntity<String> response = restTemplate.getForEntity(path, String.class);

        assertNotEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode(), path);
        assertNotEquals(HttpStatus.FORBIDDEN, response.getStatusCode(), path);
    }

    // On Railway the browser is on https://<host>, but the backend is reached through a proxy.
    // The web app's login must not be refused as a cross-site (CORS) request: 401, not 403.
    @Test
    void theWebAppCanCallItsOwnApiBehindAProxy() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.ORIGIN, "https://kaboom.example.com");
        headers.set("X-Forwarded-Proto", "https");
        headers.set("X-Forwarded-Host", "kaboom.example.com");
        String body = "{\"email\":\"nobody@example.com\",\"password\":\"wrong-password\"}";

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/auth/login", new HttpEntity<>(body, headers), String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void theApiStillNeedsALogin() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/users/me", String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}
