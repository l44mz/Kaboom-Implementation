package com.example.kaboom.controllers;

import com.example.kaboom.dtos.FocusStateRequest;
import com.example.kaboom.enums.FocusEventType;
import com.example.kaboom.enums.FocusState;
import com.example.kaboom.entities.FocusSession;
import com.example.kaboom.entities.Robot;
import com.example.kaboom.entities.User;
import com.example.kaboom.repositories.FocusSessionRepository;
import com.example.kaboom.repositories.RobotRepository;
import com.example.kaboom.repositories.UserRepository;
import com.example.kaboom.services.JwtServices;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import java.util.Map;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.jpa.hibernate.ddl-auto=create-drop"
        }
)
class RobotStompIntegrationTest {

    @Container
    static final MySQLContainer mysql =
            new MySQLContainer("mysql:8.4");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name",
                () -> "com.mysql.cj.jdbc.Driver");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RobotRepository robotRepository;

    @Autowired
    private FocusSessionRepository focusSessionRepository;

    @Autowired
    private JwtServices jwtServices;

    private StompSession stompSession;

    @AfterEach
    void cleanup() {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
        }
    }

    @Test
    void robotShouldSendFocusStateThroughWebSocket() throws Exception {

        // 1. Create a user
        User user = new User(
                "Integration Test User",
                "robot-test-" + UUID.randomUUID() + "@example.com",
                "password"
        );

        user = userRepository.save(user);

        // 2. Create a robot
        Robot robot = new Robot(user);
        robot.setSerialNumber("TEST-" + UUID.randomUUID());

        robot = robotRepository.save(robot);

        // 3. Create an active focus session
        FocusSession focusSession =
                new FocusSession(user, robot, 1800);

        focusSession.setStartTime(LocalDateTime.now());
        focusSession.setCurrentState(FocusState.FOCUSED);
        focusSession.setLastEventAt(LocalDateTime.now());

        focusSession = focusSessionRepository.save(focusSession);

        UUID robotId = robot.getId();
        UUID sessionId = focusSession.getId();

        // 4. Generate a real device JWT
        String deviceToken =
                jwtServices.generateDeviceAccessToken(robotId);

        // 5. Create the STOMP client
        ObjectMapper objectMapper = new ObjectMapper();

        WebSocketStompClient stompClient =
                new WebSocketStompClient(
                        new StandardWebSocketClient()
                );

        stompClient.setMessageConverter(
                new JacksonJsonMessageConverter()
        );

        // 6. Connect to the WebSocket endpoint
        StompHeaders connectHeaders = new StompHeaders();

        connectHeaders.add(
                "Authorization",
                "Bearer " + deviceToken
        );

        stompSession = stompClient.connectAsync(
                "ws://localhost:" + port + "/ws",
                new WebSocketHttpHeaders(),
                connectHeaders,
                new StompSessionHandlerAdapter() {
                }
        ).get(5, TimeUnit.SECONDS);

        assertTrue(stompSession.isConnected());

        // 7. Subscribe to the response queue
        CompletableFuture<Map> responseFuture =
                new CompletableFuture<>();

        stompSession.subscribe(
                "/user/queue/expression",
                new StompFrameHandler() {

                    @Override
                    public java.lang.reflect.Type getPayloadType(
                            StompHeaders headers) {
                        return Map.class;
                    }

                    @Override
                    public void handleFrame(
                            StompHeaders headers,
                            Object payload) {
                        responseFuture.complete((Map) payload);
                    }
                }
        );

        // 8. Prepare the focus-state message
        FocusStateRequest request = new FocusStateRequest(
                sessionId,
                FocusState.DISTRACTED,
                System.currentTimeMillis(),
                1L,
                FocusEventType.STATE_CHANGE
        );

        StompHeaders sendHeaders = new StompHeaders();
        sendHeaders.setDestination("/app/focus-states");
        sendHeaders.setContentType(MediaType.APPLICATION_JSON);

        stompSession.send(sendHeaders, request);



        // 10. Wait for the server response
        Map response = responseFuture.get(5, TimeUnit.SECONDS);

        assertEquals("State recorded", response.get("message"));

        // 11. Verify the database was updated
        FocusSession updatedSession =
                focusSessionRepository.findById(sessionId)
                        .orElseThrow();

        Robot updatedRobot =
                robotRepository.findById(robotId)
                        .orElseThrow();

        assertEquals(
                FocusState.DISTRACTED,
                updatedSession.getCurrentState()
        );

        assertEquals(
                1L,
                updatedSession.getLastSequenceNumber()
        );

        assertEquals(
                1,
                updatedSession.getDistractionCount()
        );

        assertEquals(
                Robot.Expression.distressed,
                updatedRobot.getCurrentExpression()
        );

        assertEquals(
                Robot.ConnectionState.connected,
                updatedRobot.getConnectionState()
        );
    }

    @Test
    void robotShouldNotUpdateAnotherRobotsSession() throws Exception {

        // Create the first user and robot (authenticated robot)
        User user1 = userRepository.save(new User(
                "Robot One User",
                "robot-one-" + UUID.randomUUID() + "@example.com",
                "password"
        ));

        Robot robot1 = new Robot(user1);
        robot1.setSerialNumber("ROBOT-ONE-" + UUID.randomUUID());
        robot1 = robotRepository.save(robot1);

        // Create the second user and robot (owns the session)
        User user2 = userRepository.save(new User(
                "Robot Two User",
                "robot-two-" + UUID.randomUUID() + "@example.com",
                "password"
        ));

        Robot robot2 = new Robot(user2);
        robot2.setSerialNumber("ROBOT-TWO-" + UUID.randomUUID());
        robot2 = robotRepository.save(robot2);

        // Create a session belonging to robot2
        FocusSession session = new FocusSession(user2, robot2, 1800);
        session.setStartTime(LocalDateTime.now());
        session.setCurrentState(FocusState.FOCUSED);
        session.setLastEventAt(LocalDateTime.now());

        session = focusSessionRepository.save(session);

        // Generate a valid JWT for robot1
        String token = jwtServices.generateDeviceAccessToken(robot1.getId());

        // Create STOMP client
        WebSocketStompClient client =
                new WebSocketStompClient(new StandardWebSocketClient());

        client.setMessageConverter(new JacksonJsonMessageConverter());

        // Connect as robot1
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + token);

        stompSession = client.connectAsync(
                "ws://localhost:" + port + "/ws",
                new WebSocketHttpHeaders(),
                connectHeaders,
                new StompSessionHandlerAdapter() {}
        ).get(5, TimeUnit.SECONDS);

        // Subscribe to error queue
        CompletableFuture<Map> errorFuture = new CompletableFuture<>();

        stompSession.subscribe(
                "/user/queue/errors",
                new StompFrameHandler() {

                    @Override
                    public java.lang.reflect.Type getPayloadType(
                            StompHeaders headers) {
                        return Map.class;
                    }

                    @Override
                    public void handleFrame(
                            StompHeaders headers,
                            Object payload) {
                        errorFuture.complete((Map) payload);
                    }
                }
        );

        // Try to update robot2's session using robot1's token
        FocusStateRequest request = new FocusStateRequest(
                session.getId(),
                FocusState.DISTRACTED,
                System.currentTimeMillis(),
                1L,
                FocusEventType.STATE_CHANGE
        );

        StompHeaders sendHeaders = new StompHeaders();
        sendHeaders.setDestination("/app/focus-states");
        sendHeaders.setContentType(MediaType.APPLICATION_JSON);

        stompSession.send(sendHeaders, request);

        // Wait for the error response
        Map error = errorFuture.get(5, TimeUnit.SECONDS);

        assertNotNull(error.get("error"));

        // Verify the session was not modified
        FocusSession unchangedSession =
                focusSessionRepository.findById(session.getId())
                        .orElseThrow();

        assertEquals(
                FocusState.FOCUSED,
                unchangedSession.getCurrentState()
        );

        assertEquals(
                0L,
                unchangedSession.getLastSequenceNumber()
        );
    }
}
