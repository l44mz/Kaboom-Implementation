package com.example.kaboom.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefreshDto {
    @NotBlank
    @Schema(description = "Refresh token issued at login")
    private String refreshToken;
}
