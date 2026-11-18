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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FocusSessionServiceTest {

    @Mock private FocusSessionRepository focusSessionRepository;
    @Mock private UserRepository userRepository;
    @Mock private RobotRepository robotRepository;

    @InjectMocks private FocusSessionService focusSessionService;

    // ---------- helpers ----------

    private User userWithId(UUID id) {
        User user = mock(User.class);
        lenient().when(user.getId()).thenReturn(id);
        return user;
    }

    private Robot robotOwnedBy(UUID robotId, User owner) {
        Robot robot = mock(Robot.class);
        lenient().when(robot.getId()).thenReturn(robotId);
        lenient().when(robot.getUser()).thenReturn(owner);
        return robot;
    }

    private CreateSessionRequest request(UUID robotId, int plannedSeconds) {
        CreateSessionRequest request = mock(CreateSessionRequest.class);
        lenient().when(request.getRobotId()).thenReturn(robotId);
        lenient().when(request.getPlannedDurationSeconds()).thenReturn(plannedSeconds);
        return request;
    }

    private FocusSession sessionWithStatus(
            User user, FocusSession.SessionStatus status) {
        FocusSession session = new FocusSession(user, null, 1500);
        session.setStartTime(LocalDateTime.now().minusMinutes(10));
        session.setStatus(status);
        session.setPausedSeconds(0);
        session.setFocusedSeconds(0);
        return session;
    }

    private FocusSession sessionWithRobot(
            User user, Robot robot, FocusSession.SessionStatus status) {
        FocusSession session = new FocusSession(user, robot, 1500);
        session.setStartTime(LocalDateTime.now().minusMinutes(10));
        session.setStatus(status);
        session.setPausedSeconds(0);
        session.setFocusedSeconds(0);
        return session;
    }

    private void saveReturnsArgument() {
        when(focusSessionRepository.save(any(FocusSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void assertStatus(
            HttpStatus expected, ResponseStatusException ex) {
        assertEquals(expected, ex.getStatusCode());
    }

    private void assertBetween(int min, int max, int actual) {
        assertTrue(actual >= min && actual <= max,
                "expected a value between " + min + " and " + max
                        + " but was " + actual);
    }

    private FocusStateRequest stateRequest(
            UUID sessionId, FocusState state, long sequenceNumber) {
        FocusStateRequest request = mock(FocusStateRequest.class);

        lenient().when(request.sessionId()).thenReturn(sessionId);
        lenient().when(request.state()).thenReturn(state);
        lenient().when(request.sequenceNumber()).thenReturn(sequenceNumber);

        return request;
    }

    // ---------- startSession ----------

    @Test
    void startSession_withoutRobot_savesActiveSession() {
        UUID userId = UUID.randomUUID();
        User user = userWithId(userId);
        CreateSessionRequest request = request(null, 1500);

        when(focusSessionRepository.findActiveOrPausedSessionByUserId(userId))
                .thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        saveReturnsArgument();

        SessionResponse result =
                focusSessionService.startSession(userId, request);

        assertNotNull(result);

        ArgumentCaptor<FocusSession> captor =
                ArgumentCaptor.forClass(FocusSession.class);

        verify(focusSessionRepository).save(captor.capture());

        FocusSession saved = captor.getValue();

        assertEquals(FocusSession.SessionStatus.active, saved.getStatus());
        assertNotNull(saved.getStartTime());
        assertSame(user, saved.getUser());
        assertNull(saved.getRobot());

        verify(robotRepository).findByUserId(userId);
        verify(robotRepository, never()).findById(any());
    }

    @Test
    void startSession_withOwnedRobot_attachesRobot() {
        UUID userId = UUID.randomUUID();
        UUID robotId = UUID.randomUUID();

        User user = userWithId(userId);
        Robot robot = robotOwnedBy(robotId, user);
        CreateSessionRequest request = request(robotId, 1500);

        when(focusSessionRepository.findActiveOrPausedSessionByUserId(userId))
                .thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(robotRepository.findById(robotId)).thenReturn(Optional.of(robot));
        saveReturnsArgument();

        SessionResponse result =
                focusSessionService.startSession(userId, request);

        assertNotNull(result);

        ArgumentCaptor<FocusSession> captor =
                ArgumentCaptor.forClass(FocusSession.class);

        verify(focusSessionRepository).save(captor.capture());
        assertSame(robot, captor.getValue().getRobot());
    }

    @Test
    void startSession_userAlreadyHasActiveOrPausedSession_throwsConflict() {
        UUID userId = UUID.randomUUID();
        CreateSessionRequest request = request(null, 1500);

        when(focusSessionRepository.findActiveOrPausedSessionByUserId(userId))
                .thenReturn(Optional.of(mock(FocusSession.class)));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> focusSessionService.startSession(userId, request));

        assertStatus(HttpStatus.CONFLICT, ex);
        verifyNoInteractions(userRepository);
        verify(focusSessionRepository, never()).save(any());
    }

    @Test
    void startSession_userNotFound_throwsNotFound() {
        UUID userId = UUID.randomUUID();

        when(focusSessionRepository.findActiveOrPausedSessionByUserId(userId))
                .thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> focusSessionService.startSession(
                        userId, request(null, 1500)));

        assertStatus(HttpStatus.NOT_FOUND, ex);
    }

    @Test
    void startSession_robotNotFound_throwsNotFound() {
        UUID userId = UUID.randomUUID();
        UUID robotId = UUID.randomUUID();

        User user = userWithId(userId);

        when(focusSessionRepository.findActiveOrPausedSessionByUserId(userId))
                .thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(robotRepository.findById(robotId)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> focusSessionService.startSession(
                        userId, request(robotId, 1500)));

        assertStatus(HttpStatus.NOT_FOUND, ex);
        verify(focusSessionRepository, never()).save(any());
    }

    @Test
    void startSession_robotBelongsToAnotherUser_throwsNotFound() {
        UUID userId = UUID.randomUUID();
        UUID robotId = UUID.randomUUID();

        User user = userWithId(userId);
        User otherUser = userWithId(UUID.randomUUID());
        Robot robot = robotOwnedBy(robotId, otherUser);

        when(focusSessionRepository.findActiveOrPausedSessionByUserId(userId))
                .thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(robotRepository.findById(robotId)).thenReturn(Optional.of(robot));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> focusSessionService.startSession(
                        userId, request(robotId, 1500)));

        assertStatus(HttpStatus.NOT_FOUND, ex);
        verify(focusSessionRepository, never()).save(any());
    }

    @Test
    void startSession_robotWithNoOwner_throwsNotFound() {
        UUID userId = UUID.randomUUID();
        UUID robotId = UUID.randomUUID();

        User user = userWithId(userId);
        Robot robot = robotOwnedBy(robotId, null);

        when(focusSessionRepository.findActiveOrPausedSessionByUserId(userId))
                .thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(robotRepository.findById(robotId)).thenReturn(Optional.of(robot));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> focusSessionService.startSession(
                        userId, request(robotId, 1500)));

        assertStatus(HttpStatus.NOT_FOUND, ex);
    }

    // ---------- startSessionForRobot ----------

    @Test
    void startSessionForRobot_validRobot_createsSessionForOwner() {
        UUID robotId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User user = userWithId(userId);
        Robot robot = robotOwnedBy(robotId, user);

        when(robotRepository.findById(robotId)).thenReturn(Optional.of(robot));
        when(focusSessionRepository.findActiveOrPausedSessionByUserId(userId))
                .thenReturn(Optional.empty());

        saveReturnsArgument();

        SessionResponse result = focusSessionService.startSessionForRobot(
                robotId, request(null, 1500));

        assertNotNull(result);

        ArgumentCaptor<FocusSession> captor =
                ArgumentCaptor.forClass(FocusSession.class);

        verify(focusSessionRepository).save(captor.capture());

        assertSame(user, captor.getValue().getUser());
        assertSame(robot, captor.getValue().getRobot());
        assertEquals(
                FocusSession.SessionStatus.active,
                captor.getValue().getStatus());
    }

    @Test
    void startSessionForRobot_robotNotFound_throwsNotFound() {
        UUID robotId = UUID.randomUUID();

        when(robotRepository.findById(robotId)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> focusSessionService.startSessionForRobot(
                        robotId, request(null, 1500)));

        assertStatus(HttpStatus.NOT_FOUND, ex);
        verify(focusSessionRepository, never()).save(any());
    }

    @Test
    void startSessionForRobot_robotNotPaired_throwsConflict() {
        UUID robotId = UUID.randomUUID();
        Robot robot = robotOwnedBy(robotId, null);

        when(robotRepository.findById(robotId)).thenReturn(Optional.of(robot));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> focusSessionService.startSessionForRobot(
                        robotId, request(null, 1500)));

        assertStatus(HttpStatus.CONFLICT, ex);
        verify(focusSessionRepository, never()).save(any());
    }

    @Test
    void startSessionForRobot_ownerAlreadyHasOpenSession_throwsConflict() {
        UUID robotId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User user = userWithId(userId);
        Robot robot = robotOwnedBy(robotId, user);

        when(robotRepository.findById(robotId)).thenReturn(Optional.of(robot));
        when(focusSessionRepository.findActiveOrPausedSessionByUserId(userId))
                .thenReturn(Optional.of(mock(FocusSession.class)));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> focusSessionService.startSessionForRobot(
                        robotId, request(null, 1500)));

        assertStatus(HttpStatus.CONFLICT, ex);
        verify(focusSessionRepository, never()).save(any());
    }

    // ---------- getSession ----------

    @Test
    void getSession_ownedSession_returnsSession() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        User user = userWithId(userId);
        FocusSession session =
                sessionWithStatus(user, FocusSession.SessionStatus.active);

        when(focusSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));

        SessionResponse result =
                focusSessionService.getSession(userId, sessionId);

        assertNotNull(result);
    }

    @Test
    void getSession_sessionNotFound_throwsNotFound() {
        UUID sessionId = UUID.randomUUID();

        when(focusSessionRepository.findById(sessionId))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> focusSessionService.getSession(
                        UUID.randomUUID(), sessionId));

        assertStatus(HttpStatus.NOT_FOUND, ex);
    }

    @Test
    void getSession_sessionBelongsToAnotherUser_throwsNotFound() {
        UUID sessionId = UUID.randomUUID();

        User owner = userWithId(UUID.randomUUID());
        FocusSession session =
                sessionWithStatus(owner, FocusSession.SessionStatus.active);

        when(focusSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> focusSessionService.getSession(
                        UUID.randomUUID(), sessionId));

        assertStatus(HttpStatus.NOT_FOUND, ex);
    }

    // ---------- pauseSession ----------

    @Test
    void pauseSession_activeSession_becomesPausedAndRecordsPauseStart() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        User user = userWithId(userId);
        FocusSession session =
                sessionWithStatus(user, FocusSession.SessionStatus.active);

        when(focusSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));

        saveReturnsArgument();

        SessionResponse result =
                focusSessionService.pauseSession(userId, sessionId);

        assertNotNull(result);
        assertEquals(FocusSession.SessionStatus.paused, session.getStatus());
        assertNotNull(session.getPauseStartedAt());

        verify(focusSessionRepository).save(session);
    }

    @Test
    void pauseSession_alreadyPaused_throwsConflict() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        User user = userWithId(userId);
        FocusSession session =
                sessionWithStatus(user, FocusSession.SessionStatus.paused);

        when(focusSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> focusSessionService.pauseSession(userId, sessionId));

        assertStatus(HttpStatus.CONFLICT, ex);
        verify(focusSessionRepository, never()).save(any());
    }

    @Test
    void pauseSession_sessionNotFound_throwsNotFound() {
        UUID sessionId = UUID.randomUUID();

        when(focusSessionRepository.findById(sessionId))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> focusSessionService.pauseSession(
                        UUID.randomUUID(), sessionId));

        assertStatus(HttpStatus.NOT_FOUND, ex);
    }

    @Test
    void pauseSession_sessionOwnedByAnotherUser_throwsNotFound() {
        UUID sessionId = UUID.randomUUID();

        User owner = userWithId(UUID.randomUUID());
        FocusSession session =
                sessionWithStatus(owner, FocusSession.SessionStatus.active);

        when(focusSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> focusSessionService.pauseSession(
                        UUID.randomUUID(), sessionId));

        assertStatus(HttpStatus.NOT_FOUND, ex);
        assertEquals(FocusSession.SessionStatus.active, session.getStatus());
        verify(focusSessionRepository, never()).save(any());
    }

    // ---------- resumeSession ----------

    @Test
    void resumeSession_pausedSession_accumulatesPauseTimeAndBecomesActive() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        User user = userWithId(userId);
        FocusSession session =
                sessionWithStatus(user, FocusSession.SessionStatus.paused);

        session.setPausedSeconds(50);
        session.setPauseStartedAt(LocalDateTime.now().minusSeconds(100));

        when(focusSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));

        saveReturnsArgument();

        SessionResponse result =
                focusSessionService.resumeSession(userId, sessionId);

        assertNotNull(result);
        assertEquals(FocusSession.SessionStatus.active, session.getStatus());
        assertNull(session.getPauseStartedAt());
        assertBetween(150, 152, (int) session.getPausedSeconds());
    }

    @Test
    void resumeSession_pausedWithoutPauseStart_keepsPausedSeconds() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        User user = userWithId(userId);
        FocusSession session =
                sessionWithStatus(user, FocusSession.SessionStatus.paused);

        session.setPausedSeconds(40);
        session.setPauseStartedAt(null);

        when(focusSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));

        saveReturnsArgument();

        focusSessionService.resumeSession(userId, sessionId);

        assertEquals(FocusSession.SessionStatus.active, session.getStatus());
        assertEquals(40, (int) session.getPausedSeconds());
    }

    @Test
    void resumeSession_activeSession_throwsConflict() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        User user = userWithId(userId);
        FocusSession session =
                sessionWithStatus(user, FocusSession.SessionStatus.active);

        when(focusSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> focusSessionService.resumeSession(userId, sessionId));

        assertStatus(HttpStatus.CONFLICT, ex);
        verify(focusSessionRepository, never()).save(any());
    }

    @Test
    void resumeSession_sessionNotFound_throwsNotFound() {
        UUID sessionId = UUID.randomUUID();

        when(focusSessionRepository.findById(sessionId))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> focusSessionService.resumeSession(
                        UUID.randomUUID(), sessionId));

        assertStatus(HttpStatus.NOT_FOUND, ex);
    }

    // ---------- endSession ----------

    @Test
    void endSession_activeSession_calculatesStatsAndAwardsCoins() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        User user = userWithId(userId);
        when(user.getCoins()).thenReturn(100);

        FocusSession session =
                sessionWithStatus(user, FocusSession.SessionStatus.active);

        session.setStartTime(LocalDateTime.now().minusHours(1));
        session.setFocusedSeconds(600);

        when(focusSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));

        EndSessionResponse result =
                focusSessionService.endSession(userId, sessionId);

        assertNotNull(result);
        assertEquals(FocusSession.SessionStatus.completed, session.getStatus());
        assertNotNull(session.getEndTime());
        assertBetween(3600, 3602, (int) session.getDurationSeconds());
        assertEquals(600, (int) session.getFocusedSeconds());
        assertEquals(16.67f, session.getFocusedPercentage(), 0.1f);
        assertEquals(10, (int) session.getRewardEarned());

        verify(user).setCoins(110);
        verify(focusSessionRepository).save(session);
        verify(userRepository).save(user);
    }

    @Test
    void endSession_rewardIsCappedAtThirtyCoins() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        User user = userWithId(userId);
        when(user.getCoins()).thenReturn(0);

        FocusSession session =
                sessionWithStatus(user, FocusSession.SessionStatus.active);

        session.setStartTime(LocalDateTime.now().minusHours(2));
        session.setFocusedSeconds(6000);

        when(focusSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));

        focusSessionService.endSession(userId, sessionId);

        assertEquals(30, (int) session.getRewardEarned());
        verify(user).setCoins(30);
    }

    @Test
    void endSession_focusedSecondsCannotExceedActiveTime() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        User user = userWithId(userId);
        when(user.getCoins()).thenReturn(0);

        FocusSession session =
                sessionWithStatus(user, FocusSession.SessionStatus.active);

        session.setStartTime(LocalDateTime.now().minusMinutes(10));
        session.setFocusedSeconds(5000);

        when(focusSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));

        focusSessionService.endSession(userId, sessionId);

        assertEquals(
                (int) session.getDurationSeconds(),
                (int) session.getFocusedSeconds());

        assertEquals(100f, session.getFocusedPercentage(), 0.01f);
    }

    @Test
    void endSession_noFocusedTime_awardsNothing() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        User user = userWithId(userId);
        when(user.getCoins()).thenReturn(25);

        FocusSession session =
                sessionWithStatus(user, FocusSession.SessionStatus.active);

        when(focusSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));

        focusSessionService.endSession(userId, sessionId);

        assertEquals(0, (int) session.getRewardEarned());
        assertEquals(0f, session.getFocusedPercentage(), 0.01f);
        verify(user).setCoins(25);
    }

    @Test
    void endSession_whilePaused_includesCurrentPauseInPausedSeconds() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        User user = userWithId(userId);
        when(user.getCoins()).thenReturn(0);

        FocusSession session =
                sessionWithStatus(user, FocusSession.SessionStatus.paused);

        session.setStartTime(LocalDateTime.now().minusMinutes(10));
        session.setPauseStartedAt(LocalDateTime.now().minusMinutes(5));

        when(focusSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));

        focusSessionService.endSession(userId, sessionId);

        assertBetween(300, 302, (int) session.getPausedSeconds());
        assertNull(session.getPauseStartedAt());
        assertEquals(FocusSession.SessionStatus.completed, session.getStatus());
    }

    @Test
    void endSession_alreadyCompleted_throwsConflict() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        User user = userWithId(userId);
        FocusSession session =
                sessionWithStatus(user, FocusSession.SessionStatus.completed);

        when(focusSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> focusSessionService.endSession(userId, sessionId));

        assertStatus(HttpStatus.CONFLICT, ex);
        verify(focusSessionRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void endSession_sessionNotFound_throwsNotFound() {
        UUID sessionId = UUID.randomUUID();

        when(focusSessionRepository.findById(sessionId))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> focusSessionService.endSession(
                        UUID.randomUUID(), sessionId));

        assertStatus(HttpStatus.NOT_FOUND, ex);
    }

    @Test
    void endSession_sessionOwnedByAnotherUser_throwsNotFoundAndAwardsNothing() {
        UUID sessionId = UUID.randomUUID();

        User owner = userWithId(UUID.randomUUID());
        FocusSession session =
                sessionWithStatus(owner, FocusSession.SessionStatus.active);

        when(focusSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> focusSessionService.endSession(
                        UUID.randomUUID(), sessionId));

        assertStatus(HttpStatus.NOT_FOUND, ex);
        verify(userRepository, never()).save(any());
        verify(owner, never()).setCoins(anyInt());
    }

    // ---------- getActiveSessionForRobot ----------

    @Test
    void getActiveSessionForRobot_activeSession_returnsSessionAndUpdatesRobot() {
        UUID robotId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User user = userWithId(userId);
        Robot robot = robotOwnedBy(robotId, user);

        FocusSession session = sessionWithRobot(
                user, robot, FocusSession.SessionStatus.active);

        when(robotRepository.findById(robotId)).thenReturn(Optional.of(robot));

        when(focusSessionRepository
                .findFirstByRobot_IdAndStatusInOrderByStartTimeDesc(
                        eq(robotId), anyList()))
                .thenReturn(Optional.of(session));

        Optional<SessionResponse> result =
                focusSessionService.getActiveSessionForRobot(robotId);

        assertTrue(result.isPresent());
        verify(robot).setLastSeenAt(any(LocalDateTime.class));
        verify(robotRepository).save(robot);
    }

    @Test
    void getActiveSessionForRobot_noSession_returnsEmpty() {
        UUID robotId = UUID.randomUUID();

        when(robotRepository.findById(robotId)).thenReturn(Optional.empty());

        when(focusSessionRepository
                .findFirstByRobot_IdAndStatusInOrderByStartTimeDesc(
                        eq(robotId), anyList()))
                .thenReturn(Optional.empty());

        Optional<SessionResponse> result =
                focusSessionService.getActiveSessionForRobot(robotId);

        assertTrue(result.isEmpty());
    }

    @Test
    void getActiveSessionForRobot_robotNotFound_stillChecksSession() {
        UUID robotId = UUID.randomUUID();

        when(robotRepository.findById(robotId)).thenReturn(Optional.empty());

        when(focusSessionRepository
                .findFirstByRobot_IdAndStatusInOrderByStartTimeDesc(
                        eq(robotId), anyList()))
                .thenReturn(Optional.empty());

        assertTrue(
                focusSessionService.getActiveSessionForRobot(robotId).isEmpty());

        verify(robotRepository, never()).save(any());
    }

    // ---------- recordState ----------

    @Test
    void recordState_focusedState_updatesSessionAndRobot() {
        UUID robotId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        User user = userWithId(UUID.randomUUID());
        Robot robot = robotOwnedBy(robotId, user);

        FocusSession session = sessionWithRobot(
                user, robot, FocusSession.SessionStatus.active);

        when(focusSessionRepository.findByIdForUpdate(sessionId))
                .thenReturn(Optional.of(session));

        FocusStateRequest request =
                stateRequest(sessionId, FocusState.FOCUSED, 1);

        FocusStateResponse result =
                focusSessionService.recordState(robotId, request);

        assertNotNull(result);
        assertEquals(FocusState.FOCUSED, session.getCurrentState());
        assertEquals(1, session.getLastSequenceNumber());

        verify(robot).setCurrentExpression(Robot.Expression.smile);
        verify(robot).setConnectionState(Robot.ConnectionState.connected);
        verify(robot).setLastSeenAt(any(LocalDateTime.class));
        verify(focusSessionRepository).save(session);
    }

    @Test
    void recordState_distractionTransition_incrementsDistractionCount() {
        UUID robotId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        User user = userWithId(UUID.randomUUID());
        Robot robot = robotOwnedBy(robotId, user);

        FocusSession session = sessionWithRobot(
                user, robot, FocusSession.SessionStatus.active);

        when(focusSessionRepository.findByIdForUpdate(sessionId))
                .thenReturn(Optional.of(session));

        focusSessionService.recordState(
                robotId,
                stateRequest(sessionId, FocusState.DISTRACTED, 1));

        assertEquals(1, session.getDistractionCount());
        assertEquals(FocusState.DISTRACTED, session.getCurrentState());
    }

    @Test
    void recordState_repeatedDistraction_doesNotIncrementCountAgain() {
        UUID robotId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        User user = userWithId(UUID.randomUUID());
        Robot robot = robotOwnedBy(robotId, user);

        FocusSession session = sessionWithRobot(
                user, robot, FocusSession.SessionStatus.active);

        when(focusSessionRepository.findByIdForUpdate(sessionId))
                .thenReturn(Optional.of(session));

        focusSessionService.recordState(
                robotId,
                stateRequest(sessionId, FocusState.DISTRACTED, 1));

        focusSessionService.recordState(
                robotId,
                stateRequest(sessionId, FocusState.DISTRACTED, 2));

        assertEquals(1, session.getDistractionCount());
    }

    @Test
    void recordState_awayTransition_incrementsAwayCount() {
        UUID robotId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        User user = userWithId(UUID.randomUUID());
        Robot robot = robotOwnedBy(robotId, user);

        FocusSession session = sessionWithRobot(
                user, robot, FocusSession.SessionStatus.active);

        when(focusSessionRepository.findByIdForUpdate(sessionId))
                .thenReturn(Optional.of(session));

        focusSessionService.recordState(
                robotId,
                stateRequest(sessionId, FocusState.AWAY, 1));

        assertEquals(1, session.getAwayCount());
        assertEquals(FocusState.AWAY, session.getCurrentState());
    }

    @Test
    void recordState_fourDistractionTransitions_triggersExplosion() {
        UUID robotId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        User user = userWithId(UUID.randomUUID());
        Robot robot = robotOwnedBy(robotId, user);

        FocusSession session = sessionWithRobot(
                user, robot, FocusSession.SessionStatus.active);

        when(focusSessionRepository.findByIdForUpdate(sessionId))
                .thenReturn(Optional.of(session));

        focusSessionService.recordState(
                robotId, stateRequest(sessionId, FocusState.DISTRACTED, 1));

        focusSessionService.recordState(
                robotId, stateRequest(sessionId, FocusState.FOCUSED, 2));

        focusSessionService.recordState(
                robotId, stateRequest(sessionId, FocusState.DISTRACTED, 3));

        focusSessionService.recordState(
                robotId, stateRequest(sessionId, FocusState.FOCUSED, 4));

        focusSessionService.recordState(
                robotId, stateRequest(sessionId, FocusState.DISTRACTED, 5));

        focusSessionService.recordState(
                robotId, stateRequest(sessionId, FocusState.FOCUSED, 6));

        focusSessionService.recordState(
                robotId, stateRequest(sessionId, FocusState.DISTRACTED, 7));

        assertEquals(4, session.getDistractionCount());
        assertTrue(session.isExplosionTriggered());
    }

    @Test
    void recordState_twoAwayTransitions_triggersExplosion() {
        UUID robotId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        User user = userWithId(UUID.randomUUID());
        Robot robot = robotOwnedBy(robotId, user);

        FocusSession session = sessionWithRobot(
                user, robot, FocusSession.SessionStatus.active);

        when(focusSessionRepository.findByIdForUpdate(sessionId))
                .thenReturn(Optional.of(session));

        focusSessionService.recordState(
                robotId, stateRequest(sessionId, FocusState.AWAY, 1));

        focusSessionService.recordState(
                robotId, stateRequest(sessionId, FocusState.FOCUSED, 2));

        focusSessionService.recordState(
                robotId, stateRequest(sessionId, FocusState.AWAY, 3));

        assertEquals(2, session.getAwayCount());
        assertTrue(session.isExplosionTriggered());
    }

    @Test
    void recordState_staleSequenceNumber_isIgnored() {
        UUID robotId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        User user = userWithId(UUID.randomUUID());
        Robot robot = robotOwnedBy(robotId, user);

        FocusSession session = sessionWithRobot(
                user, robot, FocusSession.SessionStatus.active);

        session.setLastSequenceNumber(5);
        session.setCurrentState(FocusState.FOCUSED);

        when(focusSessionRepository.findByIdForUpdate(sessionId))
                .thenReturn(Optional.of(session));

        FocusStateResponse result = focusSessionService.recordState(
                robotId,
                stateRequest(sessionId, FocusState.DISTRACTED, 4));

        assertNotNull(result);
        assertEquals(FocusState.FOCUSED, session.getCurrentState());
        assertEquals(5, session.getLastSequenceNumber());
        assertEquals(0, session.getDistractionCount());

        verify(focusSessionRepository, never()).save(any());
    }

    @Test
    void recordState_pausedSession_ignoresReport() {
        UUID robotId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        User user = userWithId(UUID.randomUUID());
        Robot robot = robotOwnedBy(robotId, user);

        FocusSession session = sessionWithRobot(
                user, robot, FocusSession.SessionStatus.paused);

        when(focusSessionRepository.findByIdForUpdate(sessionId))
                .thenReturn(Optional.of(session));

        FocusStateResponse result = focusSessionService.recordState(
                robotId,
                stateRequest(sessionId, FocusState.DISTRACTED, 1));

        assertNotNull(result);
        assertEquals(0, session.getDistractionCount());
        verify(focusSessionRepository, never()).save(any());
    }

    @Test
    void recordState_wrongRobot_throwsForbidden() {
        UUID robotId = UUID.randomUUID();
        UUID otherRobotId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        User user = userWithId(UUID.randomUUID());
        Robot robot = robotOwnedBy(otherRobotId, user);

        FocusSession session = sessionWithRobot(
                user, robot, FocusSession.SessionStatus.active);

        when(focusSessionRepository.findByIdForUpdate(sessionId))
                .thenReturn(Optional.of(session));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> focusSessionService.recordState(
                        robotId,
                        stateRequest(sessionId, FocusState.FOCUSED, 1)));

        assertStatus(HttpStatus.FORBIDDEN, ex);
        verify(focusSessionRepository, never()).save(any());
    }

    @Test
    void recordState_sessionNotFound_throwsNotFound() {
        UUID robotId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        when(focusSessionRepository.findByIdForUpdate(sessionId))
                .thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> focusSessionService.recordState(
                        robotId,
                        stateRequest(sessionId, FocusState.FOCUSED, 1)));

        assertStatus(HttpStatus.NOT_FOUND, ex);
    }

    // ---------- getHistory ----------

    @Test
    void getHistory_dayRange_returnsHistory() {
        UUID userId = UUID.randomUUID();

        when(focusSessionRepository
                .findByUser_IdAndStatusAndStartTimeGreaterThanEqualOrderByStartTimeDesc(
                        eq(userId),
                        eq(FocusSession.SessionStatus.completed),
                        any(LocalDateTime.class)))
                .thenReturn(List.of());

        HistoryResponse result =
                focusSessionService.getHistory(userId, "day");

        assertNotNull(result);
        verify(focusSessionRepository)
                .findByUser_IdAndStatusAndStartTimeGreaterThanEqualOrderByStartTimeDesc(
                        eq(userId),
                        eq(FocusSession.SessionStatus.completed),
                        any(LocalDateTime.class));
    }

    @Test
    void getHistory_weekRange_returnsHistory() {
        UUID userId = UUID.randomUUID();

        when(focusSessionRepository
                .findByUser_IdAndStatusAndStartTimeGreaterThanEqualOrderByStartTimeDesc(
                        eq(userId),
                        eq(FocusSession.SessionStatus.completed),
                        any(LocalDateTime.class)))
                .thenReturn(List.of());

        HistoryResponse result =
                focusSessionService.getHistory(userId, "week");

        assertNotNull(result);
    }

    @Test
    void getHistory_monthRange_returnsHistory() {
        UUID userId = UUID.randomUUID();

        when(focusSessionRepository
                .findByUser_IdAndStatusAndStartTimeGreaterThanEqualOrderByStartTimeDesc(
                        eq(userId),
                        eq(FocusSession.SessionStatus.completed),
                        any(LocalDateTime.class)))
                .thenReturn(List.of());

        HistoryResponse result =
                focusSessionService.getHistory(userId, "month");

        assertNotNull(result);
    }

    @Test
    void getHistory_allRange_usesAllCompletedSessionsQuery() {
        UUID userId = UUID.randomUUID();

        when(focusSessionRepository
                .findByUser_IdAndStatusOrderByStartTimeDesc(
                        userId, FocusSession.SessionStatus.completed))
                .thenReturn(List.of());

        HistoryResponse result =
                focusSessionService.getHistory(userId, "all");

        assertNotNull(result);

        verify(focusSessionRepository)
                .findByUser_IdAndStatusOrderByStartTimeDesc(
                        userId, FocusSession.SessionStatus.completed);
    }

    @Test
    void getHistory_nullRange_defaultsToWeek() {
        UUID userId = UUID.randomUUID();

        when(focusSessionRepository
                .findByUser_IdAndStatusAndStartTimeGreaterThanEqualOrderByStartTimeDesc(
                        eq(userId),
                        eq(FocusSession.SessionStatus.completed),
                        any(LocalDateTime.class)))
                .thenReturn(List.of());

        HistoryResponse result =
                focusSessionService.getHistory(userId, null);

        assertNotNull(result);
    }

    @Test
    void getHistory_invalidRange_throwsBadRequest() {
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> focusSessionService.getHistory(
                        UUID.randomUUID(), "invalid"));

        assertStatus(HttpStatus.BAD_REQUEST, ex);
    }

    @Test
    void getHistory_emptyResults_returnsHistory() {
        UUID userId = UUID.randomUUID();

        when(focusSessionRepository
                .findByUser_IdAndStatusAndStartTimeGreaterThanEqualOrderByStartTimeDesc(
                        eq(userId),
                        eq(FocusSession.SessionStatus.completed),
                        any(LocalDateTime.class)))
                .thenReturn(List.of());

        HistoryResponse result =
                focusSessionService.getHistory(userId, "week");

        assertNotNull(result);
    }
}
