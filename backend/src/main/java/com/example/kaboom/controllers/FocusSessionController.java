
package com.example.kaboom.controllers;

import com.example.kaboom.dtos.CreateSessionRequest;
import com.example.kaboom.dtos.EndSessionResponse;
import com.example.kaboom.dtos.SessionResponse;
import com.example.kaboom.services.FocusSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Handles HTTP requests for starting, pausing, resuming,
 * and ending focus sessions.
 *
 * Session business logic is delegated to FocusSessionService.
 * Access control must ensure that users can only manage their own sessions.
 */
@RestController
@RequestMapping("/sessions")
@Tag(
        name = "Focus Sessions",
        description = "Manage focus sessions, including starting, pausing, resuming, and ending sessions."
)
public class FocusSessionController {

    private final FocusSessionService focusSessionService;

    public FocusSessionController(FocusSessionService focusSessionService) {
        this.focusSessionService = focusSessionService;
    }

    @Operation(
            summary = "Start a focus session",
            description = "Creates a new focus session using the requested duration and optional robot. "
                    + "A user can have only one active or paused session at a time."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Focus session created successfully",
                    content = @Content(
                            schema = @Schema(implementation = SessionResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid authentication"),
            @ApiResponse(responseCode = "404", description = "Requested robot not found or unavailable"),
            @ApiResponse(responseCode = "409", description = "The user already has an active or paused session")
    })
    @PostMapping
    public ResponseEntity<SessionResponse> startSession(
            @RequestParam UUID userId,
            @Valid @RequestBody CreateSessionRequest request) {

        SessionResponse response =
                focusSessionService.startSession(userId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Pause a focus session",
            description = "Pauses an active session. Paused time must not be counted as focused time."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Focus session paused successfully",
                    content = @Content(
                            schema = @Schema(implementation = SessionResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Missing or invalid authentication"),
            @ApiResponse(responseCode = "404", description = "Session not found or not accessible"),
            @ApiResponse(responseCode = "409", description = "The session is not active")
    })
    @PatchMapping("/{sessionId}/pause")
    public ResponseEntity<SessionResponse> pauseSession(
            @RequestParam UUID userId,
            @PathVariable UUID sessionId) {

        SessionResponse response =
                focusSessionService.pauseSession(userId, sessionId);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Resume a focus session",
            description = "Resumes a paused session and records the elapsed pause duration."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Focus session resumed successfully",
                    content = @Content(
                            schema = @Schema(implementation = SessionResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Missing or invalid authentication"),
            @ApiResponse(responseCode = "404", description = "Session not found or not accessible"),
            @ApiResponse(responseCode = "409", description = "The session is not paused")
    })
    @PatchMapping("/{sessionId}/resume")
    public ResponseEntity<SessionResponse> resumeSession(
            @RequestParam UUID userId,
            @PathVariable UUID sessionId) {

        SessionResponse response =
                focusSessionService.resumeSession(userId, sessionId);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "End a focus session",
            description = "Completes an active or paused session, calculates the focus statistics "
                    + "and reward, and updates the user's coin balance."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Focus session completed successfully",
                    content = @Content(
                            schema = @Schema(implementation = EndSessionResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Missing or invalid authentication"),
            @ApiResponse(responseCode = "404", description = "Session not found or not accessible"),
            @ApiResponse(responseCode = "409", description = "The session has already been completed")
    })
    @PatchMapping("/{sessionId}/end")
    public ResponseEntity<EndSessionResponse> endSession(
            @RequestParam UUID userId,
            @PathVariable UUID sessionId) {

        EndSessionResponse response =
                focusSessionService.endSession(userId, sessionId);

        return ResponseEntity.ok(response);
    }
}
