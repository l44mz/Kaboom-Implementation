package com.example.kaboom.services;

import com.example.kaboom.dtos.CreateSessionRequest;
import com.example.kaboom.dtos.EndSessionResponse;
import com.example.kaboom.dtos.FocusStateRequest;
import com.example.kaboom.dtos.FocusStateResponse;
import com.example.kaboom.dtos.HistoryResponse;
import com.example.kaboom.dtos.SessionResponse;
import com.example.kaboom.entities.FocusSession;
import com.example.kaboom.entities.Robot;
import com.example.kaboom.entities.User;
import com.example.kaboom.enums.FocusState;
import com.example.kaboom.repositories.FocusSessionRepository;
import com.example.kaboom.repositories.RobotRepository;
import com.example.kaboom.repositories.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages focus session lifecycle, robot state reporting, timing, and rewards.
 */
@Service
public class FocusSessionService {

    private static final int MAX_REWARD = 30;

    /** Gaps between robot reports longer than this (e.g. a Wi-Fi drop) are not counted. */
    private static final long MAX_GAP_SECONDS = 30;

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
     * If the app sends no robotId, the user's own robot (if they have one) is attached,
     * so the robot can find the session.
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

        Robot robot;

        if (request.getRobotId() != null) {
            robot = robotRepository.findById(request.getRobotId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Robot not found"));

            if (robot.getUser() == null
                    || !robot.getUser().getId().equals(userId)) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Robot not found");
            }
        } else {
            // No robot chosen in the app: use the user's robot, or none.
            robot = robotRepository.findByUserId(userId)
                    .stream()
                    .findFirst()
                    .orElse(null);
        }

        FocusSession session = new FocusSession(
                user, robot, request.getPlannedDurationSeconds());

        session.setStartTime(LocalDateTime.now());
        session.setStatus(FocusSession.SessionStatus.active);

        FocusSession savedSession = focusSessionRepository.save(session);

        return toSessionResponse(savedSession);
    }

    /**
     * Returns one of the user's sessions, including the robot's latest state.
     * The app polls this during a session.
     */
    @Transactional(readOnly = true)
    public SessionResponse getSession(UUID userId, UUID sessionId) {
        return toSessionResponse(getOwnedSession(userId, sessionId));
    }

    /**
     * Pauses an active session and records the pause start time.
     * Time up to the pause is credited to the last reported state.
     */
    @Transactional
    public SessionResponse pauseSession(UUID userId, UUID sessionId) {
        FocusSession session = getOwnedSession(userId, sessionId);

        if (session.getStatus() != FocusSession.SessionStatus.active) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only an active session can be paused");
        }

        LocalDateTime now = LocalDateTime.now();

        accrueTime(session, now);
        session.setLastEventAt(null);   // nothing accrues while paused

        session.setPauseStartedAt(now);
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
        session.setLastEventAt(null);   // counting restarts at the next robot report
        session.setStatus(FocusSession.SessionStatus.active);

        return toSessionResponse(focusSessionRepository.save(session));
    }

    /**
     * Ends an active or paused session and updates the user's coin balance.
     * Focus time comes from the states reported by the robot.
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

        if (session.getStatus() == FocusSession.SessionStatus.active) {
            // Credit the last few seconds since the robot's final report.
            accrueTime(session, now);
        }

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
        session.setLastEventAt(null);
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

    /**
     * Returns the robot's current session (active or paused), if any.
     * The robot polls this every few seconds.
     */
    @Transactional
    public Optional<SessionResponse> getActiveSessionForRobot(UUID robotId) {
        // Remember that the robot is online, so the app can show "Connected".
        robotRepository.findById(robotId).ifPresent(robot -> {
            robot.setLastSeenAt(LocalDateTime.now());
            robotRepository.save(robot);
        });

        return focusSessionRepository
                .findFirstByRobot_IdAndStatusInOrderByStartTimeDesc(
                        robotId,
                        List.of(FocusSession.SessionStatus.active,
                                FocusSession.SessionStatus.paused))
                .map(this::toSessionResponse);
    }

    /**
     * Records a state reported by the robot and returns the expression to display.
     * Reports for sessions that are not active, and old sequence numbers, are ignored (not errors).
     */
    @Transactional
    public FocusStateResponse recordState(UUID robotId, FocusStateRequest request) {
        FocusSession session = focusSessionRepository
                .findByIdForUpdate(request.sessionId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Session not found"));

        if (session.getRobot() == null
                || !session.getRobot().getId().equals(robotId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Robot does not own this session");
        }

        if (session.getStatus() != FocusSession.SessionStatus.active) {
            return new FocusStateResponse(
                    "Session is not active; event ignored",
                    session.getCurrentState(),
                    session.getLastSequenceNumber());
        }

        if (request.sequenceNumber() <= session.getLastSequenceNumber()) {
            return new FocusStateResponse(
                    "Stale event ignored",
                    session.getCurrentState(),
                    session.getLastSequenceNumber());
        }

        // The server clock is used, so the robot's clock cannot change rewards.
        LocalDateTime now = LocalDateTime.now();
        accrueTime(session, now);

        // Count each time the user becomes distracted (shown in History).
        if (request.state() == FocusState.DISTRACTED
                && session.getCurrentState() != FocusState.DISTRACTED) {
            session.setDistractionCount(session.getDistractionCount() + 1);
        }

        session.setCurrentState(request.state());
        session.setLastEventAt(now);
        session.setLastSequenceNumber(request.sequenceNumber());

        // Save the matching face on the robot, so the app can read it later.
        Robot.Expression expression = switch (request.state()) {
            case FOCUSED -> Robot.Expression.smile;
            case DISTRACTED -> Robot.Expression.distressed;
            case AWAY -> Robot.Expression.eyes_closed;
        };
        session.getRobot().setCurrentExpression(expression);
        session.getRobot().setConnectionState(Robot.ConnectionState.connected);
        session.getRobot().setLastSeenAt(now);

        focusSessionRepository.save(session);

        return new FocusStateResponse(
                "State recorded",
                request.state(),
                request.sequenceNumber());
    }

    /**
     * The History screen: totals, the "Focus minutes" chart and the finished sessions
     * for one range. range is day (today), week (this Monday to Sunday),
     * month (this month) or all (everything; the chart shows the last 6 months).
     */
    @Transactional(readOnly = true)
    public HistoryResponse getHistory(UUID userId, String rangeParam) {
        String range = rangeParam == null ? "week" : rangeParam.trim().toLowerCase(Locale.ROOT);
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();

        // 1. The chart's buckets (start inclusive, end exclusive) and where the list starts.
        List<Bucket> buckets = new ArrayList<>();
        LocalDateTime listFrom;

        switch (range) {
            case "day" -> {
                LocalDateTime start = today.atStartOfDay();
                String[] labels = {"12a", "4a", "8a", "12p", "4p", "8p"};
                for (int i = 0; i < labels.length; i++) {
                    buckets.add(new Bucket(labels[i], start.plusHours(4L * i), start.plusHours(4L * (i + 1))));
                }
                listFrom = start;
            }
            case "week" -> {
                LocalDate monday = today.with(DayOfWeek.MONDAY);
                String[] labels = {"M", "T", "W", "T", "F", "S", "S"};
                for (int i = 0; i < 7; i++) {
                    LocalDate day = monday.plusDays(i);
                    buckets.add(new Bucket(labels[i], day.atStartOfDay(), day.plusDays(1).atStartOfDay()));
                }
                listFrom = monday.atStartOfDay();
            }
            case "month" -> {
                LocalDate first = today.withDayOfMonth(1);
                LocalDate nextMonth = first.plusMonths(1);
                int week = 1;
                for (LocalDate start = first; start.isBefore(nextMonth); start = start.plusDays(7)) {
                    LocalDate end = start.plusDays(7).isAfter(nextMonth) ? nextMonth : start.plusDays(7);
                    buckets.add(new Bucket("W" + week++, start.atStartOfDay(), end.atStartOfDay()));
                }
                listFrom = first.atStartOfDay();
            }
            case "all" -> {
                LocalDate thisMonth = today.withDayOfMonth(1);
                for (int i = 5; i >= 0; i--) {
                    LocalDate month = thisMonth.minusMonths(i);
                    String label = month.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                    buckets.add(new Bucket(label, month.atStartOfDay(), month.plusMonths(1).atStartOfDay()));
                }
                listFrom = null;
            }
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "range must be day, week, month or all");
        }

        // 2. The finished sessions in the range, newest first.
        List<FocusSession> sessions = listFrom == null
                ? focusSessionRepository.findByUser_IdAndStatusOrderByStartTimeDesc(
                        userId, FocusSession.SessionStatus.completed)
                : focusSessionRepository.findByUser_IdAndStatusAndStartTimeGreaterThanEqualOrderByStartTimeDesc(
                        userId, FocusSession.SessionStatus.completed, listFrom);

        // 3. Totals.
        int totalFocusSeconds = 0;
        float percentageSum = 0f;
        for (FocusSession session : sessions) {
            totalFocusSeconds += session.getFocusedSeconds();
            percentageSum += session.getFocusedPercentage() == null ? 0f : session.getFocusedPercentage();
        }
        float avgFocus = sessions.isEmpty() ? 0f : percentageSum / sessions.size();

        // 4. Chart bars: focused minutes of the sessions that started in each bucket.
        List<HistoryResponse.Bar> bars = new ArrayList<>();
        for (Bucket bucket : buckets) {
            int seconds = 0;
            for (FocusSession session : sessions) {
                LocalDateTime start = session.getStartTime();
                if (!start.isBefore(bucket.start()) && start.isBefore(bucket.end())) {
                    seconds += session.getFocusedSeconds();
                }
            }
            boolean current = !now.isBefore(bucket.start()) && now.isBefore(bucket.end());
            bars.add(new HistoryResponse.Bar(bucket.label(), Math.round(seconds / 60f), current));
        }

        // 5. The list (at most 50 sessions).
        List<HistoryResponse.PastSession> past = sessions.stream()
                .limit(50)
                .map(session -> new HistoryResponse.PastSession(
                        session.getId(),
                        session.getStartTime(),
                        Math.max(0, session.getDurationSeconds() - session.getPausedSeconds()),
                        session.getFocusedSeconds(),
                        session.getFocusedPercentage() == null ? 0f : session.getFocusedPercentage(),
                        session.getDistractionCount(),
                        session.getRewardEarned()))
                .toList();

        return new HistoryResponse(range, totalFocusSeconds, sessions.size(), avgFocus, bars, past);
    }

    /** One part of the History chart, from start (inclusive) to end (exclusive). */
    private record Bucket(String label, LocalDateTime start, LocalDateTime end) {}

    /**
     * Credits the time since the last report to the previous state.
     * Does nothing before the first report or right after a pause/resume.
     */
    private void accrueTime(FocusSession session, LocalDateTime now) {
        if (session.getCurrentState() == null || session.getLastEventAt() == null) {
            return;
        }

        long elapsed = Duration.between(session.getLastEventAt(), now).getSeconds();
        int seconds = (int) Math.min(Math.max(elapsed, 0), MAX_GAP_SECONDS);

        switch (session.getCurrentState()) {
            case FOCUSED -> session.setFocusedSeconds(session.getFocusedSeconds() + seconds);
            case DISTRACTED -> session.setDistractedSeconds(session.getDistractedSeconds() + seconds);
            case AWAY -> session.setAwaySeconds(session.getAwaySeconds() + seconds);
        }

        session.setLastEventAt(now);
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

        String currentState = session.getCurrentState() != null
                ? session.getCurrentState().name()
                : null;

        return new SessionResponse(
                session.getId(),
                robotId,
                session.getPlannedDurationSeconds(),
                session.getStartTime(),
                session.getStatus().name(),
                currentState
        );
    }
}
