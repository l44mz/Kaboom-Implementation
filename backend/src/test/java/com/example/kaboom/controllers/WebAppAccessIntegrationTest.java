package com.example.kaboom.controllers;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpStatus;
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

    @Test
    void theApiStillNeedsALogin() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/v1/users/me", String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}
