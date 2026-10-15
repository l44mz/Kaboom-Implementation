package com.example.kaboom.services;

import com.example.kaboom.dtos.CreateSessionRequest;
import com.example.kaboom.dtos.EndSessionResponse;
import com.example.kaboom.dtos.SessionResponse;
import com.example.kaboom.entities.FocusSession;
import com.example.kaboom.entities.Robot;
import com.example.kaboom.entities.User;
import com.example.kaboom.repositories.FocusSessionRepository;
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

    public FocusSessionService(FocusSessionRepository focusSessionRepository,
                               UserRepository userRepository) {
        this.focusSessionRepository = focusSessionRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public SessionResponse startSession(UUID userId, CreateSessionRequest request) {
        focusSessionRepository.findActiveOrPausedSessionByUserId(userId)
                .ifPresent(session -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "User already has an active or paused session");
                });

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Robot robot = null;
        if (request.getRobotId() != null) {
            robot = new Robot();
            try {
                robot.setId(UUID.fromString(request.getRobotId().toString()));
            } catch (Exception e) {
                // Fallback
            }
        }

        FocusSession session = new FocusSession(user, robot, request.getPlannedDurationSeconds());
        session.startSession();

        FocusSession savedSession = focusSessionRepository.save(session);
        UUID robotId = (savedSession.getRobot() != null) ? savedSession.getRobot().getId() : null;

        return new SessionResponse(
                savedSession.getId(),
                robotId,
                savedSession.getPlannedDurationSeconds(),
                savedSession.getStartTime(),
                savedSession.getStatus().name()
        );
    }

    @Transactional
    public SessionResponse pauseSession(UUID userId, UUID sessionId) {
        FocusSession session = focusSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        if (session.getUser() == null || !session.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found");
        }

        if (session.getStatus() != FocusSession.SessionStatus.active) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only an active session can be paused");
        }

        session.pauseSession();
        focusSessionRepository.save(session);
        UUID robotId = (session.getRobot() != null) ? session.getRobot().getId() : null;

        return new SessionResponse(
                session.getId(),
                robotId,
                session.getPlannedDurationSeconds(),
                session.getStartTime(),
                session.getStatus().name()
        );
    }

    @Transactional
    public SessionResponse resumeSession(UUID userId, UUID sessionId) {
        FocusSession session = focusSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        if (session.getUser() == null || !session.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found");
        }

        if (session.getStatus() != FocusSession.SessionStatus.paused) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only a paused session can be resumed");
        }

        session.continueSession();
        focusSessionRepository.save(session);
        UUID robotId = (session.getRobot() != null) ? session.getRobot().getId() : null;

        return new SessionResponse(
                session.getId(),
                robotId,
                session.getPlannedDurationSeconds(),
                session.getStartTime(),
                session.getStatus().name()
        );
    }

    @Transactional
    public EndSessionResponse endSession(UUID userId, UUID sessionId) {
        FocusSession session = focusSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        if (session.getUser() == null || !session.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found");
        }

        if (session.getStatus() == FocusSession.SessionStatus.completed) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Session is already completed");
        }

        LocalDateTime now = LocalDateTime.now();
        int totalDuration = (int) Duration.between(session.getStartTime(), now).getSeconds();
        int focusedSeconds = Math.max(0, totalDuration - session.getPausedSeconds());

        session.endSession(focusedSeconds, session.getPausedSeconds());
        focusSessionRepository.save(session);

        User user = session.getUser();
        int currentCoins = user.getCoins();
        int updatedCoins = currentCoins + session.getRewardEarned();
        user.setCoins(updatedCoins);
        userRepository.save(user);

        return new EndSessionResponse(
                session.getId(),
                session.getStatus().name(),
                session.getDurationSeconds(),
                session.getFocusedPercentage() != null ? session.getFocusedPercentage() : 0f,
                session.getRewardEarned(),
                updatedCoins
        );
    }
}