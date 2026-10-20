
package com.example.kaboom.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Represents the response returned after successfully refreshing
 * an access token using a valid refresh token.
 */
@Getter
@AllArgsConstructor
@Schema(description = "Response containing the newly issued access token and its expiry duration.")
public class RefreshResponseDto {

    @Schema(
            description = "New access token used to authenticate API requests",
            example = "eyJhbGciOiJIUzI1NiJ9.example.signature"
    )
    private String accessToken;

    @Schema(
            description = "Access token validity duration in seconds",
            example = "900"
    )
    private long expiresInSeconds;
}
