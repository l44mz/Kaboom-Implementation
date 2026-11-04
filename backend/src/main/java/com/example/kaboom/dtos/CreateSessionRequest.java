package com.example.kaboom.dtos;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class CreateSessionRequest {

    @NotNull
    private UUID robotId;

    @NotNull
    private Integer plannedDurationSeconds;

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