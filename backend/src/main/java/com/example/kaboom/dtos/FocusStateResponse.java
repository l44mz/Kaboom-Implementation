package com.example.kaboom.dtos;

import com.example.kaboom.enums.FocusState;

/** The backend's reply to the robot after it reports a state. */
public record FocusStateResponse(
        String message,
        FocusState robotExpression,
        long acceptedSequenceNumber) {}
