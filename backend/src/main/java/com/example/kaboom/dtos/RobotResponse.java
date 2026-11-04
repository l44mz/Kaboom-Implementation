package com.example.kaboom.dtos;

import com.example.kaboom.entities.Robot;

import java.util.UUID;

public record RobotResponse(
        UUID id,
        Robot.Expression currentExpression,
        Robot.ConnectionState connectionState) {}
