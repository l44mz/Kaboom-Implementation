package com.example.kaboom.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * The user's robot, as sent to the app.
 * (The Robot entity itself is never returned, because it holds the whole User.)
 *
 * @param name      a short display name, e.g. "FocusRobot - A4F2"
 * @param connected true when the robot talked to the backend in the last 30 seconds
 */
public record RobotDto(
        UUID id,
        String name,
        boolean connected,
        LocalDateTime lastSeenAt,
        String currentExpression) {}
