package com.example.kaboom.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtServices {

    private static final long ACCESS_MS = 1000L * 60 * 10;            // 10 minutes
    private static final long REFRESH_MS = 1000L * 60 * 60 * 24 * 7;  // 7 days
    private static final String TYPE = "type";

    //Robot Device Token
    private static final long DEVICE_ACCESS_MS = 1000L * 60 * 10;               // 10 minutes
    private static final long DEVICE_REFRESH_MS = 1000L * 60 * 60 * 24 * 90;    // 90 days
    private static final String ROBOT_ID = "robotId";

    private final SecretKey key;

    public JwtServices(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    public String generateToken(String username, String userRole) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(username)
                .claim("role", userRole)
                .claim(TYPE, "access")
                .issuedAt(new Date(now))
                .expiration(new Date(now + ACCESS_MS))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(String username) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(username)
                .claim(TYPE, "refresh")
                .issuedAt(new Date(now))
                .expiration(new Date(now + REFRESH_MS))
                .signWith(key)
                .compact();
    }

    public boolean isValidRefreshToken(String token) {
        return hasType(token, "refresh");
    }

    public boolean isValidAccessToken(String token) {
        return hasType(token, "access");
    }

    public String extractEmail(String token) {
        return parse(token).getSubject(); //to extract the email from token
    }

    public long getAccessExpirationSeconds() {
        return ACCESS_MS / 1000;
    }

    private boolean hasType(String token, String expected) {
        try {
            return expected.equals(parse(token).get(TYPE, String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();
    }

    //Robot Device Token

    public String generateDeviceAccessToken(UUID robotId) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(robotId.toString())
                .claim(ROBOT_ID, robotId.toString())
                .claim(TYPE, "device_access")
                .issuedAt(new Date(now))
                .expiration(new Date(now + DEVICE_ACCESS_MS))
                .signWith(key)
                .compact();
    }

    public String generateDeviceRefreshToken(UUID robotId) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(robotId.toString())
                .claim(ROBOT_ID, robotId.toString())
                .claim(TYPE, "device_refresh")
                .issuedAt(new Date(now))
                .expiration(new Date(now + DEVICE_REFRESH_MS))
                .signWith(key)
                .compact();
    }

    public boolean isValidDeviceAccessToken(String token) {
        return hasType(token, "device_access");
    }

    public boolean isValidDeviceRefreshToken(String token) {
        return hasType(token, "device_refresh");
    }

    public UUID extractRobotId(String token) {
        return UUID.fromString(parse(token).get(ROBOT_ID, String.class));
    }

    public long getDeviceAccessExpirationSeconds() {
        return DEVICE_ACCESS_MS / 1000;
    }

}
