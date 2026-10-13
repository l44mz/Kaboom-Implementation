package com.example.kaboom.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "focus_sessions")
@NoArgsConstructor
@Getter
@Setter
public class FocusSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "robot_id")
    private Robot robot;

    @Column(name = "planned_duration_seconds")
    private int plannedDurationSeconds = 1800;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "duration_seconds")
    private int durationSeconds;

    @Column(name = "paused_seconds")
    private int pausedSeconds;

    @Column(name = "focused_seconds")
    private int focusedSeconds;

    @Column(name = "focused_percentage")
    private Float focusedPercentage;

    @Column(name = "reward_earned")
    private int rewardEarned;

    @Enumerated(EnumType.STRING)
    private SessionStatus status = SessionStatus.active;

    public enum SessionStatus {
        active, paused, completed
    }

    public FocusSession(User user, Robot robot, int plannedDurationSeconds) {
        this.user = user;
        this.robot = robot;
        this.plannedDurationSeconds = plannedDurationSeconds;
    }

    public void startSession() {
        this.startTime = LocalDateTime.now();
        this.status = SessionStatus.active;
    }

    public void pauseSession() {
        if (this.status == SessionStatus.active) {
            this.status = SessionStatus.paused;
        }
    }

    public void continueSession() {
        if (this.status == SessionStatus.paused) {
            this.status = SessionStatus.active;
        }
    }

    public void endSession(int actualFocusedSeconds, int totalPausedSeconds) {
        this.endTime = LocalDateTime.now();
        this.focusedSeconds = actualFocusedSeconds;
        this.pausedSeconds = totalPausedSeconds;

        if (this.startTime != null && this.endTime != null) {
            this.durationSeconds = (int) java.time.Duration.between(this.startTime, this.endTime).getSeconds();
        }

        if (this.durationSeconds > 0) {
            int activeTime = this.durationSeconds - this.pausedSeconds;
            if (activeTime > 0) {
                this.focusedPercentage = ((float) this.focusedSeconds / activeTime) * 100f;
            } else {
                this.focusedPercentage = 0f;
            }
        }

        this.rewardEarned = calculateReward();
        this.status = SessionStatus.completed;
    }

    public int calculateReward() {
        return this.focusedSeconds / 60;
    }

    public Map<String, Object> sessionSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("focusedSeconds", this.focusedSeconds);
        summary.put("rewardEarned", this.rewardEarned);
        summary.put("focusedPercentage", this.focusedPercentage);
        summary.put("durationSeconds", this.durationSeconds);
        return summary;
    }
}