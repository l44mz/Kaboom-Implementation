package com.example.kaboom.services;

import com.example.kaboom.dtos.RobotResponse;
import com.example.kaboom.entities.Robot;
import com.example.kaboom.entities.User;
import com.example.kaboom.repositories.RobotRepository;
import com.example.kaboom.repositories.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class RobotService {
    private final RobotRepository robotRepository;
    private final UserRepository userRepository;

    public RobotService(RobotRepository robotRepository,  UserRepository userRepository) {
        this.robotRepository = robotRepository;
        this.userRepository = userRepository;
    }

    public List<Robot> getUserRobots(UUID userId) {
        return robotRepository.findByUserId(userId);
    }

    public Robot selectRobot(UUID userId, UUID robotId) {
        return robotRepository.findById(robotId).orElseThrow(() -> new RuntimeException("Robot not found"));
    }

    @Transactional(readOnly = true)
    public RobotResponse getRobotForUser(String email) {
        User user = userRepository.findByEmail(email)          // ADAPT: your UserRepository method name
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));

        Robot robot = robotRepository.findByUserId(user.getId()).stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No robot is associated with the user"));

        return new RobotResponse(
                robot.getId(), robot.getCurrentExpression(), robot.getConnectionState());
    }
}