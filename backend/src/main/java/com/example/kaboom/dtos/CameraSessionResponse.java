package com.example.kaboom.dtos;

import com.example.kaboom.enums.FocusState;

import java.time.LocalDateTime;
import java.util.UUID;

public record CameraSessionResponse(
        UUID sessionId,
        int plannedDurationSeconds,
        LocalDateTime startTime,
        String status,
        FocusState currentState,
        int focusedSeconds
) {}
