package com.example.kaboom.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

public class EndSessionResponse {

    @Schema(description = "Unique ID of the focus session", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "Session status", example = "completed")
    private String status;

    @Schema(description = "Total duration in seconds", example = "1800")
    private Integer durationSeconds;

    @Schema(description = "Focused percentage", example = "100.0")
    private Float focusedPercentage;

    @Schema(description = "Reward earned in coins", example = "15")
    private Integer rewardEarned;

    @Schema(description = "Updated total user coins", example = "125")
    private Integer totalCoins;

    public EndSessionResponse() {}

    public EndSessionResponse(UUID id, String status, Integer durationSeconds, Float focusedPercentage, Integer rewardEarned, Integer totalCoins) {
        this.id = id;
        this.status = status;
        this.durationSeconds = durationSeconds;
        this.focusedPercentage = focusedPercentage;
        this.rewardEarned = rewardEarned;
        this.totalCoins = totalCoins;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }

    public Float getFocusedPercentage() { return focusedPercentage; }
    public void setFocusedPercentage(Float focusedPercentage) { this.focusedPercentage = focusedPercentage; }

    public Integer getRewardEarned() { return rewardEarned; }
    public void setRewardEarned(Integer rewardEarned) { this.rewardEarned = rewardEarned; }

    public Integer getTotalCoins() { return totalCoins; }
    public void setTotalCoins(Integer totalCoins) { this.totalCoins = totalCoins; }
}