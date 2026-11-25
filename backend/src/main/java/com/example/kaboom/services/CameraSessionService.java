package com.example.kaboom.services;

import com.example.kaboom.dtos.CameraSessionEndResponse;
import com.example.kaboom.dtos.CameraSessionRequest;
import com.example.kaboom.dtos.CameraSessionResponse;
import com.example.kaboom.dtos.CameraStateRequest;
import com.example.kaboom.dtos.CameraStateResponse;
import com.example.kaboom.entities.FocusSession;
import com.example.kaboom.entities.User;
import com.example.kaboom.enums.FocusState;
import com.example.kaboom.enums.SessionSource;
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
public class CameraSessionService {

    private static final int MAX_REWARD = 30;
    private static final int MAX_GAP_SECONDS = 30;

    private final FocusSessionRepository focusSessionRepository;
    private final UserRepository userRepository;

    public CameraSessionService(
            FocusSessionRepository focusSessionRepository,
            UserRepository userRepository) {
        this.focusSessionRepository = focusSessionRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CameraSessionResponse startSession(
            UUID userId,
            CameraSessionRequest request) {

        focusSessionRepository.findActiveOrPausedSessionByUserId(userId)
                .ifPresent(session -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "User already has an active or paused session");
                });

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        // Camera sessions are never attached to a robot.
        FocusSession session = new FocusSession(
                user, null, request.plannedDurationSeconds());

        session.setSource(SessionSource.CAMERA);
        session.setStartTime(LocalDateTime.now());
        session.setStatus(FocusSession.SessionStatus.active);

        return toResponse(focusSessionRepository.save(session));
    }

    @Transactional(readOnly = true)
    public CameraSessionResponse getSession(UUID userId, UUID sessionId) {
        return toResponse(getOwnedCameraSession(userId, sessionId));
    }

    @Transactional
    public CameraSessionResponse pauseSession(UUID userId, UUID sessionId) {
        FocusSession session = getOwnedCameraSession(userId, sessionId);

        if (session.getStatus() != FocusSession.SessionStatus.active) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Session is not active");
        }

        LocalDateTime now = LocalDateTime.now();

        accrueTime(session, now);
        session.setLastEventAt(null);
        session.setPauseStartedAt(now);
        session.setStatus(FocusSession.SessionStatus.paused);

        return toResponse(focusSessionRepository.save(session));
    }

    @Transactional
    public CameraSessionResponse resumeSession(UUID userId, UUID sessionId) {
        FocusSession session = getOwnedCameraSession(userId, sessionId);

        if (session.getStatus() != FocusSession.SessionStatus.paused) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Session is not paused");
        }

        LocalDateTime now = LocalDateTime.now();

        if (session.getPauseStartedAt() != null) {
            long paused = Duration.between(
                    session.getPauseStartedAt(), now).getSeconds();

            session.setPausedSeconds(
                    session.getPausedSeconds() + (int) Math.max(0, paused));
        }

        session.setPauseStartedAt(null);
        session.setLastEventAt(null);
        session.setStatus(FocusSession.SessionStatus.active);

        return toResponse(focusSessionRepository.save(session));
    }

    @Transactional
    public CameraStateResponse recordState(
            UUID userId,
            UUID sessionId,
            CameraStateRequest request) {

        if (!sessionId.equals(request.sessionId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Session ID does not match");
        }

        FocusSession session = focusSessionRepository
                .findByIdForUpdate(sessionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Session not found"));

        verifyOwnershipAndSource(session, userId);

        if (session.getStatus() != FocusSession.SessionStatus.active) {
            return new CameraStateResponse(
                    "Session is not active",
                    session.getCurrentState(),
                    session.getLastSequenceNumber(),
                    session.isExplosionTriggered());
        }

        if (request.sequenceNumber() <= session.getLastSequenceNumber()) {
            return new CameraStateResponse(
                    "Ignored stale report",
                    session.getCurrentState(),
                    session.getLastSequenceNumber(),
                    session.isExplosionTriggered());
        }

        LocalDateTime now = LocalDateTime.now();

        // Start actual camera timing from the first observation after calibration.
        boolean firstObservation = session.getCurrentState() == null
                && session.getLastEventAt() == null;

        if (firstObservation) {
            session.setStartTime(now);
        }

        // Credit elapsed time to the previous state first.
        accrueTime(session, now);

        FocusState previousState = session.getCurrentState();
        FocusState newState = request.state();

        if (previousState != newState) {
            if (newState == FocusState.DISTRACTED) {
                session.setDistractionCount(
                        session.getDistractionCount() + 1);
            } else if (newState == FocusState.AWAY) {
                session.setAwayCount(session.getAwayCount() + 1);
            }
        }

        // Kaboom: the same rule as robot sessions (FocusSessionService).
        // Away twice or distracted four times makes Sparky explode, once per session.
        if (!session.isExplosionTriggered()
                && (session.getAwayCount() >= 2
                || session.getDistractionCount() >= 4)) {
            session.setExplosionTriggered(true);
        }

        session.setCurrentState(newState);
        session.setLastEventAt(now);
        session.setLastSequenceNumber(request.sequenceNumber());

        focusSessionRepository.save(session);

        return new CameraStateResponse(
                "Camera state recorded",
                newState,
                session.getLastSequenceNumber(),
                session.isExplosionTriggered());
    }

    @Transactional
    public CameraSessionEndResponse endSession(
            UUID userId, UUID sessionId) {

        FocusSession session = focusSessionRepository
                .findByIdForUpdate(sessionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Session not found"));

        verifyOwnershipAndSource(session, userId);

        if (session.getStatus() == FocusSession.SessionStatus.completed) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Session is already completed");
        }

        LocalDateTime now = LocalDateTime.now();

        if (session.getStatus() == FocusSession.SessionStatus.active) {
            accrueTime(session, now);
        }

        if (session.getStatus() == FocusSession.SessionStatus.paused
                && session.getPauseStartedAt() != null) {

            long paused = Duration.between(
                    session.getPauseStartedAt(), now).getSeconds();

            session.setPausedSeconds(
                    session.getPausedSeconds() + (int) Math.max(0, paused));
        }

        session.setEndTime(now);

        int totalDuration = (int) Math.max(
                0, Duration.between(session.getStartTime(), now).getSeconds());

        session.setDurationSeconds(totalDuration);

        int activeDuration = Math.max(
                0, totalDuration - session.getPausedSeconds());

        int focusedSeconds = Math.min(
                activeDuration,
                Math.max(0, session.getFocusedSeconds()));

        session.setFocusedSeconds(focusedSeconds);

        float percentage = activeDuration > 0
                ? (focusedSeconds * 100f) / activeDuration
                : 0f;

        session.setFocusedPercentage(percentage);

        int reward = Math.min(MAX_REWARD, focusedSeconds / 60);
        session.setRewardEarned(reward);

        session.setPauseStartedAt(null);
        session.setLastEventAt(null);
        session.setStatus(FocusSession.SessionStatus.completed);

        User user = session.getUser();
        int updatedCoins = user.getCoins() + reward;
        user.setCoins(updatedCoins);

        focusSessionRepository.save(session);
        userRepository.save(user);

        return new CameraSessionEndResponse(
                session.getId(),
                session.getStatus().name(),
                session.getDurationSeconds(),
                session.getFocusedSeconds(),
                session.getFocusedPercentage(),
                session.getRewardEarned(),
                updatedCoins);
    }

    private void accrueTime(FocusSession session, LocalDateTime now) {
        if (session.getCurrentState() == null
                || session.getLastEventAt() == null) {
            return;
        }

        long elapsed = Duration.between(
                session.getLastEventAt(), now).getSeconds();

        int seconds = (int) Math.min(
                Math.max(elapsed, 0), MAX_GAP_SECONDS);

        switch (session.getCurrentState()) {
            case FOCUSED ->
                    session.setFocusedSeconds(
                            session.getFocusedSeconds() + seconds);
            case DISTRACTED ->
                    session.setDistractedSeconds(
                            session.getDistractedSeconds() + seconds);
            case AWAY ->
                    session.setAwaySeconds(
                            session.getAwaySeconds() + seconds);
        }

        session.setLastEventAt(now);
    }

    private FocusSession getOwnedCameraSession(UUID userId, UUID sessionId) {
        FocusSession session = focusSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Session not found"));

        verifyOwnershipAndSource(session, userId);
        return session;
    }

    private void verifyOwnershipAndSource(
            FocusSession session, UUID userId) {

        if (session.getUser() == null
                || !session.getUser().getId().equals(userId)
                || session.getSource() != SessionSource.CAMERA) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Camera session not found");
        }
    }

    private CameraSessionResponse toResponse(FocusSession session) {
        return new CameraSessionResponse(
                session.getId(),
                session.getPlannedDurationSeconds(),
                session.getStartTime(),
                session.getStatus().name(),
                session.getCurrentState(),
                session.getFocusedSeconds());
    }
}