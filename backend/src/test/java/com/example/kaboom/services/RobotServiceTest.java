package com.example.kaboom.services;

import com.example.kaboom.dtos.RobotResponse;
import com.example.kaboom.entities.Robot;
import com.example.kaboom.entities.User;
import com.example.kaboom.repositories.RobotRepository;
import com.example.kaboom.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class RobotServiceTest {

    @Mock private RobotRepository robotRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private RobotService robotService;

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

    private void assertStatus(HttpStatus expected, ResponseStatusException ex) {
        assertEquals(expected, ex.getStatusCode());
    }

    // ---------- getUserRobots ----------

    @Test
    void getUserRobots_userHasRobots_returnsRobotList() {
        UUID userId = UUID.randomUUID();
        User user = userWithId(userId);

        Robot robot1 = robotOwnedBy(UUID.randomUUID(), user);
        Robot robot2 = robotOwnedBy(UUID.randomUUID(), user);

        List<Robot> robots = List.of(robot1, robot2);

        when(robotRepository.findByUserId(userId)).thenReturn(robots);

        List<Robot> result = robotService.getUserRobots(userId);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertSame(robot1, result.get(0));
        assertSame(robot2, result.get(1));

        verify(robotRepository).findByUserId(userId);
    }

    @Test
    void getUserRobots_userHasNoRobots_returnsEmptyList() {
        UUID userId = UUID.randomUUID();
        when(robotRepository.findByUserId(userId)).thenReturn(List.of());

        List<Robot> result = robotService.getUserRobots(userId);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(robotRepository).findByUserId(userId);
    }

    // ---------- selectRobot ----------

    @Test
    void selectRobot_robotExists_returnsRobot() {
        UUID userId = UUID.randomUUID();
        UUID robotId = UUID.randomUUID();

        User user = userWithId(userId);
        Robot robot = robotOwnedBy(robotId, user);

        when(robotRepository.findById(robotId)).thenReturn(Optional.of(robot));

        Robot result = robotService.selectRobot(userId, robotId);

        assertNotNull(result);
        assertSame(robot, result);

        verify(robotRepository).findById(robotId);
    }

    @Test
    void selectRobot_robotDoesNotExist_throwsRuntimeException() {
        UUID userId = UUID.randomUUID();
        UUID robotId = UUID.randomUUID();

        when(robotRepository.findById(robotId)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> robotService.selectRobot(userId, robotId));

        assertEquals("Robot not found", ex.getMessage());

        verify(robotRepository).findById(robotId);
    }

    // ---------- getRobotForUser ----------

    @Test
    void getRobotForUser_userHasRobot_returnsRobotResponse() {
        String email = "test@example.com";
        UUID userId = UUID.randomUUID();

        User user = userWithId(userId);
        Robot robot = robotOwnedBy(UUID.randomUUID(), user);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(robotRepository.findByUserId(userId)).thenReturn(List.of(robot));

        RobotResponse result = robotService.getRobotForUser(email);

        assertNotNull(result);

        verify(userRepository).findByEmail(email);
        verify(robotRepository).findByUserId(userId);
        verify(robot).getId();
        verify(robot).getCurrentExpression();
        verify(robot).getConnectionState();
    }

    @Test
    void getRobotForUser_userDoesNotExist_throwsNotFound() {
        String email = "unknown@example.com";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> robotService.getRobotForUser(email));

        assertStatus(HttpStatus.NOT_FOUND, ex);

        verify(userRepository).findByEmail(email);
        verifyNoInteractions(robotRepository);
    }

    @Test
    void getRobotForUser_userHasNoRobot_throwsNotFound() {
        String email = "test@example.com";
        UUID userId = UUID.randomUUID();

        User user = userWithId(userId);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(robotRepository.findByUserId(userId)).thenReturn(List.of());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> robotService.getRobotForUser(email));

        assertStatus(HttpStatus.NOT_FOUND, ex);

        verify(userRepository).findByEmail(email);
        verify(robotRepository).findByUserId(userId);
    }
}

