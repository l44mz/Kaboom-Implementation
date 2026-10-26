
package com.example.kaboom.entities;

import com.example.kaboom.enums.FocusState;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "focus_sessions")
@NoArgsConstructor
@Getter
@Setter
public class FocusSession {

    @Enumerated(EnumType.STRING)
    private FocusState currentState;       // last state reported by the robot

    private LocalDateTime lastEventAt;     // null while paused or before the first event
    private long lastSequenceNumber = 0;
    private int distractedSeconds = 0;
    private int awaySeconds = 0;

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

    @Column(name = "planned_duration_seconds", nullable = false)
    private int plannedDurationSeconds = 1800;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    @Column(name = "paused_seconds", nullable = false)
    private int pausedSeconds;

    @Column(name = "focused_seconds", nullable = false)
    private int focusedSeconds;

    @Column(name = "focused_percentage")
    private Float focusedPercentage;

    @Column(name = "reward_earned", nullable = false)
    private int rewardEarned;

    @Column(name = "pause_started_at")
    private LocalDateTime pauseStartedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status = SessionStatus.active;

    public enum SessionStatus {
        active,
        paused,
        completed
    }

    public FocusSession(User user, Robot robot, int plannedDurationSeconds) {
        this.user = user;
        this.robot = robot;
        this.plannedDurationSeconds = plannedDurationSeconds;
    }
}
