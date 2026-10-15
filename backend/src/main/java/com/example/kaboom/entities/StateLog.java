package com.example.kaboom.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "state_logs")
@NoArgsConstructor
@Getter
@Setter
public class StateLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "session_id", nullable = false)
    private FocusSession focusSession;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private State state;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    public enum State {
        focused, distracted, away
    }

    public StateLog(FocusSession focusSession, State state) {
        this.focusSession = focusSession;
        this.state = state;
        this.startedAt = LocalDateTime.now();
    }

    public void endLog() {
        this.endedAt = LocalDateTime.now();
    }
}