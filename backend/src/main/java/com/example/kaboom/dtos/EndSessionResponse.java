
package com.example.kaboom.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Represents the response returned after ending a focus session.
 * Contains the session results and the user's updated coin balance.
 */
@Schema(description = "Response containing the completed focus session summary and earned rewards")
public class EndSessionResponse {

    @Schema(
            description = "Unique ID of the focus session",
            example = "123e4567-e89b-12d3-a456-426614174000"
    )
    private UUID id;

    @Schema(description = "Session status", example = "completed")
    private String status;

    @Schema(description = "Total duration in seconds", example = "1800")
    private Integer durationSeconds;

    @Schema(description = "Percentage of active session time spent focused", example = "88.5")
    private Float focusedPercentage;

    @Schema(description = "Reward earned in coins", example = "26")
    private Integer rewardEarned;

    @Schema(description = "Updated total user coins", example = "146")
    private Integer totalCoins;

    public EndSessionResponse() {
    }

    public EndSessionResponse(
            UUID id,
            String status,
            Integer durationSeconds,
            Float focusedPercentage,
            Integer rewardEarned,
            Integer totalCoins) {
        this.id = id;
        this.status = status;
        this.durationSeconds = durationSeconds;
        this.focusedPercentage = focusedPercentage;
        this.rewardEarned = rewardEarned;
        this.totalCoins = totalCoins;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Float getFocusedPercentage() {
        return focusedPercentage;
    }

    public void setFocusedPercentage(Float focusedPercentage) {
        this.focusedPercentage = focusedPercentage;
    }

    public Integer getRewardEarned() {
        return rewardEarned;
    }

    public void setRewardEarned(Integer rewardEarned) {
        this.rewardEarned = rewardEarned;
    }

    public Integer getTotalCoins() {
        return totalCoins;
    }

    public void setTotalCoins(Integer totalCoins) {
        this.totalCoins = totalCoins;
    }
}
