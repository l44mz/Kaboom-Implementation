package com.example.kaboom.controllers;

import com.example.kaboom.entities.StateLog;
import com.example.kaboom.services.StateLogService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/test/state-logs")
public class StateLogTestController {

    private final StateLogService stateLogService;

    public StateLogTestController(StateLogService stateLogService) {
        this.stateLogService = stateLogService;
    }

    @PostMapping("/{sessionId}")
    public StateLogTestResponse recordState(
            @PathVariable UUID sessionId,
            @RequestParam StateLog.State state) {

        StateLog log = stateLogService.recordState(sessionId, state);

        return new StateLogTestResponse(
                log.getId(),
                log.getState().name(),
                log.getStartedAt(),
                log.getEndedAt()
        );
    }

    public record StateLogTestResponse(
            UUID id,
            String state,
            LocalDateTime startedAt,
            LocalDateTime endedAt
    ) {}
}