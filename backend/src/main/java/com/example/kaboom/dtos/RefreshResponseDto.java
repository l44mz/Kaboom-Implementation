package com.example.kaboom.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RefreshResponseDto {
    private String accessToken;
    @Schema(example = "900")
    private long expiresInSeconds;
}
