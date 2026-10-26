package com.example.kaboom.dtos;

import com.example.kaboom.enums.FocusEventType;
import com.example.kaboom.enums.FocusState;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.UUID;

public record FocusStateRequest(
        @NotNull UUID sessionId,
        @NotNull FocusState state,
        @NotNull Long timestamp,
        @NotNull @PositiveOrZero Long sequenceNumber,
        @NotNull FocusEventType eventType) {}