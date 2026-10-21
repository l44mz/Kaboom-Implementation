

package com.example.kaboom.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Represents a request to create a focus session.
 */
@Schema(description = "Request payload for starting a focus session")
public class CreateSessionRequest {

    @Schema(
            description = "UUID of the paired robot (optional)",
            example = "123e4567-e89b-12d3-a456-426614174000"
    )
    private UUID robotId;

    @NotNull(message = "Planned duration is required")
    @Min(value = 600, message = "Planned duration must be at least 60 seconds")
    @Schema(
            description = "Planned session duration in seconds",
            example = "1800",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer plannedDurationSeconds;

    public CreateSessionRequest() {}

    public UUID getRobotId() {
        return robotId;
    }

    public void setRobotId(UUID robotId) {
        this.robotId = robotId;
    }

    public Integer getPlannedDurationSeconds() {
        return plannedDurationSeconds;
    }

    public void setPlannedDurationSeconds(Integer plannedDurationSeconds) {
        this.plannedDurationSeconds = plannedDurationSeconds;
    }
}
