package com.example.kaboom.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CameraSessionRequest(
        @NotNull
        @Min(value = 600, message = "Planned duration must be at least 600 seconds")
        Integer plannedDurationSeconds
) {}
