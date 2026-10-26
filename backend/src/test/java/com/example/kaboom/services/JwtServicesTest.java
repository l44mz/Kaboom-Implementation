package com.example.kaboom.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JwtServices.
 * No Mockito and no Spring: JwtServices only needs a Base64 secret, so we create the real object.
 */
class JwtServicesTest {

    
    private static final String SECRET = base64("test-secret-key-for-jwt-unit-tests-0123456789");
    private static final String OTHER_SECRET = base64("a-completely-different-secret-key-0123456789!!");

    private JwtServices jwtServices;

    @BeforeEach
    void setUp() {
        jwtServices = new JwtServices(SECRET);
    }

    // ---------- helpers ----------

    private static String base64(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    private static SecretKey keyFor(String base64Secret) {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
    }

    /** Builds a token signed with the given secret that expired an hour ago. */
    private static String expiredToken(String base64Secret, String type) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject("john_12@example.com")
                .claim("type", type)
                .issuedAt(new Date(now - 2 * 60 * 60 * 1000))
                .expiration(new Date(now - 60 * 60 * 1000))
                .signWith(keyFor(base64Secret))
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(keyFor(SECRET)).build()
                .parseSignedClaims(token).getPayload();
    }

    // ---------- constructor ----------

    @Test
    void constructor_secretTooShort_throwsWeakKeyException() {
        String shortSecret = base64("too-short");

        assertThrows(WeakKeyException.class, () -> new JwtServices(shortSecret));
    }

    // ---------- access tokens ----------

    @Test
    void generateToken_isValidAccessToken_andNotValidRefreshToken() {
        String token = jwtServices.generateToken("john_12@example.com", "USER");

        assertTrue(jwtServices.isValidAccessToken(token));
        assertFalse(jwtServices.isValidRefreshToken(token));
    }

    @Test
    void generateToken_containsEmailAndRole() {
        String token = jwtServices.generateToken("john_12@example.com", "ADMIN");

        Claims claims = parseClaims(token);
        assertEquals("john_12@example.com", claims.getSubject());
        assertEquals("ADMIN", claims.get("role", String.class));
        assertEquals("access", claims.get("type", String.class));
    }

    @Test
    void generateToken_expiresInAboutTenMinutes() {
        String token = jwtServices.generateToken("john_12@example.com", "USER");

        Claims claims = parseClaims(token);
        long lifetimeMs = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertEquals(10 * 60 * 1000, lifetimeMs);
    }

    // ---------- refresh tokens ----------

    @Test
    void generateRefreshToken_isValidRefreshToken_andNotValidAccessToken() {
        String token = jwtServices.generateRefreshToken("john_12@example.com");

        assertTrue(jwtServices.isValidRefreshToken(token));
        assertFalse(jwtServices.isValidAccessToken(token));
    }

    @Test
    void generateRefreshToken_expiresInAboutSevenDays() {
        String token = jwtServices.generateRefreshToken("john_12@example.com");

        Claims claims = parseClaims(token);
        long lifetimeMs = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertEquals(7L * 24 * 60 * 60 * 1000, lifetimeMs);
    }

    // ---------- extractEmail ----------

    @Test
    void extractEmail_fromAccessToken_returnsEmail() {
        String token = jwtServices.generateToken("john_12@example.com", "USER");

        assertEquals("john_12@example.com", jwtServices.extractEmail(token));
    }

    @Test
    void extractEmail_fromRefreshToken_returnsEmail() {
        String token = jwtServices.generateRefreshToken("john_12@example.com");

        assertEquals("john_12@example.com", jwtServices.extractEmail(token));
    }

    @Test
    void extractEmail_expiredToken_throwsExpiredJwtException() {
        String token = expiredToken(SECRET, "access");

        assertThrows(ExpiredJwtException.class, () -> jwtServices.extractEmail(token));
    }

    // ---------- rejecting bad tokens ----------

    @Test
    void expiredAccessToken_isNotValid() {
        assertFalse(jwtServices.isValidAccessToken(expiredToken(SECRET, "access")));
    }

    @Test
    void expiredRefreshToken_isNotValid() {
        assertFalse(jwtServices.isValidRefreshToken(expiredToken(SECRET, "refresh")));
    }

    @Test
    void tokenSignedWithDifferentSecret_isNotValid() {
        JwtServices otherService = new JwtServices(OTHER_SECRET);
        String forgedAccess = otherService.generateToken("john_12@example.com", "ADMIN");
        String forgedRefresh = otherService.generateRefreshToken("john_12@example.com");

        assertFalse(jwtServices.isValidAccessToken(forgedAccess));
        assertFalse(jwtServices.isValidRefreshToken(forgedRefresh));
    }

    @Test
    void tokenWithTamperedPayload_isNotValid() {
        String admin = jwtServices.generateToken("john_12@example.com", "ADMIN");
        String user = jwtServices.generateToken("john_12@example.com", "USER");

        // header + payload from the ADMIN token, signature from the USER token
        String[] adminParts = admin.split("\\.");
        String[] userParts = user.split("\\.");
        String tampered = adminParts[0] + "." + adminParts[1] + "." + userParts[2];

        assertFalse(jwtServices.isValidAccessToken(tampered));
    }

    @Test
    void garbageToken_isNotValid() {
        assertFalse(jwtServices.isValidAccessToken("not.a.jwt"));
        assertFalse(jwtServices.isValidRefreshToken("not.a.jwt"));
        assertFalse(jwtServices.isValidAccessToken("garbage"));
    }

    @Test
    void nullOrEmptyToken_isNotValid() {
        assertFalse(jwtServices.isValidAccessToken(null));
        assertFalse(jwtServices.isValidRefreshToken(null));
        assertFalse(jwtServices.isValidAccessToken(""));
        assertFalse(jwtServices.isValidRefreshToken(""));
    }

    // ---------- expiration seconds ----------

    @Test
    void getAccessExpirationSeconds_returnsSixHundred() {
        assertEquals(600, jwtServices.getAccessExpirationSeconds());
    }
}
