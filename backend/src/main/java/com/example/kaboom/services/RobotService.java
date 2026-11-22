package com.example.kaboom.services;

import com.example.kaboom.dtos.RobotDto;
import com.example.kaboom.dtos.RobotResponse;
import com.example.kaboom.entities.Robot;
import com.example.kaboom.entities.User;
import com.example.kaboom.repositories.RobotRepository;
import com.example.kaboom.repositories.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class RobotService {

    /** The robot counts as connected if it called the backend this recently. */
    private static final long CONNECTED_WITHIN_SECONDS = 30;

    private final RobotRepository robotRepository;
    private final UserRepository userRepository;

    public RobotService(RobotRepository robotRepository, UserRepository userRepository) {
        this.robotRepository = robotRepository;
        this.userRepository = userRepository;
    }

    public List<Robot> getUserRobots(UUID userId) {
        return robotRepository.findByUserId(userId);
    }

    public Robot selectRobot(UUID userId, UUID robotId) {
        return robotRepository.findById(robotId).orElseThrow(() -> new RuntimeException("Robot not found"));
    }

    /**
     * The user's robot for the app (My Account): name, connected, last seen.
     * Empty if the user has no robot yet. Used by GET /robots/me.
     */
    @Transactional(readOnly = true)
    public Optional<RobotDto> getRobotForUser(UUID userId) {
        return robotRepository.findByUserId(userId)
                .stream()
                .findFirst()
                .map(RobotService::toDto);
    }

    /**
     * The user's robot as id + expression + connection state (from the pairing work).
     * Throws 404 when the user or the robot does not exist.
     */
    @Transactional(readOnly = true)
    public RobotResponse getRobotForUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        Robot robot = robotRepository.findByUserId(user.getId()).stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No robot is associated with the user"));

        return new RobotResponse(
                robot.getId(), robot.getCurrentExpression(), robot.getConnectionState());
    }

    /** Public so controllers can answer with a RobotDto instead of the Robot entity. */
    public static RobotDto toDto(Robot robot) {
        String id = robot.getId().toString().replace("-", "");
        String shortId = id.substring(id.length() - 4).toUpperCase(Locale.ROOT);

        LocalDateTime lastSeen = robot.getLastSeenAt();
        boolean connected = lastSeen != null
                && lastSeen.isAfter(LocalDateTime.now().minusSeconds(CONNECTED_WITHIN_SECONDS));

        return new RobotDto(
                robot.getId(),
                "FocusRobot - " + shortId,
                connected,
                lastSeen,
                robot.getCurrentExpression() == null ? null : robot.getCurrentExpression().name());
    }
}
