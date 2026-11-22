package com.example.kaboom.dtos;

import com.example.kaboom.enums.FocusState;

public record CameraStateResponse(
        String message,
        FocusState currentState,
        Long sequenceNumber,
        boolean explosionTriggered) {}
