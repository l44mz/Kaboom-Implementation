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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FocusSessionService.
 */
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

    /** A real session started 10 minutes ago with all counters set to 0. */
    private FocusSession sessionWithStatus(User user, FocusSession.SessionStatus status) {
        FocusSession session = new FocusSession(user, null, 1500);
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

    private void assertStatus(HttpStatus expected, ResponseStatusException ex) {
        assertEquals(expected, ex.getStatusCode());
    }

    private void assertBetween(int min, int max, int actual) {
        assertTrue(actual >= min && actual <= max,
                "expected a value between " + min + " and " + max + " but was " + actual);
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

        SessionResponse result = focusSessionService.startSession(userId, request);

        assertNotNull(result);
        ArgumentCaptor<FocusSession> captor = ArgumentCaptor.forClass(FocusSession.class);
        verify(focusSessionRepository).save(captor.capture());
        FocusSession saved = captor.getValue();
        assertEquals(FocusSession.SessionStatus.active, saved.getStatus());
        assertNotNull(saved.getStartTime());
        assertSame(user, saved.getUser());
        assertNull(saved.getRobot());
        verifyNoInteractions(robotRepository);
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

        SessionResponse result = focusSessionService.startSession(userId, request);

        assertNotNull(result);
        ArgumentCaptor<FocusSession> captor = ArgumentCaptor.forClass(FocusSession.class);
        verify(focusSessionRepository).save(captor.capture());
        assertSame(robot, captor.getValue().getRobot());
    }

    @Test
    void startSession_userAlreadyHasActiveOrPausedSession_throwsConflict() {
        UUID userId = UUID.randomUUID();
        CreateSessionRequest request = request(null, 1500);
        FocusSession existing = mock(FocusSession.class);
        when(focusSessionRepository.findActiveOrPausedSessionByUserId(userId))
                .thenReturn(Optional.of(existing));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> focusSessionService.startSession(userId, request));

        assertStatus(HttpStatus.CONFLICT, ex);
        verifyNoInteractions(userRepository);
        verify(focusSessionRepository, never()).save(any());
    }

    @Test
    void startSession_userNotFound_throwsNotFound() {
        UUID userId = UUID.randomUUID();
        CreateSessionRequest request = request(null, 1500);
        when(focusSessionRepository.findActiveOrPausedSessionByUserId(userId))
                .thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> focusSessionService.startSession(userId, request));

        assertStatus(HttpStatus.NOT_FOUND, ex);
        verify(focusSessionRepository, never()).save(any());
    }

    @Test
    void startSession_robotNotFound_throwsNotFound() {
        UUID userId = UUID.randomUUID();
        UUID robotId = UUID.randomUUID();
        User user = userWithId(userId);
        CreateSessionRequest request = request(robotId, 1500);
        when(focusSessionRepository.findActiveOrPausedSessionByUserId(userId))
                .thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(robotRepository.findById(robotId)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> focusSessionService.startSession(userId, request));

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
        CreateSessionRequest request = request(robotId, 1500);
        when(focusSessionRepository.findActiveOrPausedSessionByUserId(userId))
                .thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(robotRepository.findById(robotId)).thenReturn(Optional.of(robot));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> focusSessionService.startSession(userId, request));

        // 404 instead of 403 so we don't reveal that the robot exists
        assertStatus(HttpStatus.NOT_FOUND, ex);
        verify(focusSessionRepository, never()).save(any());
    }

    @Test
    void startSession_robotWithNoOwner_throwsNotFound() {
        UUID userId = UUID.randomUUID();
        UUID robotId = UUID.randomUUID();
        User user = userWithId(userId);
        Robot robot = robotOwnedBy(robotId, null);
        CreateSessionRequest request = request(robotId, 1500);
        when(focusSessionRepository.findActiveOrPausedSessionByUserId(userId))
                .thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(robotRepository.findById(robotId)).thenReturn(Optional.of(robot));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> focusSessionService.startSession(userId, request));

        assertStatus(HttpStatus.NOT_FOUND, ex);
    }

    // ---------- pauseSession ----------

    @Test
    void pauseSession_activeSession_becomesPausedAndRecordsPauseStart() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        User user = userWithId(userId);
        FocusSession session = sessionWithStatus(user, FocusSession.SessionStatus.active);
        when(focusSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        saveReturnsArgument();

        SessionResponse result = focusSessionService.pauseSession(userId, sessionId);

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
        FocusSession session = sessionWithStatus(user, FocusSession.SessionStatus.paused);
        when(focusSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> focusSessionService.pauseSession(userId, sessionId));

        assertStatus(HttpStatus.CONFLICT, ex);
        verify(focusSessionRepository, never()).save(any());
    }

    @Test
    void pauseSession_sessionNotFound_throwsNotFound() {
        UUID sessionId = UUID.randomUUID();
        when(focusSessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> focusSessionService.pauseSession(UUID.randomUUID(), sessionId));

        assertStatus(HttpStatus.NOT_FOUND, ex);
    }

    @Test
    void pauseSession_sessionOwnedByAnotherUser_throwsNotFound() {
        UUID sessionId = UUID.randomUUID();
        User owner = userWithId(UUID.randomUUID());
        FocusSession session = sessionWithStatus(owner, FocusSession.SessionStatus.active);
        when(focusSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> focusSessionService.pauseSession(UUID.randomUUID(), sessionId));

        assertStatus(HttpStatus.NOT_FOUND, ex);
        assertEquals(FocusSession.SessionStatus.active, session.getStatus()); // untouched
        verify(focusSessionRepository, never()).save(any());
    }

    // ---------- resumeSession ----------

    @Test
    void resumeSession_pausedSession_accumulatesPauseTimeAndBecomesActive() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        User user = userWithId(userId);
        FocusSession session = sessionWithStatus(user, FocusSession.SessionStatus.paused);
        session.setPausedSeconds(50);                                         // earlier pause
        session.setPauseStartedAt(LocalDateTime.now().minusSeconds(100));     // current pause
        when(focusSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        saveReturnsArgument();

        SessionResponse result = focusSessionService.resumeSession(userId, sessionId);

        assertNotNull(result);
        assertEquals(FocusSession.SessionStatus.active, session.getStatus());
        assertNull(session.getPauseStartedAt());
        assertBetween(150, 152, (int) session.getPausedSeconds());   // 50 + about 100
    }

    @Test
    void resumeSession_pausedWithoutPauseStart_keepsPausedSeconds() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        User user = userWithId(userId);
        FocusSession session = sessionWithStatus(user, FocusSession.SessionStatus.paused);
        session.setPausedSeconds(40);
        session.setPauseStartedAt(null);
        when(focusSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
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
        FocusSession session = sessionWithStatus(user, FocusSession.SessionStatus.active);
        when(focusSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> focusSessionService.resumeSession(userId, sessionId));

        assertStatus(HttpStatus.CONFLICT, ex);
        verify(focusSessionRepository, never()).save(any());
    }

    @Test
    void resumeSession_sessionNotFound_throwsNotFound() {
        UUID sessionId = UUID.randomUUID();
        when(focusSessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> focusSessionService.resumeSession(UUID.randomUUID(), sessionId));

        assertStatus(HttpStatus.NOT_FOUND, ex);
    }

    // ---------- endSession ----------

    @Test
    void endSession_activeSession_calculatesStatsAndAwardsCoins() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        User user = userWithId(userId);
        when(user.getCoins()).thenReturn(100);
        FocusSession session = sessionWithStatus(user, FocusSession.SessionStatus.active);
        session.setStartTime(LocalDateTime.now().minusHours(1));   // about 3600 s
        session.setFocusedSeconds(600);                            // 10 focused minutes
        when(focusSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        EndSessionResponse result = focusSessionService.endSession(userId, sessionId);

        assertNotNull(result);
        assertEquals(FocusSession.SessionStatus.completed, session.getStatus());
        assertNotNull(session.getEndTime());
        assertBetween(3600, 3602, (int) session.getDurationSeconds());
        assertEquals(600, (int) session.getFocusedSeconds());
        assertEquals(16.67f, session.getFocusedPercentage(), 0.1f);   // 600 / about 3600
        assertEquals(10, (int) session.getRewardEarned());            // 600 s / 60 = 10 coins
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
        FocusSession session = sessionWithStatus(user, FocusSession.SessionStatus.active);
        session.setStartTime(LocalDateTime.now().minusHours(2));
        session.setFocusedSeconds(6000);                           // 100 minutes, would be 100 coins
        when(focusSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

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
        FocusSession session = sessionWithStatus(user, FocusSession.SessionStatus.active);
        session.setStartTime(LocalDateTime.now().minusMinutes(10));   // about 600 s
        session.setFocusedSeconds(5000);                              // impossible value
        when(focusSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        focusSessionService.endSession(userId, sessionId);

        assertEquals((int) session.getDurationSeconds(), (int) session.getFocusedSeconds());
        assertEquals(100f, session.getFocusedPercentage(), 0.01f);
    }

    @Test
    void endSession_noFocusedTime_awardsNothing() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        User user = userWithId(userId);
        when(user.getCoins()).thenReturn(25);
        FocusSession session = sessionWithStatus(user, FocusSession.SessionStatus.active);
        // focusedSeconds stays 0 (state logging is not implemented yet)
        when(focusSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        focusSessionService.endSession(userId, sessionId);

        assertEquals(0, (int) session.getRewardEarned());
        assertEquals(0f, session.getFocusedPercentage(), 0.01f);
        verify(user).setCoins(25);   // balance unchanged
    }

    @Test
    void endSession_whilePaused_includesCurrentPauseInPausedSeconds() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        User user = userWithId(userId);
        when(user.getCoins()).thenReturn(0);
        FocusSession session = sessionWithStatus(user, FocusSession.SessionStatus.paused);
        session.setStartTime(LocalDateTime.now().minusMinutes(10));
        session.setPauseStartedAt(LocalDateTime.now().minusMinutes(5));   // paused for the last 5 min
        when(focusSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

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
        FocusSession session = sessionWithStatus(user, FocusSession.SessionStatus.completed);
        when(focusSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> focusSessionService.endSession(userId, sessionId));

        assertStatus(HttpStatus.CONFLICT, ex);
        verify(focusSessionRepository, never()).save(any());
        verify(userRepository, never()).save(any());   // no double reward
    }

    @Test
    void endSession_sessionNotFound_throwsNotFound() {
        UUID sessionId = UUID.randomUUID();
        when(focusSessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> focusSessionService.endSession(UUID.randomUUID(), sessionId));

        assertStatus(HttpStatus.NOT_FOUND, ex);
    }

    @Test
    void endSession_sessionOwnedByAnotherUser_throwsNotFoundAndAwardsNothing() {
        UUID sessionId = UUID.randomUUID();
        User owner = userWithId(UUID.randomUUID());
        FocusSession session = sessionWithStatus(owner, FocusSession.SessionStatus.active);
        when(focusSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> focusSessionService.endSession(UUID.randomUUID(), sessionId));

        assertStatus(HttpStatus.NOT_FOUND, ex);
        verify(userRepository, never()).save(any());
        verify(owner, never()).setCoins(anyInt());
    }
}
