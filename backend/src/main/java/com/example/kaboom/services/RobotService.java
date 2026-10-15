package com.example.kaboom.services;

import com.example.kaboom.entities.Robot;
import com.example.kaboom.repositories.RobotRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class RobotService {
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
}