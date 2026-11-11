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

    // ----- Filled in by the robot's focus-state reports -----

    /** Last state the robot reported (null until the first report). */
    @Enumerated(EnumType.STRING)
    @Column(name = "current_state")
    private FocusState currentState;

    /** When the last report arrived (null while paused or before the first report). */
    @Column(name = "last_event_at")
    private LocalDateTime lastEventAt;

    /** Highest sequence number accepted, so old or repeated reports are ignored. */
    @Column(name = "last_sequence_number", nullable = false)
    private long lastSequenceNumber = 0;

    @Column(name = "distracted_seconds", nullable = false)
    private int distractedSeconds = 0;

    @Column(name = "away_seconds", nullable = false)
    private int awaySeconds = 0;

    /** How many times the robot saw the user become distracted (for History). */
    @Column(name = "distraction_count", nullable = false)
    private int distractionCount = 0;

    /** How many times the user entered the AWAY state during this session. */
    @Column(name = "away_count", nullable = false)
    private int awayCount = 0;

    /** Whether Sparky has exploded during this session. */
    @Column(name = "explosion_triggered", nullable = false)
    private boolean explosionTriggered = false;

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
