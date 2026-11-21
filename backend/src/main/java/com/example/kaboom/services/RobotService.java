package com.example.kaboom.services;

import com.example.kaboom.dtos.RobotDto;
import com.example.kaboom.entities.Robot;
import com.example.kaboom.repositories.RobotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public RobotService(RobotRepository robotRepository) {
        this.robotRepository = robotRepository;
    }

    public List<Robot> getUserRobots(UUID userId) {
        return robotRepository.findByUserId(userId);
    }

    public Robot selectRobot(UUID userId, UUID robotId) {
        return robotRepository.findById(robotId).orElseThrow(() -> new RuntimeException("Robot not found"));
    }

    /** The user's robot for the app (My Account), or empty if they have none. */
    @Transactional(readOnly = true)
    public Optional<RobotDto> getRobotForUser(UUID userId) {
        return robotRepository.findByUserId(userId)
                .stream()
                .findFirst()
                .map(RobotService::toDto);
    }

    static RobotDto toDto(Robot robot) {
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
