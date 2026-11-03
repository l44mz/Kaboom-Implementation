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

@Service
public class FocusSessionService {

    private final FocusSessionRepository focusSessionRepository;
    private final UserRepository userRepository;
    private final RobotRepository robotRepository;
    private final StateLogService stateLogService;

    public FocusSessionService(
            FocusSessionRepository focusSessionRepository,
            UserRepository userRepository,
            RobotRepository robotRepository,
            StateLogService stateLogService
    ) {
        this.focusSessionRepository = focusSessionRepository;
        this.userRepository = userRepository;
        this.robotRepository = robotRepository;
        this.stateLogService = stateLogService;
    }

    /**
     * Starts a new focus session for a user.
     */
    @Transactional
    public SessionResponse startSession(UUID userId, CreateSessionRequest request) {

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
     * Pauses an active session and closes its current robot state log.
     */
    @Transactional
    public SessionResponse pauseSession(UUID userId, UUID sessionId) {
        FocusSession session = getOwnedSession(userId, sessionId);

        if (session.getStatus() != FocusSession.SessionStatus.active) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only an active session can be paused");
        }

        stateLogService.closeCurrentState(sessionId);

        session.setPauseStartedAt(LocalDateTime.now());
        session.setStatus(FocusSession.SessionStatus.paused);

        return toSessionResponse(focusSessionRepository.save(session));
    }

    /**
     * Resumes a paused session.
     * The next robot state must be recorded when it arrives.
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
     * Ends a session, closes its last state log,
     * calculates actual focused time, and awards coins exactly once.
     */
    @Transactional
    public EndSessionResponse endSession(UUID userId, UUID sessionId) {

        FocusSession session = focusSessionRepository.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Session not found"));

        if (session.getUser() == null
                || !session.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Session not found");
        }

        if (session.getStatus() == FocusSession.SessionStatus.completed) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Session is already completed");
        }

        LocalDateTime now = LocalDateTime.now();

        // Close the current robot state before calculating focus time.
        stateLogService.closeCurrentState(sessionId);

        // Include the current pause duration if the session is still paused.
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

        // Active duration excludes paused time.
        int activeDuration = Math.max(
                0, totalDuration - session.getPausedSeconds());

        // Only focused state logs count as actual focus.
        int focusedSeconds = (int) Math.min(
                activeDuration,
                stateLogService.calculateFocusedSeconds(sessionId));

        session.setFocusedSeconds(focusedSeconds);

        float focusedPercentage = activeDuration > 0
                ? (focusedSeconds * 100f) / activeDuration
                : 0f;

        session.setFocusedPercentage(focusedPercentage);

        // One coin for every completed minute of active session time.
        // Rewards are independent of focused/distracted/away states.
        int reward = activeDuration / 60;

        session.setRewardEarned(reward);
        session.setPauseStartedAt(null);
        session.setStatus(FocusSession.SessionStatus.completed);

        User user = session.getUser();

        if (reward > 0) {
            user.addReward(reward);
        }

        int updatedCoins = user.getCoins();

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