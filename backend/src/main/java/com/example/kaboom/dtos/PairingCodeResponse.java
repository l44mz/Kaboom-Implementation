package com.example.kaboom.dtos;

import java.time.LocalDateTime;

public record PairingCodeResponse(
        String pairingCode,
        String pairingSecret,
        LocalDateTime expiresAt) {}
