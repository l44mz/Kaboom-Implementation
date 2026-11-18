
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServicesTest {

    private static final String SECRET =
            base64("test-secret-key-for-jwt-unit-tests-0123456789");

    private static final String OTHER_SECRET =
            base64("a-completely-different-secret-key-0123456789!!");

    private static final String EMAIL = "john_12@example.com";

    private static final long TEN_MINUTES = 10L * 60 * 1000;
    private static final long SEVEN_DAYS = 7L * 24 * 60 * 60 * 1000;
    private static final long NINETY_DAYS = 90L * 24 * 60 * 60 * 1000;

    private JwtServices jwtServices;

    @BeforeEach
    void setUp() {
        jwtServices = new JwtServices(SECRET);
    }

    // ---------- Helpers ----------

    private static String base64(String text) {
        return Base64.getEncoder()
                .encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    private static SecretKey keyFor(String base64Secret) {
        return Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(base64Secret)
        );
    }

    private static String expiredToken(
            String base64Secret,
            String type,
            String subject,
            String claimName,
            String claimValue
    ) {
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .subject(subject)
                .claim("type", type)
                .claim(claimName, claimValue)
                .issuedAt(new Date(now - 2 * 60 * 60 * 1000))
                .expiration(new Date(now - 60 * 60 * 1000))
                .signWith(keyFor(base64Secret))
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(keyFor(SECRET))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // ---------- Constructor ----------

    @Test
    void constructor_secretTooShort_throwsWeakKeyException() {
        String shortSecret = base64("too-short");

        assertThrows(
                WeakKeyException.class,
                () -> new JwtServices(shortSecret)
        );
    }

    // ---------- User Access Tokens ----------

    @Test
    void generateToken_isValidAccessToken_andNotValidRefreshToken() {
        String token = jwtServices.generateToken(EMAIL, "USER");

        assertTrue(jwtServices.isValidAccessToken(token));
        assertFalse(jwtServices.isValidRefreshToken(token));
    }

    @Test
    void generateToken_containsEmailAndRole() {
        String token = jwtServices.generateToken(EMAIL, "ADMIN");

        Claims claims = parseClaims(token);

        assertEquals(EMAIL, claims.getSubject());
        assertEquals("ADMIN", claims.get("role", String.class));
        assertEquals("access", claims.get("type", String.class));
    }

    @Test
    void generateToken_expiresInTenMinutes() {
        String token = jwtServices.generateToken(EMAIL, "USER");

        Claims claims = parseClaims(token);

        long lifetime = claims.getExpiration().getTime()
                - claims.getIssuedAt().getTime();

        assertEquals(TEN_MINUTES, lifetime);
    }

    // ---------- User Refresh Tokens ----------

    @Test
    void generateRefreshToken_isValidRefreshToken_andNotValidAccessToken() {
        String token = jwtServices.generateRefreshToken(EMAIL);

        assertTrue(jwtServices.isValidRefreshToken(token));
        assertFalse(jwtServices.isValidAccessToken(token));
    }

    @Test
    void generateRefreshToken_containsEmailAndType() {
        String token = jwtServices.generateRefreshToken(EMAIL);

        Claims claims = parseClaims(token);

        assertEquals(EMAIL, claims.getSubject());
        assertEquals("refresh", claims.get("type", String.class));
    }

    @Test
    void generateRefreshToken_expiresInSevenDays() {
        String token = jwtServices.generateRefreshToken(EMAIL);

        Claims claims = parseClaims(token);

        long lifetime = claims.getExpiration().getTime()
                - claims.getIssuedAt().getTime();

        assertEquals(SEVEN_DAYS, lifetime);
    }

    // ---------- Extract Email ----------

    @Test
    void extractEmail_fromAccessToken_returnsEmail() {
        String token = jwtServices.generateToken(EMAIL, "USER");

        assertEquals(EMAIL, jwtServices.extractEmail(token));
    }

    @Test
    void extractEmail_fromRefreshToken_returnsEmail() {
        String token = jwtServices.generateRefreshToken(EMAIL);

        assertEquals(EMAIL, jwtServices.extractEmail(token));
    }

    @Test
    void extractEmail_expiredToken_throwsExpiredJwtException() {
        String token = expiredToken(
                SECRET, "access", EMAIL, "role", "USER"
        );

        assertThrows(
                ExpiredJwtException.class,
                () -> jwtServices.extractEmail(token)
        );
    }

    // ---------- Device Access Tokens ----------

    @Test
    void generateDeviceAccessToken_isValidDeviceAccessToken() {
        UUID robotId = UUID.randomUUID();

        String token = jwtServices.generateDeviceAccessToken(robotId);

        assertTrue(jwtServices.isValidDeviceAccessToken(token));
        assertFalse(jwtServices.isValidDeviceRefreshToken(token));
    }

    @Test
    void generateDeviceAccessToken_containsRobotIdAndType() {
        UUID robotId = UUID.randomUUID();

        String token = jwtServices.generateDeviceAccessToken(robotId);

        Claims claims = parseClaims(token);

        assertEquals(robotId.toString(), claims.getSubject());
        assertEquals(
                robotId.toString(),
                claims.get("robotId", String.class)
        );
        assertEquals(
                "device_access",
                claims.get("type", String.class)
        );
    }

    @Test
    void generateDeviceAccessToken_expiresInTenMinutes() {
        String token = jwtServices.generateDeviceAccessToken(
                UUID.randomUUID()
        );

        Claims claims = parseClaims(token);

        long lifetime = claims.getExpiration().getTime()
                - claims.getIssuedAt().getTime();

        assertEquals(TEN_MINUTES, lifetime);
    }

    // ---------- Device Refresh Tokens ----------

    @Test
    void generateDeviceRefreshToken_isValidDeviceRefreshToken() {
        UUID robotId = UUID.randomUUID();

        String token = jwtServices.generateDeviceRefreshToken(robotId);

        assertTrue(jwtServices.isValidDeviceRefreshToken(token));
        assertFalse(jwtServices.isValidDeviceAccessToken(token));
    }

    @Test
    void generateDeviceRefreshToken_containsRobotIdAndType() {
        UUID robotId = UUID.randomUUID();

        String token = jwtServices.generateDeviceRefreshToken(robotId);

        Claims claims = parseClaims(token);

        assertEquals(robotId.toString(), claims.getSubject());
        assertEquals(
                robotId.toString(),
                claims.get("robotId", String.class)
        );
        assertEquals(
                "device_refresh",
                claims.get("type", String.class)
        );
    }

    @Test
    void generateDeviceRefreshToken_expiresInNinetyDays() {
        String token = jwtServices.generateDeviceRefreshToken(
                UUID.randomUUID()
        );

        Claims claims = parseClaims(token);

        long lifetime = claims.getExpiration().getTime()
                - claims.getIssuedAt().getTime();

        assertEquals(NINETY_DAYS, lifetime);
    }

    // ---------- Extract Robot ID ----------

    @Test
    void extractRobotId_fromDeviceAccessToken_returnsRobotId() {
        UUID robotId = UUID.randomUUID();

        String token = jwtServices.generateDeviceAccessToken(robotId);

        assertEquals(robotId, jwtServices.extractRobotId(token));
    }

    @Test
    void extractRobotId_fromDeviceRefreshToken_returnsRobotId() {
        UUID robotId = UUID.randomUUID();

        String token = jwtServices.generateDeviceRefreshToken(robotId);

        assertEquals(robotId, jwtServices.extractRobotId(token));
    }

    @Test
    void extractRobotId_expiredToken_throwsExpiredJwtException() {
        UUID robotId = UUID.randomUUID();

        String token = expiredToken(
                SECRET,
                "device_access",
                robotId.toString(),
                "robotId",
                robotId.toString()
        );

        assertThrows(
                ExpiredJwtException.class,
                () -> jwtServices.extractRobotId(token)
        );
    }

    // ---------- Rejecting Expired Tokens ----------

    @Test
    void expiredAccessToken_isNotValid() {
        String token = expiredToken(
                SECRET, "access", EMAIL, "role", "USER"
        );

        assertFalse(jwtServices.isValidAccessToken(token));
    }

    @Test
    void expiredRefreshToken_isNotValid() {
        String token = expiredToken(
                SECRET, "refresh", EMAIL, "type", "refresh"
        );

        assertFalse(jwtServices.isValidRefreshToken(token));
    }

    @Test
    void expiredDeviceAccessToken_isNotValid() {
        UUID robotId = UUID.randomUUID();

        String token = expiredToken(
                SECRET,
                "device_access",
                robotId.toString(),
                "robotId",
                robotId.toString()
        );

        assertFalse(jwtServices.isValidDeviceAccessToken(token));
    }

    @Test
    void expiredDeviceRefreshToken_isNotValid() {
        UUID robotId = UUID.randomUUID();

        String token = expiredToken(
                SECRET,
                "device_refresh",
                robotId.toString(),
                "robotId",
                robotId.toString()
        );

        assertFalse(jwtServices.isValidDeviceRefreshToken(token));
    }

    // ---------- Rejecting Wrong Secrets ----------

    @Test
    void tokenSignedWithDifferentSecret_isNotValid() {
        JwtServices otherService = new JwtServices(OTHER_SECRET);

        String access = otherService.generateToken(EMAIL, "ADMIN");
        String refresh = otherService.generateRefreshToken(EMAIL);

        assertFalse(jwtServices.isValidAccessToken(access));
        assertFalse(jwtServices.isValidRefreshToken(refresh));
    }

    @Test
    void deviceTokenSignedWithDifferentSecret_isNotValid() {
        JwtServices otherService = new JwtServices(OTHER_SECRET);

        UUID robotId = UUID.randomUUID();

        String access = otherService.generateDeviceAccessToken(robotId);
        String refresh = otherService.generateDeviceRefreshToken(robotId);

        assertFalse(jwtServices.isValidDeviceAccessToken(access));
        assertFalse(jwtServices.isValidDeviceRefreshToken(refresh));
    }

    // ---------- Rejecting Tampered Tokens ----------

    @Test
    void tokenWithTamperedPayload_isNotValid() {
        String admin = jwtServices.generateToken(EMAIL, "ADMIN");
        String user = jwtServices.generateToken(EMAIL, "USER");

        String[] adminParts = admin.split("\\.");
        String[] userParts = user.split("\\.");

        String tampered = adminParts[0] + "."
                + adminParts[1] + "."
                + userParts[2];

        assertFalse(jwtServices.isValidAccessToken(tampered));
    }

    @Test
    void deviceTokenWithTamperedPayload_isNotValid() {
        UUID robot1 = UUID.randomUUID();
        UUID robot2 = UUID.randomUUID();

        String token1 = jwtServices.generateDeviceAccessToken(robot1);
        String token2 = jwtServices.generateDeviceAccessToken(robot2);

        String[] parts1 = token1.split("\\.");
        String[] parts2 = token2.split("\\.");

        String tampered = parts1[0] + "."
                + parts2[1] + "."
                + parts1[2];

        assertFalse(jwtServices.isValidDeviceAccessToken(tampered));
    }

    // ---------- Rejecting Wrong Token Types ----------

    @Test
    void userTokens_areNotValidDeviceTokens() {
        String access = jwtServices.generateToken(EMAIL, "USER");
        String refresh = jwtServices.generateRefreshToken(EMAIL);

        assertFalse(jwtServices.isValidDeviceAccessToken(access));
        assertFalse(jwtServices.isValidDeviceRefreshToken(access));

        assertFalse(jwtServices.isValidDeviceAccessToken(refresh));
        assertFalse(jwtServices.isValidDeviceRefreshToken(refresh));
    }

    @Test
    void deviceTokens_areNotValidUserTokens() {
        UUID robotId = UUID.randomUUID();

        String access = jwtServices.generateDeviceAccessToken(robotId);
        String refresh = jwtServices.generateDeviceRefreshToken(robotId);

        assertFalse(jwtServices.isValidAccessToken(access));
        assertFalse(jwtServices.isValidRefreshToken(access));

        assertFalse(jwtServices.isValidAccessToken(refresh));
        assertFalse(jwtServices.isValidRefreshToken(refresh));
    }

    // ---------- Garbage and Null Tokens ----------

    @Test
    void garbageToken_isNotValid() {
        assertFalse(jwtServices.isValidAccessToken("not.a.jwt"));
        assertFalse(jwtServices.isValidRefreshToken("not.a.jwt"));

        assertFalse(jwtServices.isValidDeviceAccessToken("not.a.jwt"));
        assertFalse(jwtServices.isValidDeviceRefreshToken("not.a.jwt"));

        assertFalse(jwtServices.isValidAccessToken("garbage"));
    }

    @Test
    void nullOrEmptyToken_isNotValid() {
        assertFalse(jwtServices.isValidAccessToken(null));
        assertFalse(jwtServices.isValidRefreshToken(null));

        assertFalse(jwtServices.isValidDeviceAccessToken(null));
        assertFalse(jwtServices.isValidDeviceRefreshToken(null));

        assertFalse(jwtServices.isValidAccessToken(""));
        assertFalse(jwtServices.isValidRefreshToken(""));

        assertFalse(jwtServices.isValidDeviceAccessToken(""));
        assertFalse(jwtServices.isValidDeviceRefreshToken(""));
    }

    // ---------- Expiration Seconds ----------

    @Test
    void getAccessExpirationSeconds_returnsSixHundred() {
        assertEquals(600, jwtServices.getAccessExpirationSeconds());
    }

    @Test
    void getDeviceAccessExpirationSeconds_returnsSixHundred() {
        assertEquals(
                600,
                jwtServices.getDeviceAccessExpirationSeconds()
        );
    }
}

