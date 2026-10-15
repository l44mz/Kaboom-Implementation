package com.example.kaboom.controllers;

import com.example.kaboom.entities.Robot;
import com.example.kaboom.services.RobotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/robots")
public class RobotController {
    private final RobotService robotService;
    public RobotController(RobotService robotService) { this.robotService = robotService; }
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Robot>> getUserRobots(@PathVariable UUID userId) { return ResponseEntity.ok(robotService.getUserRobots(userId)); }
    @PostMapping("/select")
    public ResponseEntity<Robot> selectRobot(@RequestParam UUID userId, @RequestParam UUID robotId) {
        return ResponseEntity.ok(robotService.selectRobot(userId, robotId));
    }
}