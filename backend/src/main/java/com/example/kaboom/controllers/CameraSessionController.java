package com.example.kaboom.controllers;

import com.example.kaboom.dtos.CameraSessionEndResponse;
import com.example.kaboom.dtos.CameraSessionRequest;
import com.example.kaboom.dtos.CameraSessionResponse;
import com.example.kaboom.dtos.CameraStateRequest;
import com.example.kaboom.dtos.CameraStateResponse;
import com.example.kaboom.services.CameraSessionService;
import com.example.kaboom.services.UserServices;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/camera-sessions")
@Tag(name = "Camera Sessions", description = "Focus sessions using a device camera")
public class CameraSessionController {

    private final CameraSessionService cameraSessionService;
    private final UserServices userServices;

    public CameraSessionController(
            CameraSessionService cameraSessionService,
            UserServices userServices) {
        this.cameraSessionService = cameraSessionService;
        this.userServices = userServices;
    }

    @PostMapping("/start")
    public ResponseEntity<CameraSessionResponse> startSession(
            Authentication authentication,
            @Valid @RequestBody CameraSessionRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(
                cameraSessionService.startSession(currentUserId(authentication), request));
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<CameraSessionResponse> getSession(
            Authentication authentication,
            @PathVariable UUID sessionId) {

        return ResponseEntity.ok(
                cameraSessionService.getSession(currentUserId(authentication), sessionId));
    }

    @PostMapping("/{sessionId}/state")
    public ResponseEntity<CameraStateResponse> recordState(
            Authentication authentication,
            @PathVariable UUID sessionId,
            @Valid @RequestBody CameraStateRequest request) {

        return ResponseEntity.ok(
                cameraSessionService.recordState(
                        currentUserId(authentication), sessionId, request));
    }

    @PutMapping("/{sessionId}/pause")
    public ResponseEntity<CameraSessionResponse> pauseSession(
            Authentication authentication,
            @PathVariable UUID sessionId) {

        return ResponseEntity.ok(
                cameraSessionService.pauseSession(currentUserId(authentication), sessionId));
    }

    @PutMapping("/{sessionId}/resume")
    public ResponseEntity<CameraSessionResponse> resumeSession(
            Authentication authentication,
            @PathVariable UUID sessionId) {

        return ResponseEntity.ok(
                cameraSessionService.resumeSession(currentUserId(authentication), sessionId));
    }

    @PutMapping("/{sessionId}/end")
    public ResponseEntity<CameraSessionEndResponse> endSession(
            Authentication authentication,
            @PathVariable UUID sessionId) {

        return ResponseEntity.ok(
                cameraSessionService.endSession(currentUserId(authentication), sessionId));
    }

    private UUID currentUserId(Authentication authentication) {
        return userServices.getByEmail(authentication.getName()).getId();
    }
}
