package com.example.kaboom.dtos;

import com.example.kaboom.enums.FocusState;

public record FocusStateResponse(
        String message,
        FocusState robotExpression,
        long acceptedSequenceNumber) {}