package com.example.kaboom.dtos;

import com.example.kaboom.entities.Robot;
import com.example.kaboom.enums.FocusState;

public record FocusStateResponse(
        String message,
        Robot.Expression robotExpression,
        long acceptedSequenceNumber) {}