package com.example.kaboom.controllers;

import com.example.kaboom.dtos.CreateSessionRequest;
import com.example.kaboom.dtos.EndSessionResponse;
import com.example.kaboom.dtos.SessionResponse;
import com.example.kaboom.services.FocusSessionService;
import com.example.kaboom.services.UserServices;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/focus-sessions")
@Tag(name = "Focus Session", description = "Session time")
public class FocusSessionController {

    private final FocusSessionService focusSessionService;
    private final UserServices userServices;

    public FocusSessionController(FocusSessionService focusSessionService,
                                  UserServices userServices) {
        this.focusSessionService = focusSessionService;
        this.userServices = userServices;
    }

    /** Starts a session for the logged-in user. Body: plannedDurationSeconds, robotId (optional). */
    @PostMapping("/start")
    public ResponseEntity<SessionResponse> startSession(
            Authentication authentication,
            @Valid @RequestBody CreateSessionRequest request) {
        SessionResponse response =
                focusSessionService.startSession(currentUserId(authentication), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{sessionId}/pause")
    public ResponseEntity<SessionResponse> pauseSession(
            Authentication authentication,
            @PathVariable UUID sessionId) {
        return ResponseEntity.ok(
                focusSessionService.pauseSession(currentUserId(authentication), sessionId));
    }

    @PutMapping("/{sessionId}/resume")
    public ResponseEntity<SessionResponse> resumeSession(
            Authentication authentication,
            @PathVariable UUID sessionId) {
        return ResponseEntity.ok(
                focusSessionService.resumeSession(currentUserId(authentication), sessionId));
    }

    @PutMapping("/{sessionId}/end")
    public ResponseEntity<EndSessionResponse> endSession(
            Authentication authentication,
            @PathVariable UUID sessionId) {
        return ResponseEntity.ok(
                focusSessionService.endSession(currentUserId(authentication), sessionId));
    }

    /** The logged-in user's ID, found from the email in their access token. */
    private UUID currentUserId(Authentication authentication) {
        return userServices.getByEmail(authentication.getName()).getId();
    }
}