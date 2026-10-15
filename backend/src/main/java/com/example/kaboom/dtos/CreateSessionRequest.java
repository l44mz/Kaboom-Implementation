package com.example.kaboom.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class CreateSessionRequest {

    @Schema(description = "ID of the paired robot (optional)", example = "1")
    private Long robotId;

    @NotNull(message = "Planned duration is required")
    @Min(value = 60, message = "Planned duration must be at least 60 seconds")
    @Schema(description = "Planned session duration in seconds", example = "1800", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer plannedDurationSeconds;

    public CreateSessionRequest() {}

    public Long getRobotId() { return robotId; }
    public void setRobotId(Long robotId) { this.robotId = robotId; }

    public Integer getPlannedDurationSeconds() { return plannedDurationSeconds; }
    public void setPlannedDurationSeconds(Integer plannedDurationSeconds) { this.plannedDurationSeconds = plannedDurationSeconds; }
}
