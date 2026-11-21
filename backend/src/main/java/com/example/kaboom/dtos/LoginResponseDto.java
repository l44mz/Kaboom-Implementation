
package com.example.kaboom.dtos;

/**
 * Represents the response returned after a successful login.
 *
 * @param accessToken the JWT access token used to authenticate API requests
 * @param refreshToken the token used to obtain a new access token
 * @param tokenType the type of access token, typically "Bearer"
 * @param expiresInSeconds the access token lifetime in seconds
 * @param user the authenticated user's public information
 */
public record LoginResponseDto(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        UserDto user
) {
}
