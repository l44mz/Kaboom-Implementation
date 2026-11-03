package com.example.kaboom.services;

import com.example.kaboom.entities.FocusSession;
import com.example.kaboom.entities.StateLog;
import com.example.kaboom.repositories.FocusSessionRepository;
import com.example.kaboom.repositories.StateLogRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class StateLogService {

    private final StateLogRepository stateLogRepository;
    private final FocusSessionRepository focusSessionRepository;

    public StateLogService(
            StateLogRepository stateLogRepository,
            FocusSessionRepository focusSessionRepository
    ) {
        this.stateLogRepository = stateLogRepository;
        this.focusSessionRepository = focusSessionRepository;
    }

    /**
     * Records a new robot state for a focus session.
     * The previous state log is closed before the new one starts.
     */
    @Transactional
    public StateLog recordState(UUID sessionId, StateLog.State newState) {

        FocusSession session = getSession(sessionId);

        if (session.getStatus() != FocusSession.SessionStatus.active) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "States can only be recorded during an active session"
            );
        }

        StateLog currentLog = stateLogRepository
                .findFirstByFocusSession_IdAndEndedAtIsNullOrderByStartedAtDesc(
                        sessionId
                )
                .orElse(null);

        // Avoid creating duplicate consecutive logs for the same state.
        if (currentLog != null && currentLog.getState() == newState) {
            return currentLog;
        }

        LocalDateTime now = LocalDateTime.now();

        if (currentLog != null) {
            currentLog.setEndedAt(now);
            stateLogRepository.save(currentLog);
        }

        StateLog newLog = new StateLog(session, newState);
        newLog.setStartedAt(now);

        return stateLogRepository.save(newLog);
    }

    /**
     * Closes the currently active state log.
     * This is used when a session is paused or ended.
     */
    @Transactional
    public void closeCurrentState(UUID sessionId) {

        StateLog currentLog = stateLogRepository
                .findFirstByFocusSession_IdAndEndedAtIsNullOrderByStartedAtDesc(
                        sessionId
                )
                .orElse(null);

        if (currentLog != null) {
            currentLog.endLog();
            stateLogRepository.save(currentLog);
        }
    }

    /**
     * Calculates the actual focused time in seconds.
     * Only logs with the focused state are included.
     */
    @Transactional(readOnly = true)
    public long calculateFocusedSeconds(UUID sessionId) {

        LocalDateTime now = LocalDateTime.now();

        return stateLogRepository
                .findByFocusSession_IdOrderByStartedAtAsc(sessionId)
                .stream()
                .filter(log -> log.getState() == StateLog.State.focused)
                .mapToLong(log -> {
                    LocalDateTime end = log.getEndedAt() != null
                            ? log.getEndedAt()
                            : now;

                    return Math.max(
                            0,
                            Duration.between(log.getStartedAt(), end).getSeconds()
                    );
                })
                .sum();
    }

    /**
     * Counts how many distracted state periods occurred in the session.
     */
    @Transactional(readOnly = true)
    public long countDistractions(UUID sessionId) {
        return stateLogRepository.countByFocusSession_IdAndState(
                sessionId,
                StateLog.State.distracted
        );
    }

    private FocusSession getSession(UUID sessionId) {
        return focusSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Session not found"
                ));
    }
}