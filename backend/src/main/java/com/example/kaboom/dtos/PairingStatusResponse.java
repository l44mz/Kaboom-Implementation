package com.example.kaboom.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PairingStatusResponse(
        String status,                 // "pending" or "paired"
        UUID robotId,
        String deviceAccessToken,
        String deviceRefreshToken,
        Long expiresInSeconds) {}
