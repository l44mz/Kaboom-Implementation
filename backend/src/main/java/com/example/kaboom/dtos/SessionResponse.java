package com.example.kaboom.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

public class SessionResponse {

    @Schema(description = "Unique ID of the focus session", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "ID of the associated robot", example = "123e4567-e89b-12d3-a456-426614174001")
    private UUID robotId;

    @Schema(description = "Planned duration in seconds", example = "1800")
    private Integer plannedDurationSeconds;

    @Schema(description = "Session start timestamp", example = "2026-10-07T14:00:00Z")
    private LocalDateTime startTime;

    @Schema(description = "Session current status", example = "active")
    private String status;

    public SessionResponse() {}

    public SessionResponse(UUID id, UUID robotId, Integer plannedDurationSeconds, LocalDateTime startTime, String status) {
        this.id = id;
        this.robotId = robotId;
        this.plannedDurationSeconds = plannedDurationSeconds;
        this.startTime = startTime;
        this.status = status;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getRobotId() { return robotId; }
    public void setRobotId(UUID robotId) { this.robotId = robotId; }

    public Integer getPlannedDurationSeconds() { return plannedDurationSeconds; }
    public void setPlannedDurationSeconds(Integer plannedDurationSeconds) { this.plannedDurationSeconds = plannedDurationSeconds; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}