package com.example.kaboom.controllers;

import com.example.kaboom.dtos.FocusStateRequest;
import com.example.kaboom.entities.FocusSession;
import com.example.kaboom.entities.Robot;
import com.example.kaboom.entities.User;
import com.example.kaboom.enums.FocusEventType;
import com.example.kaboom.enums.FocusState;
import com.example.kaboom.repositories.FocusSessionRepository;
import com.example.kaboom.repositories.RobotRepository;
import com.example.kaboom.repositories.UserRepository;
import com.example.kaboom.services.FocusSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Consumer;
import static org.junit.jupiter.api.Assertions.*;

class FocusSessionIntegrationTest extends AbstractIntegrationTest {

    private static final String AUTH_URL = "/api/v1/auth";
    private static final String FOCUS_URL = "/api/v1/focus-sessions";
    private static final String PASSWORD = "SecurePass123!";

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private RobotRepository robotRepository;
    @Autowired private FocusSessionRepository focusSessionRepository;
    @Autowired private FocusSessionService focusSessionService;


    // ---------- Start ----------

    @Test
    void shouldStartSession() {

        String token = login(registerUser());

        ResponseEntity<String> response = start(token);

        assertStatus(HttpStatus.CREATED, response);
        UUID id = sessionId(response);
        assertEquals(FocusSession.SessionStatus.active,
                focusSessionRepository.findById(id).orElseThrow().getStatus());
    }

    @Test
    void shouldRequireAuthenticationToStart() {

        assertStatus(HttpStatus.UNAUTHORIZED, start(null));
    }

    @Test
    void shouldRejectInvalidPlannedDuration() {

        String token = login(registerUser());

        ResponseEntity<String> response = send(HttpMethod.POST, FOCUS_URL + "/start",
                "{\"plannedDurationSeconds\": -5}", token);

        assertStatus(HttpStatus.BAD_REQUEST, response);
    }

    @Test
    void shouldRejectSecondOpenSession() {

        String token = login(registerUser());

        assertStatus(HttpStatus.CREATED, start(token));
        assertStatus(HttpStatus.CONFLICT, start(token));
    }

    @Test
    void shouldReturn404ForUnknownRobotOnStart() {

        String token = login(registerUser());

        assertStatus(HttpStatus.NOT_FOUND, startWithRobot(token, UUID.randomUUID()));
    }

    @Test
    void shouldNotAllowStartingWithAnotherUsersRobot() {

        String emailA = registerUser();
        String emailB = registerUser();
        Robot robotOfA = createRobot(emailA);
        String tokenB = login(emailB);

        assertStatus(HttpStatus.NOT_FOUND, startWithRobot(tokenB, robotOfA.getId()));
    }

    @Test
    void shouldAttachUsersOwnRobotWhenNoRobotIdGiven() {

        String email = registerUser();
        Robot robot = createRobot(email);
        String token = login(email);

        ResponseEntity<String> response = start(token);

        assertStatus(HttpStatus.CREATED, response);
        assertTrue(response.getBody().contains(robot.getId().toString()),
                "robot id missing from: " + response.getBody());
    }

    @Test
    void shouldAllowNewSessionAfterEndingPreviousOne() {

        String token = login(registerUser());
        UUID id = sessionId(start(token));

        assertStatus(HttpStatus.OK, end(token, id));
        assertStatus(HttpStatus.CREATED, start(token));
    }


    // ---------- Read / ownership ----------

    @Test
    void shouldReturnOwnSession() {

        String token = login(registerUser());
        UUID id = sessionId(start(token));

        assertStatus(HttpStatus.OK, send(HttpMethod.GET, FOCUS_URL + "/" + id, null, token));
    }

    @Test
    void shouldReturn404ForUnknownSession() {

        String token = login(registerUser());

        assertStatus(HttpStatus.NOT_FOUND,
                send(HttpMethod.GET, FOCUS_URL + "/" + UUID.randomUUID(), null, token));
    }

    @Test
    void shouldNotAllowAnotherUserToAccessSession() {

        String tokenA = login(registerUser());
        String tokenB = login(registerUser());
        UUID id = sessionId(start(tokenA));

        assertStatus(HttpStatus.NOT_FOUND, send(HttpMethod.GET, FOCUS_URL + "/" + id, null, tokenB));
        assertStatus(HttpStatus.NOT_FOUND, pause(tokenB, id));
        assertStatus(HttpStatus.NOT_FOUND, resume(tokenB, id));
        assertStatus(HttpStatus.NOT_FOUND, end(tokenB, id));

        assertEquals(FocusSession.SessionStatus.active,
                focusSessionRepository.findById(id).orElseThrow().getStatus());
    }


    // ---------- Pause / resume ----------

    @Test
    void shouldPauseActiveSession() {

        String token = login(registerUser());
        UUID id = sessionId(start(token));

        assertStatus(HttpStatus.OK, pause(token, id));
        assertEquals(FocusSession.SessionStatus.paused,
                focusSessionRepository.findById(id).orElseThrow().getStatus());
    }

    @Test
    void shouldRejectPausingAPausedSession() {

        String token = login(registerUser());
        UUID id = sessionId(start(token));
        pause(token, id);

        assertStatus(HttpStatus.CONFLICT, pause(token, id));
    }

    @Test
    void shouldRejectResumingASessionThatIsNotPaused() {

        String token = login(registerUser());
        UUID id = sessionId(start(token));

        assertStatus(HttpStatus.CONFLICT, resume(token, id));
    }

    @Test
    void shouldAccumulatePausedSecondsOnResume() {

        String token = login(registerUser());
        UUID id = sessionId(start(token));
        pause(token, id);
        update(id, s -> s.setPauseStartedAt(LocalDateTime.now().minusSeconds(100)));

        assertStatus(HttpStatus.OK, resume(token, id));

        FocusSession session = focusSessionRepository.findById(id).orElseThrow();
        assertEquals(FocusSession.SessionStatus.active, session.getStatus());
        assertTrue(session.getPausedSeconds() >= 100,
                "pausedSeconds was " + session.getPausedSeconds());
    }


    // ---------- End and rewards ----------

    @Test
    void shouldAwardOneCoinPerFocusedMinute() {

        String email = registerUser();
        String token = login(email);
        UUID id = sessionId(start(token));
        update(id, s -> {
            s.setStartTime(LocalDateTime.now().minusMinutes(10));
            s.setFocusedSeconds(300);
        });

        assertStatus(HttpStatus.OK, end(token, id));

        assertEquals(105, coinsOf(email));
        FocusSession session = focusSessionRepository.findById(id).orElseThrow();
        assertEquals(FocusSession.SessionStatus.completed, session.getStatus());
        assertEquals(5, session.getRewardEarned());
    }

    @Test
    void shouldCapRewardAtThirtyCoins() {

        String email = registerUser();
        String token = login(email);
        UUID id = sessionId(start(token));
        update(id, s -> {
            s.setStartTime(LocalDateTime.now().minusHours(2));
            s.setFocusedSeconds(3600);
        });

        assertStatus(HttpStatus.OK, end(token, id));

        assertEquals(130, coinsOf(email));
    }

    @Test
    void shouldAwardNothingWhenNoFocusedTime() {

        String email = registerUser();
        String token = login(email);
        UUID id = sessionId(start(token));

        assertStatus(HttpStatus.OK, end(token, id));

        assertEquals(100, coinsOf(email));
    }

    @Test
    void shouldNotCountMoreFocusedTimeThanSessionDuration() {

        String email = registerUser();
        String token = login(email);
        UUID id = sessionId(start(token));
        // claims 1 hour focused but the session only lasted 2 minutes
        update(id, s -> {
            s.setStartTime(LocalDateTime.now().minusMinutes(2));
            s.setFocusedSeconds(3600);
        });

        assertStatus(HttpStatus.OK, end(token, id));

        assertTrue(coinsOf(email) <= 102, "coins were " + coinsOf(email));
    }

    @Test
    void shouldNotAwardCoinsTwiceWhenEndedTwice() {

        String email = registerUser();
        String token = login(email);
        UUID id = sessionId(start(token));
        update(id, s -> {
            s.setStartTime(LocalDateTime.now().minusMinutes(10));
            s.setFocusedSeconds(300);
        });

        assertStatus(HttpStatus.OK, end(token, id));
        assertStatus(HttpStatus.CONFLICT, end(token, id));

        assertEquals(105, coinsOf(email));
    }

    @Test
    void shouldEndAPausedSession() {

        String token = login(registerUser());
        UUID id = sessionId(start(token));
        pause(token, id);
        update(id, s -> s.setPauseStartedAt(LocalDateTime.now().minusSeconds(60)));

        assertStatus(HttpStatus.OK, end(token, id));

        FocusSession session = focusSessionRepository.findById(id).orElseThrow();
        assertEquals(FocusSession.SessionStatus.completed, session.getStatus());
        assertTrue(session.getPausedSeconds() >= 60);
    }


    // ---------- Concurrency (these look for real race conditions) ----------

    @Test
    void shouldAwardCoinsOnlyOnceWhenEndedConcurrently() throws Exception {

        String email = registerUser();
        String token = login(email);
        UUID id = sessionId(start(token));
        update(id, s -> {
            s.setStartTime(LocalDateTime.now().minusMinutes(10));
            s.setFocusedSeconds(300);
        });

        int[] statuses = runConcurrently(() -> end(token, id), () -> end(token, id));

        assertEquals(1, count(statuses, 200), "statuses: " + Arrays.toString(statuses));
        assertEquals(1, count(statuses, 409), "statuses: " + Arrays.toString(statuses));
        assertEquals(105, coinsOf(email));
    }

    @Test
    void shouldAllowOnlyOneOpenSessionWhenStartedConcurrently() throws Exception {

        String token = login(registerUser());

        int[] statuses = runConcurrently(() -> start(token), () -> start(token));

        assertEquals(1, count(statuses, 201), "statuses: " + Arrays.toString(statuses));
        assertEquals(1, count(statuses, 409), "statuses: " + Arrays.toString(statuses));
    }


    // ---------- Robot state reports (service level) ----------

    @Test
    void shouldRecordStateFromOwningRobot() {

        String email = registerUser();
        Robot robot = createRobot(email);
        UUID id = sessionId(start(login(email)));

        report(robot.getId(), id, FocusState.FOCUSED, 1);

        FocusSession session = focusSessionRepository.findById(id).orElseThrow();
        assertEquals(FocusState.FOCUSED, session.getCurrentState());
        assertEquals(1, session.getLastSequenceNumber());
    }

    @Test
    void shouldIgnoreStaleSequenceNumbers() {

        String email = registerUser();
        Robot robot = createRobot(email);
        UUID id = sessionId(start(login(email)));

        report(robot.getId(), id, FocusState.FOCUSED, 5);
        report(robot.getId(), id, FocusState.DISTRACTED, 3);   // older: must be ignored
        report(robot.getId(), id, FocusState.DISTRACTED, 5);   // duplicate: must be ignored

        FocusSession session = focusSessionRepository.findById(id).orElseThrow();
        assertEquals(FocusState.FOCUSED, session.getCurrentState());
        assertEquals(5, session.getLastSequenceNumber());
        assertEquals(0, session.getDistractionCount());
    }

    @Test
    void shouldRejectReportsFromAnotherRobot() {

        String emailA = registerUser();
        String emailB = registerUser();
        createRobot(emailA);
        Robot robotB = createRobot(emailB);
        UUID id = sessionId(start(login(emailA)));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> report(robotB.getId(), id, FocusState.FOCUSED, 1));

        assertEquals(403, ex.getStatusCode().value());
    }

    @Test
    void shouldIgnoreReportsWhileSessionIsPaused() {

        String email = registerUser();
        Robot robot = createRobot(email);
        String token = login(email);
        UUID id = sessionId(start(token));
        pause(token, id);

        report(robot.getId(), id, FocusState.DISTRACTED, 1);

        FocusSession session = focusSessionRepository.findById(id).orElseThrow();
        assertNull(session.getCurrentState());
        assertEquals(0, session.getDistractionCount());
    }

    @Test
    void shouldCountOnlyNewTransitionsIntoDistracted() {

        String email = registerUser();
        Robot robot = createRobot(email);
        UUID id = sessionId(start(login(email)));

        report(robot.getId(), id, FocusState.DISTRACTED, 1);
        report(robot.getId(), id, FocusState.DISTRACTED, 2);   // same state: not a new distraction
        report(robot.getId(), id, FocusState.FOCUSED, 3);
        report(robot.getId(), id, FocusState.DISTRACTED, 4);

        assertEquals(2, focusSessionRepository.findById(id).orElseThrow().getDistractionCount());
    }

    @Test
    void shouldTriggerExplosionAfterTwoAwayEvents() {

        String email = registerUser();
        Robot robot = createRobot(email);
        UUID id = sessionId(start(login(email)));

        report(robot.getId(), id, FocusState.AWAY, 1);
        assertFalse(focusSessionRepository.findById(id).orElseThrow().isExplosionTriggered());

        report(robot.getId(), id, FocusState.FOCUSED, 2);
        report(robot.getId(), id, FocusState.AWAY, 3);

        assertTrue(focusSessionRepository.findById(id).orElseThrow().isExplosionTriggered());
    }

    @Test
    void shouldCapTimeCreditedAfterALongGapBetweenReports() {

        String email = registerUser();
        Robot robot = createRobot(email);
        UUID id = sessionId(start(login(email)));
        report(robot.getId(), id, FocusState.FOCUSED, 1);
        // robot went silent for 10 minutes (e.g. Wi-Fi drop)
        update(id, s -> s.setLastEventAt(LocalDateTime.now().minusMinutes(10)));

        report(robot.getId(), id, FocusState.DISTRACTED, 2);

        int focused = focusSessionRepository.findById(id).orElseThrow().getFocusedSeconds();
        assertTrue(focused <= 31, "focusedSeconds was " + focused + " (gap cap is 30s)");
    }


    // ---------- History ----------

    @Test
    void shouldRejectInvalidHistoryRange() {

        String token = login(registerUser());

        assertStatus(HttpStatus.BAD_REQUEST,
                send(HttpMethod.GET, FOCUS_URL + "/history?range=bogus", null, token));
    }

    @Test
    void shouldIncludeFinishedSessionsOnlyInOwnersHistory() {

        String tokenA = login(registerUser());
        String tokenB = login(registerUser());
        UUID id = sessionId(start(tokenA));
        end(tokenA, id);

        ResponseEntity<String> historyA =
                send(HttpMethod.GET, FOCUS_URL + "/history?range=day", null, tokenA);
        ResponseEntity<String> historyB =
                send(HttpMethod.GET, FOCUS_URL + "/history?range=day", null, tokenB);

        assertStatus(HttpStatus.OK, historyA);
        assertTrue(historyA.getBody().contains(id.toString()), historyA.getBody());
        assertStatus(HttpStatus.OK, historyB);
        assertFalse(historyB.getBody().contains(id.toString()));
    }

    @Test
    void shouldRequireAuthenticationForHistory() {

        assertStatus(HttpStatus.UNAUTHORIZED,
                send(HttpMethod.GET, FOCUS_URL + "/history", null, null));
    }


    // ---------- Helpers ----------

    private void assertStatus(HttpStatus expected, ResponseEntity<String> response) {
        assertEquals(expected, response.getStatusCode(), response.getBody());
    }

    private ResponseEntity<String> start(String token) {
        return send(HttpMethod.POST, FOCUS_URL + "/start",
                "{\"plannedDurationSeconds\": 1500}", token);
    }

    private ResponseEntity<String> startWithRobot(String token, UUID robotId) {
        return send(HttpMethod.POST, FOCUS_URL + "/start",
                "{\"plannedDurationSeconds\": 1500, \"robotId\": \"" + robotId + "\"}", token);
    }

    private ResponseEntity<String> pause(String token, UUID id) {
        return send(HttpMethod.PUT, FOCUS_URL + "/" + id + "/pause", null, token);
    }

    private ResponseEntity<String> resume(String token, UUID id) {
        return send(HttpMethod.PUT, FOCUS_URL + "/" + id + "/resume", null, token);
    }

    private ResponseEntity<String> end(String token, UUID id) {
        return send(HttpMethod.PUT, FOCUS_URL + "/" + id + "/end", null, token);
    }

    private UUID sessionId(ResponseEntity<String> response) {
        assertEquals(HttpStatus.CREATED, response.getStatusCode(), response.getBody());
        return UUID.fromString(extractJsonValue(response.getBody(), "id"));
    }

    private void report(UUID robotId, UUID sessionId, FocusState state, long sequenceNumber) {
        focusSessionService.recordState(
                robotId,
                new FocusStateRequest(
                        sessionId,
                        state,
                        System.currentTimeMillis(),
                        sequenceNumber,
                        FocusEventType.STATE_CHANGE
                )
        );
    }

    private Robot createRobot(String email) {

        User user = userRepository.findByEmail(email).orElseThrow();
        Robot robot = new Robot();
        robot.setUser(user);
        robot.setSerialNumber("SN-" + UUID.randomUUID());
        return robotRepository.save(robot);
    }

    private void update(UUID sessionId, Consumer<FocusSession> change) {
        FocusSession session = focusSessionRepository.findById(sessionId).orElseThrow();
        change.accept(session);
        focusSessionRepository.save(session);
    }

    private int coinsOf(String email) {
        return userRepository.findByEmail(email).orElseThrow().getCoins();
    }

    @SafeVarargs
    private int[] runConcurrently(Callable<ResponseEntity<String>>... calls) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(calls.length);
        CountDownLatch startSignal = new CountDownLatch(1);
        try {
            @SuppressWarnings("unchecked")
            Future<ResponseEntity<String>>[] futures = new Future[calls.length];
            for (int i = 0; i < calls.length; i++) {
                Callable<ResponseEntity<String>> call = calls[i];
                futures[i] = pool.submit(() -> {
                    startSignal.await();
                    return call.call();
                });
            }
            startSignal.countDown();

            int[] statuses = new int[calls.length];
            for (int i = 0; i < calls.length; i++) {
                statuses[i] = futures[i].get(30, TimeUnit.SECONDS).getStatusCode().value();
            }
            return statuses;
        } finally {
            pool.shutdownNow();
        }
    }

    private long count(int[] statuses, int status) {
        return Arrays.stream(statuses).filter(s -> s == status).count();
    }

    private String registerUser() {
        String email = "test-" + UUID.randomUUID() + "@example.com";
        String body = """
                {
                    "name": "Test User",
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(email, PASSWORD);

        assertStatus(HttpStatus.CREATED,
                send(HttpMethod.POST, AUTH_URL + "/register", body, null));
        return email;
    }

    private String login(String email) {
        String body = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(email, PASSWORD);

        ResponseEntity<String> response = send(HttpMethod.POST, AUTH_URL + "/login", body, null);

        assertStatus(HttpStatus.OK, response);
        return extractJsonValue(response.getBody(), "accessToken");
    }

    private ResponseEntity<String> send(HttpMethod method, String url, String body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return restTemplate.exchange(url, method, new HttpEntity<>(body, headers), String.class);
    }

    private String extractJsonValue(String json, String key) {
        try {
            com.fasterxml.jackson.databind.JsonNode node =
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
            return node.get(key).asText();
        } catch (Exception e) {
            throw new RuntimeException("Could not extract " + key, e);
        }
    }
}
