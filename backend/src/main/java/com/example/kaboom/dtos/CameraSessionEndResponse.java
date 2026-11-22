package com.example.kaboom.dtos;

import java.util.UUID;

public record CameraSessionEndResponse(
        UUID sessionId,
        String status,
        int durationSeconds,
        int focusedSeconds,
        float focusedPercentage,
        int rewardEarned,
        int updatedCoins
) {}
