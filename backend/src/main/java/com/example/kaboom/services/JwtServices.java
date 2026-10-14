package com.example.kaboom.services;

import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

@Service
public class JwtServices {

    private static final long ACCESS_MS = 1000L * 60 * 10;            // 10 minutes
    private static final long REFRESH_MS = 1000L * 60 * 60 * 24 * 7;  // 7 days

    private final SecretKey key;

    public JwtServices() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacSHA256");
        this.key = keyGenerator.generateKey();
    }

    public String generateToken(String username, String userRole) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(username)
                .claim("role", userRole)
                .issuedAt(new Date(now))
                .expiration(new Date(now + ACCESS_MS))
                .signWith(getKey())
                .compact();
    }

    public String generateRefreshToken(String username) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date(now))
                .expiration(new Date(now + REFRESH_MS))
                .signWith(getKey())
                .compact();
    }

    public long getAccessExpirationSeconds() {
        return ACCESS_MS / 1000;
    }

    private SecretKey getKey() {
        return key;
    }
}
