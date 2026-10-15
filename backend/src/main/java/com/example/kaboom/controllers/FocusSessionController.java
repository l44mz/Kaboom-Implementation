package com.example.kaboom.controllers;

import com.example.kaboom.dtos.CreateSessionRequest;
import com.example.kaboom.dtos.EndSessionResponse;
import com.example.kaboom.dtos.SessionResponse;
import com.example.kaboom.services.FocusSessionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/focus-sessions")
public class FocusSessionController {

    private final FocusSessionService focusSessionService;

    public FocusSessionController(FocusSessionService focusSessionService) {
        this.focusSessionService = focusSessionService;
    }

    @PostMapping("/start")
    public ResponseEntity<SessionResponse> startSession(
            @RequestParam UUID userId,
            @Valid @RequestBody CreateSessionRequest request) {
        SessionResponse response = focusSessionService.startSession(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{sessionId}/pause")
    public ResponseEntity<SessionResponse> pauseSession(
            @RequestParam UUID userId,
            @PathVariable UUID sessionId) {
        SessionResponse response = focusSessionService.pauseSession(userId, sessionId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{sessionId}/resume")
    public ResponseEntity<SessionResponse> resumeSession(
            @RequestParam UUID userId,
            @PathVariable UUID sessionId) {
        SessionResponse response = focusSessionService.resumeSession(userId, sessionId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{sessionId}/end")
    public ResponseEntity<EndSessionResponse> endSession(
            @RequestParam UUID userId,
            @PathVariable UUID sessionId) {
        EndSessionResponse response = focusSessionService.endSession(userId, sessionId);
        return ResponseEntity.ok(response);
    }
}