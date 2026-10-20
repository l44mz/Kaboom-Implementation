
package com.example.kaboom.services;

import com.example.kaboom.dtos.CreateSessionRequest;
import com.example.kaboom.dtos.EndSessionResponse;
import com.example.kaboom.dtos.SessionResponse;
import com.example.kaboom.entities.FocusSession;
import com.example.kaboom.entities.Robot;
import com.example.kaboom.entities.User;
import com.example.kaboom.repositories.FocusSessionRepository;
import com.example.kaboom.repositories.RobotRepository;
import com.example.kaboom.repositories.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Manages focus session lifecycle, timing, and rewards.
 */
@Service
public class FocusSessionService {

    private static final int MAX_REWARD = 30;

    private final FocusSessionRepository focusSessionRepository;
    private final UserRepository userRepository;
    private final RobotRepository robotRepository;

    public FocusSessionService(
            FocusSessionRepository focusSessionRepository,
            UserRepository userRepository,
            RobotRepository robotRepository) {
        this.focusSessionRepository = focusSessionRepository;
        this.userRepository = userRepository;
        this.robotRepository = robotRepository;
    }

    /**
     * Starts a new focus session for a user.
     */
    @Transactional
    public SessionResponse startSession(
            UUID userId,
            CreateSessionRequest request) {

        focusSessionRepository.findActiveOrPausedSessionByUserId(userId)
                .ifPresent(session -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "User already has an active or paused session");
                });

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        Robot robot = null;

        if (request.getRobotId() != null) {
            robot = robotRepository.findById(request.getRobotId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Robot not found"));

            if (robot.getUser() == null
                    || !robot.getUser().getId().equals(userId)) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Robot not found");
            }
        }

        FocusSession session = new FocusSession(
                user, robot, request.getPlannedDurationSeconds());

        session.setStartTime(LocalDateTime.now());
        session.setStatus(FocusSession.SessionStatus.active);

        FocusSession savedSession = focusSessionRepository.save(session);

        return toSessionResponse(savedSession);
    }

    /**
     * Pauses an active session and records the pause start time.
     */
    @Transactional
    public SessionResponse pauseSession(UUID userId, UUID sessionId) {
        FocusSession session = getOwnedSession(userId, sessionId);

        if (session.getStatus() != FocusSession.SessionStatus.active) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only an active session can be paused");
        }

        session.setPauseStartedAt(LocalDateTime.now());
        session.setStatus(FocusSession.SessionStatus.paused);

        return toSessionResponse(focusSessionRepository.save(session));
    }

    /**
     * Resumes a paused session and accumulates its pause duration.
     */
    @Transactional
    public SessionResponse resumeSession(UUID userId, UUID sessionId) {
        FocusSession session = getOwnedSession(userId, sessionId);

        if (session.getStatus() != FocusSession.SessionStatus.paused) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only a paused session can be resumed");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime pauseStartedAt = session.getPauseStartedAt();

        if (pauseStartedAt != null) {
            long pauseDuration = Duration.between(
                    pauseStartedAt, now).getSeconds();

            session.setPausedSeconds(
                    session.getPausedSeconds()
                            + (int) Math.max(0, pauseDuration));
        }

        session.setPauseStartedAt(null);
        session.setStatus(FocusSession.SessionStatus.active);

        return toSessionResponse(focusSessionRepository.save(session));
    }

    /**
     * Ends an active or paused session and updates the user's coin balance.
     *
     * Focus time must be populated from recorded state intervals.
     */
    @Transactional
    public EndSessionResponse endSession(UUID userId, UUID sessionId) {
        FocusSession session = getOwnedSession(userId, sessionId);

        if (session.getStatus() == FocusSession.SessionStatus.completed) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Session is already completed");
        }

        LocalDateTime now = LocalDateTime.now();

        // Include the current pause if the session is still paused.
        if (session.getStatus() == FocusSession.SessionStatus.paused
                && session.getPauseStartedAt() != null) {

            long pauseDuration = Duration.between(
                    session.getPauseStartedAt(), now).getSeconds();

            session.setPausedSeconds(
                    session.getPausedSeconds()
                            + (int) Math.max(0, pauseDuration));
        }

        session.setEndTime(now);

        int totalDuration = (int) Math.max(
                0,
                Duration.between(session.getStartTime(), now).getSeconds());

        session.setDurationSeconds(totalDuration);

        int activeDuration = Math.max(
                0, totalDuration - session.getPausedSeconds());

        // Focus time must come from state logs.
        // Until state logging is implemented, the stored value may be zero.
        int focusedSeconds = Math.min(
                activeDuration,
                Math.max(0, session.getFocusedSeconds()));

        session.setFocusedSeconds(focusedSeconds);

        float focusedPercentage = activeDuration > 0
                ? (focusedSeconds * 100f) / activeDuration
                : 0f;

        session.setFocusedPercentage(focusedPercentage);

        int reward = Math.min(MAX_REWARD, focusedSeconds / 60);
        session.setRewardEarned(reward);

        session.setPauseStartedAt(null);
        session.setStatus(FocusSession.SessionStatus.completed);

        User user = session.getUser();

        int updatedCoins = user.getCoins() + reward;
        user.setCoins(updatedCoins);

        focusSessionRepository.save(session);
        userRepository.save(user);

        return new EndSessionResponse(
                session.getId(),
                session.getStatus().name(),
                session.getDurationSeconds(),
                session.getFocusedPercentage(),
                session.getRewardEarned(),
                updatedCoins
        );
    }

    private FocusSession getOwnedSession(UUID userId, UUID sessionId) {
        FocusSession session = focusSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Session not found"));

        if (session.getUser() == null
                || !session.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Session not found");
        }

        return session;
    }

    private SessionResponse toSessionResponse(FocusSession session) {
        UUID robotId = session.getRobot() != null
                ? session.getRobot().getId()
                : null;

        return new SessionResponse(
                session.getId(),
                robotId,
                session.getPlannedDurationSeconds(),
                session.getStartTime(),
                session.getStatus().name()
        );
    }
}
