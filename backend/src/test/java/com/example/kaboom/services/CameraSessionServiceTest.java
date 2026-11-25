package com.example.kaboom.services;

import com.example.kaboom.dtos.CameraSessionEndResponse;
import com.example.kaboom.dtos.CameraSessionRequest;
import com.example.kaboom.dtos.CameraSessionResponse;
import com.example.kaboom.dtos.CameraStateRequest;
import com.example.kaboom.dtos.CameraStateResponse;
import com.example.kaboom.entities.FocusSession;
import com.example.kaboom.entities.User;
import com.example.kaboom.enums.FocusEventType;
import com.example.kaboom.enums.FocusState;
import com.example.kaboom.enums.SessionSource;
import com.example.kaboom.repositories.FocusSessionRepository;
import com.example.kaboom.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CameraSessionServiceTest {

    @Mock
    private FocusSessionRepository focusSessionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CameraSessionService cameraSessionService;

    private UUID userId;
    private UUID sessionId;
    private User user;
    private FocusSession session;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        sessionId = UUID.randomUUID();

        user = mock(User.class);
        session = mock(FocusSession.class);

        lenient().when(user.getId()).thenReturn(userId);
        lenient().when(user.getCoins()).thenReturn(100);

        lenient().when(session.getId()).thenReturn(sessionId);
        lenient().when(session.getUser()).thenReturn(user);
        lenient().when(session.getSource()).thenReturn(SessionSource.CAMERA);
        lenient().when(session.getStatus())
                .thenReturn(FocusSession.SessionStatus.active);
        lenient().when(session.getPlannedDurationSeconds()).thenReturn(1500);
        lenient().when(session.getStartTime()).thenReturn(LocalDateTime.now());
        lenient().when(session.getFocusedSeconds()).thenReturn(0);
        lenient().when(session.getPausedSeconds()).thenReturn(0);
        lenient().when(session.getDistractedSeconds()).thenReturn(0);
        lenient().when(session.getAwaySeconds()).thenReturn(0);
        lenient().when(session.getDistractionCount()).thenReturn(0);
        lenient().when(session.getAwayCount()).thenReturn(0);
        lenient().when(session.getLastSequenceNumber()).thenReturn(0L);
        lenient().when(focusSessionRepository.save(any(FocusSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private CameraStateRequest stateRequest(FocusState state, long sequence) {
        return new CameraStateRequest(
                sessionId,
                state,
                System.currentTimeMillis(),
                sequence,
                FocusEventType.values()[0]
        );
    }

    private void mockOwnedCameraSession() {
        when(focusSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(session));
    }

    private void mockSessionForUpdate() {
        when(focusSessionRepository.findByIdForUpdate(sessionId))
                .thenReturn(Optional.of(session));
    }

    private void assertStatus(
            HttpStatus expected,
            ResponseStatusException exception
    ) {
        assertEquals(expected, exception.getStatusCode());
    }

    // START SESSION

    @Test
    void startSession_validRequest_createsCameraSession() {
        CameraSessionRequest request = new CameraSessionRequest(1500);

        when(focusSessionRepository.findActiveOrPausedSessionByUserId(userId))
                .thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(focusSessionRepository.save(any(FocusSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CameraSessionResponse response =
                cameraSessionService.startSession(userId, request);

        assertNotNull(response);

        ArgumentCaptor<FocusSession> captor =
                ArgumentCaptor.forClass(FocusSession.class);

        verify(focusSessionRepository).save(captor.capture());

        FocusSession savedSession = captor.getValue();

        assertSame(user, savedSession.getUser());
        assertEquals(1500, savedSession.getPlannedDurationSeconds());
        assertEquals(SessionSource.CAMERA, savedSession.getSource());
        assertEquals(
                FocusSession.SessionStatus.active,
                savedSession.getStatus()
        );
        assertNotNull(savedSession.getStartTime());

        verify(userRepository).findById(userId);
    }

    @Test
    void startSession_existingActiveSession_throwsConflict() {
        CameraSessionRequest request = new CameraSessionRequest(1500);

        when(focusSessionRepository.findActiveOrPausedSessionByUserId(userId))
                .thenReturn(Optional.of(session));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> cameraSessionService.startSession(userId, request)
        );

        assertStatus(HttpStatus.CONFLICT, exception);
        verify(userRepository, never()).findById(any(UUID.class));
        verify(focusSessionRepository, never()).save(any(FocusSession.class));
    }

    @Test
    void startSession_userNotFound_throwsNotFound() {
        CameraSessionRequest request = new CameraSessionRequest(1500);

        when(focusSessionRepository.findActiveOrPausedSessionByUserId(userId))
                .thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> cameraSessionService.startSession(userId, request)
        );

        assertStatus(HttpStatus.NOT_FOUND, exception);
        verify(focusSessionRepository, never()).save(any(FocusSession.class));
    }

    // GET SESSION

    @Test
    void getSession_validOwner_returnsSession() {
        mockOwnedCameraSession();

        CameraSessionResponse response =
                cameraSessionService.getSession(userId, sessionId);

        assertNotNull(response);
        verify(focusSessionRepository).findById(sessionId);
    }

    @Test
    void getSession_sessionNotFound_throwsNotFound() {
        when(focusSessionRepository.findById(sessionId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> cameraSessionService.getSession(userId, sessionId)
        );

        assertStatus(HttpStatus.NOT_FOUND, exception);
    }

    @Test
    void getSession_differentOwner_throwsNotFound() {
        mockOwnedCameraSession();
        when(user.getId()).thenReturn(UUID.randomUUID());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> cameraSessionService.getSession(userId, sessionId)
        );

        assertStatus(HttpStatus.NOT_FOUND, exception);
    }

    @Test
    void getSession_nonCameraSession_throwsNotFound() {
        mockOwnedCameraSession();
        when(session.getSource()).thenReturn(SessionSource.ROBOT);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> cameraSessionService.getSession(userId, sessionId)
        );

        assertStatus(HttpStatus.NOT_FOUND, exception);
    }

    // PAUSE SESSION

    @Test
    void pauseSession_activeSession_pausesSuccessfully() {
        mockOwnedCameraSession();

        when(session.getCurrentState()).thenReturn(FocusState.FOCUSED);


        CameraSessionResponse response =
                cameraSessionService.pauseSession(userId, sessionId);

        assertNotNull(response);

        verify(session).setLastEventAt(null);
        verify(session).setPauseStartedAt(any(LocalDateTime.class));
        verify(session).setStatus(FocusSession.SessionStatus.paused);
        verify(focusSessionRepository).save(session);
    }

    @Test
    void pauseSession_sessionNotActive_throwsConflict() {
        mockOwnedCameraSession();
        when(session.getStatus()).thenReturn(FocusSession.SessionStatus.paused);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> cameraSessionService.pauseSession(userId, sessionId)
        );

        assertStatus(HttpStatus.CONFLICT, exception);
        verify(focusSessionRepository, never()).save(any(FocusSession.class));
    }

    // RESUME SESSION

    @Test
    void resumeSession_pausedSession_resumesSuccessfully() {
        mockOwnedCameraSession();

        when(session.getStatus()).thenReturn(FocusSession.SessionStatus.paused);
        when(session.getPauseStartedAt()).thenReturn(null);

        CameraSessionResponse response =
                cameraSessionService.resumeSession(userId, sessionId);

        assertNotNull(response);

        verify(session).setPauseStartedAt(null);
        verify(session).setLastEventAt(null);
        verify(session).setStatus(FocusSession.SessionStatus.active);
        verify(focusSessionRepository).save(session);
    }

    @Test
    void resumeSession_pausedSession_updatesPausedSeconds() {
        mockOwnedCameraSession();

        LocalDateTime pauseStarted = LocalDateTime.now().minusSeconds(60);

        when(session.getStatus()).thenReturn(FocusSession.SessionStatus.paused);
        when(session.getPauseStartedAt()).thenReturn(pauseStarted);
        when(session.getPausedSeconds()).thenReturn(20);

        CameraSessionResponse response =
                cameraSessionService.resumeSession(userId, sessionId);

        assertNotNull(response);

        ArgumentCaptor<Integer> captor =
                ArgumentCaptor.forClass(Integer.class);

        verify(session).setPausedSeconds(captor.capture());

        assertTrue(captor.getValue() >= 79);

        verify(session).setStatus(FocusSession.SessionStatus.active);
        verify(focusSessionRepository).save(session);
    }

    @Test
    void resumeSession_sessionNotPaused_throwsConflict() {
        mockOwnedCameraSession();

        when(session.getStatus()).thenReturn(FocusSession.SessionStatus.active);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> cameraSessionService.resumeSession(userId, sessionId)
        );

        assertStatus(HttpStatus.CONFLICT, exception);
        verify(focusSessionRepository, never()).save(any(FocusSession.class));
    }

    // RECORD CAMERA STATE

    @Test
    void recordState_validFocusedState_recordsState() {
        mockSessionForUpdate();

        when(session.getCurrentState()).thenReturn(null);
        when(session.getLastEventAt()).thenReturn(null);

        CameraStateResponse response = cameraSessionService.recordState(
                userId, sessionId, stateRequest(FocusState.FOCUSED, 1L)
        );

        assertNotNull(response);

        verify(session).setCurrentState(FocusState.FOCUSED);
        verify(session).setLastEventAt(any(LocalDateTime.class));
        verify(session).setLastSequenceNumber(1L);
        verify(focusSessionRepository).save(session);
    }

    @Test
    void recordState_sessionIdMismatch_throwsBadRequest() {

        CameraStateRequest request = new CameraStateRequest(
                UUID.randomUUID(),
                FocusState.FOCUSED,
                System.currentTimeMillis(),
                1L,
                FocusEventType.values()[0]
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> cameraSessionService.recordState(userId, sessionId, request)
        );

        assertStatus(HttpStatus.BAD_REQUEST, exception);

        verify(focusSessionRepository, never())
                .findByIdForUpdate(any(UUID.class));
    }

    @Test
    void recordState_sessionNotFound_throwsNotFound() {
        when(focusSessionRepository.findByIdForUpdate(sessionId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> cameraSessionService.recordState(
                        userId, sessionId,
                        stateRequest(FocusState.FOCUSED, 1L)
                )
        );

        assertStatus(HttpStatus.NOT_FOUND, exception);
    }

    @Test
    void recordState_differentOwner_throwsNotFound() {
        mockSessionForUpdate();
        when(user.getId()).thenReturn(UUID.randomUUID());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> cameraSessionService.recordState(
                        userId, sessionId,
                        stateRequest(FocusState.FOCUSED, 1L)
                )
        );

        assertStatus(HttpStatus.NOT_FOUND, exception);
        verify(focusSessionRepository, never()).save(any(FocusSession.class));
    }

    @Test
    void recordState_sessionNotActive_returnsWithoutSaving() {
        mockSessionForUpdate();

        when(session.getStatus()).thenReturn(FocusSession.SessionStatus.paused);
        when(session.getCurrentState()).thenReturn(FocusState.FOCUSED);
        when(session.getLastSequenceNumber()).thenReturn(5L);

        CameraStateResponse response = cameraSessionService.recordState(
                userId, sessionId,
                stateRequest(FocusState.DISTRACTED, 6L)
        );

        assertNotNull(response);
        verify(focusSessionRepository, never()).save(any(FocusSession.class));
    }

    @Test
    void recordState_staleSequenceNumber_ignoresReport() {
        mockSessionForUpdate();

        when(session.getLastSequenceNumber()).thenReturn(5L);
        when(session.getCurrentState()).thenReturn(FocusState.FOCUSED);

        CameraStateResponse response = cameraSessionService.recordState(
                userId, sessionId,
                stateRequest(FocusState.DISTRACTED, 5L)
        );

        assertNotNull(response);

        verify(focusSessionRepository, never()).save(any(FocusSession.class));
        verify(session, never()).setCurrentState(any(FocusState.class));
    }

    @Test
    void recordState_stateChangesToDistracted_incrementsDistractionCount() {
        mockSessionForUpdate();

        when(session.getCurrentState()).thenReturn(FocusState.FOCUSED);
        when(session.getLastEventAt()).thenReturn(null);
        when(session.getDistractionCount()).thenReturn(2);

        cameraSessionService.recordState(
                userId, sessionId,
                stateRequest(FocusState.DISTRACTED, 1L)
        );

        verify(session).setDistractionCount(3);
        verify(session).setCurrentState(FocusState.DISTRACTED);
        verify(focusSessionRepository).save(session);
    }

    @Test
    void recordState_stateChangesToAway_incrementsAwayCount() {
        mockSessionForUpdate();

        when(session.getCurrentState()).thenReturn(FocusState.FOCUSED);
        when(session.getLastEventAt()).thenReturn(null);
        when(session.getAwayCount()).thenReturn(1);

        cameraSessionService.recordState(
                userId, sessionId,
                stateRequest(FocusState.AWAY, 1L)
        );

        verify(session).setAwayCount(2);
        verify(session).setCurrentState(FocusState.AWAY);
        verify(focusSessionRepository).save(session);
    }

    // END SESSION

    @Test
    void endSession_activeSession_completesAndAwardsCoins() {
        mockSessionForUpdate();

        LocalDateTime startTime = LocalDateTime.now().minusSeconds(120);

        when(session.getStatus()).thenReturn(FocusSession.SessionStatus.active);
        when(session.getStartTime()).thenReturn(startTime);
        when(session.getCurrentState()).thenReturn(null);
        when(session.getPausedSeconds()).thenReturn(0);
        when(session.getFocusedSeconds()).thenReturn(60);


        CameraSessionEndResponse response =
                cameraSessionService.endSession(userId, sessionId);

        assertNotNull(response);

        verify(session).setEndTime(any(LocalDateTime.class));
        verify(session).setDurationSeconds(intThat(value -> value >= 120));
        verify(session).setFocusedPercentage(anyFloat());
        verify(session).setRewardEarned(1);
        verify(session).setStatus(FocusSession.SessionStatus.completed);

        verify(user).setCoins(101);
        verify(focusSessionRepository).save(session);
        verify(userRepository).save(user);
    }

    @Test
    void endSession_pausedSession_completesSuccessfully() {
        mockSessionForUpdate();

        LocalDateTime startTime = LocalDateTime.now().minusSeconds(120);
        LocalDateTime pauseStarted = LocalDateTime.now().minusSeconds(10);

        when(session.getStatus()).thenReturn(FocusSession.SessionStatus.paused);
        when(session.getStartTime()).thenReturn(startTime);
        when(session.getPauseStartedAt()).thenReturn(pauseStarted);
        when(session.getPausedSeconds()).thenReturn(10);
        when(session.getFocusedSeconds()).thenReturn(60);
        when(user.getCoins()).thenReturn(100);

        CameraSessionEndResponse response =
                cameraSessionService.endSession(userId, sessionId);

        assertNotNull(response);

        verify(session).setPausedSeconds(intThat(value -> value >= 10));
        verify(session).setStatus(FocusSession.SessionStatus.completed);
        verify(session).setPauseStartedAt(null);

        verify(focusSessionRepository).save(session);
        verify(userRepository).save(user);
    }

    @Test
    void endSession_sessionNotFound_throwsNotFound() {
        when(focusSessionRepository.findByIdForUpdate(sessionId))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> cameraSessionService.endSession(userId, sessionId)
        );

        assertStatus(HttpStatus.NOT_FOUND, exception);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void endSession_differentOwner_throwsNotFound() {
        mockSessionForUpdate();
        when(user.getId()).thenReturn(UUID.randomUUID());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> cameraSessionService.endSession(userId, sessionId)
        );

        assertStatus(HttpStatus.NOT_FOUND, exception);

        verify(focusSessionRepository, never()).save(any(FocusSession.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void endSession_nonCameraSession_throwsNotFound() {
        mockSessionForUpdate();
        when(session.getSource()).thenReturn(SessionSource.ROBOT);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> cameraSessionService.endSession(userId, sessionId)
        );

        assertStatus(HttpStatus.NOT_FOUND, exception);
    }

    @Test
    void endSession_alreadyCompleted_throwsConflict() {
        mockSessionForUpdate();

        when(session.getStatus())
                .thenReturn(FocusSession.SessionStatus.completed);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> cameraSessionService.endSession(userId, sessionId)
        );

        assertStatus(HttpStatus.CONFLICT, exception);

        verify(focusSessionRepository, never()).save(any(FocusSession.class));
        verify(userRepository, never()).save(any(User.class));
    }

    // KABOOM (EXPLOSION)

    @Test
    void recordState_secondTimeAway_triggersExplosion() {
        mockSessionForUpdate();

        when(session.getCurrentState()).thenReturn(FocusState.FOCUSED);
        when(session.getLastEventAt()).thenReturn(LocalDateTime.now());
        // The count after this report: the user left the desk for the second time.
        when(session.getAwayCount()).thenReturn(2);
        when(session.isExplosionTriggered()).thenReturn(false);

        cameraSessionService.recordState(
                userId, sessionId, stateRequest(FocusState.AWAY, 5L)
        );

        verify(session).setExplosionTriggered(true);
    }

    @Test
    void recordState_fourthDistraction_triggersExplosion() {
        mockSessionForUpdate();

        when(session.getCurrentState()).thenReturn(FocusState.FOCUSED);
        when(session.getLastEventAt()).thenReturn(LocalDateTime.now());
        when(session.getDistractionCount()).thenReturn(4);
        when(session.isExplosionTriggered()).thenReturn(false);

        cameraSessionService.recordState(
                userId, sessionId, stateRequest(FocusState.DISTRACTED, 5L)
        );

        verify(session).setExplosionTriggered(true);
    }

    @Test
    void recordState_fewDistractions_doesNotExplode() {
        mockSessionForUpdate();

        when(session.getCurrentState()).thenReturn(FocusState.FOCUSED);
        when(session.getLastEventAt()).thenReturn(LocalDateTime.now());
        when(session.getDistractionCount()).thenReturn(1);
        when(session.getAwayCount()).thenReturn(0);
        when(session.isExplosionTriggered()).thenReturn(false);

        CameraStateResponse response = cameraSessionService.recordState(
                userId, sessionId, stateRequest(FocusState.DISTRACTED, 5L)
        );

        verify(session, never()).setExplosionTriggered(true);
        assertFalse(response.explosionTriggered());
    }
}