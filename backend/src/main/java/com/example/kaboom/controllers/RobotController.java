package com.example.kaboom.controllers;

import com.example.kaboom.dtos.FocusStateRequest;
import com.example.kaboom.dtos.FocusStateResponse;
import com.example.kaboom.dtos.PairRequest;
import com.example.kaboom.dtos.PairingCodeRequest;
import com.example.kaboom.dtos.PairingCodeResponse;
import com.example.kaboom.dtos.PairingStatusRequest;
import com.example.kaboom.dtos.PairingStatusResponse;
import com.example.kaboom.dtos.RefreshDto;
import com.example.kaboom.dtos.RefreshResponseDto;
import com.example.kaboom.dtos.RobotDto;
import com.example.kaboom.dtos.SessionResponse;
import com.example.kaboom.entities.Robot;
import com.example.kaboom.services.FocusSessionService;
import com.example.kaboom.services.JwtServices;
import com.example.kaboom.services.RobotPairingService;
import com.example.kaboom.services.RobotService;
import com.example.kaboom.services.UserServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Handles requests for retrieving and selecting robots,
 * and the calls the robot itself makes during a session.
 *
 * Robot business logic is delegated to RobotService and FocusSessionService.
 * Users must only be able to access robots associated with their own accounts.
 */
@RestController
@RequestMapping("/robots")
@Tag(
        name = "Robots",
        description = "Retrieve the user's robot, manage robot selection, and robot session calls."
)
public class RobotController {

    private final RobotService robotService;
    private final FocusSessionService focusSessionService;
    private final UserServices userServices;
    private final RobotPairingService robotPairingService;
    private final JwtServices jwtServices;

    public RobotController(RobotService robotService,
                           FocusSessionService focusSessionService,
                           UserServices userServices,
                           RobotPairingService robotPairingService,
                           JwtServices jwtServices) {
        this.robotService = robotService;
        this.focusSessionService = focusSessionService;
        this.userServices = userServices;
        this.robotPairingService = robotPairingService;
        this.jwtServices = jwtServices;
    }

    /**
     * Retrieves the robot associated with the authenticated user.
     * The README specifies GET /robots/me for this operation.
     */
    @Operation(
            summary = "Get my robot",
            description = "Returns the robot associated with the authenticated user's account, "
                    + "including its current expression and connection state."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Robot retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid authentication"),
            @ApiResponse(responseCode = "404", description = "No robot is associated with the user")
    })
    @GetMapping("/me")
    public ResponseEntity<RobotDto> getMyRobot(Authentication authentication) {
        UUID userId = userServices.getByEmail(authentication.getName()).getId();

        return robotService.getRobotForUser(userId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Selects a robot for the specified user.
     * This is an additional operation and is not defined in the current README API contract.
     */
    @Operation(
            summary = "Select a robot",
            description = "Selects a robot for the user, subject to ownership and availability checks."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Robot selected successfully",
                    content = @Content(
                            schema = @Schema(implementation = Robot.class)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Missing or invalid authentication"),
            @ApiResponse(responseCode = "404", description = "Robot not found or not accessible"),
            @ApiResponse(responseCode = "409", description = "Robot selection conflicts with account rules")
    })
    @PostMapping("/select")
    public ResponseEntity<Robot> selectRobot(
            @RequestParam UUID userId,
            @RequestParam UUID robotId) {

        // TODO: Verify the authenticated user and robot ownership before selecting the robot.
        return ResponseEntity.ok(robotService.selectRobot(userId, robotId));
    }

    @Operation(summary = "Get the robot's current session",
            description = "Polled by the robot. Returns 204 when no session is active or paused.")
    @GetMapping("/me/active-session")
    public ResponseEntity<SessionResponse> getActiveSession(
            @RequestParam UUID robotId) {   // TODO: take robotId from a device token, not the client

        return focusSessionService.getActiveSessionForRobot(robotId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @Operation(summary = "Report a focus state",
            description = "Called by the robot when the state changes, and as a heartbeat.")
    @PostMapping("/{robotId}/focus-states")
    public ResponseEntity<FocusStateResponse> reportFocusState(
            @PathVariable UUID robotId,
            @Valid @RequestBody FocusStateRequest request) {

        // TODO: when device tokens exist, return 403 unless the token's robot equals robotId.
        return ResponseEntity.ok(focusSessionService.recordState(robotId, request));
    }

    // ---------------------------------------------------------------------
    // Pairing: the robot shows a 6-digit code on its screen, the user types it in the app.
    //   1. Robot: POST /robots/pairing-codes   {serialNumber}  -> {code, pairingSecret, expiresAt}
    //   2. App:   POST /robots/pair            {pairingCode}   (user token) -> the robot
    //   3. Robot: POST /robots/pairing-status  {pairingSecret} -> "pending", then "paired" + device tokens
    //   4. Robot: POST /robots/token/refresh   {refreshToken}  -> a new device access token
    // ---------------------------------------------------------------------

    @Operation(summary = "Request a pairing code",
            description = "Robot only, no token. The robot shows the code on its screen.")
    @PostMapping("/pairing-codes")
    public ResponseEntity<PairingCodeResponse> requestPairingCode(
            @Valid @RequestBody PairingCodeRequest request) {
        return ResponseEntity.ok(robotPairingService.requestCode(request.serialNumber()));
    }

    @Operation(summary = "Claim a pairing code",
            description = "App only. Links the robot that shows this code to the logged-in user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Robot linked to the user"),
            @ApiResponse(responseCode = "404", description = "Pairing code not found or expired"),
            @ApiResponse(responseCode = "409", description = "The user already has a different robot")
    })
    @PostMapping("/pair")
    public ResponseEntity<RobotDto> pair(
            Authentication authentication,
            @Valid @RequestBody PairRequest request) {
        // Answer with the DTO, never the Robot entity (it holds the User with its password hash).
        Robot robot = robotPairingService.claim(authentication.getName(), request.pairingCode());
        return ResponseEntity.ok(RobotService.toDto(robot));
    }

    @Operation(summary = "Collect the device tokens",
            description = "Robot only, no token: the pairing secret is the credential. "
                    + "Returns \"pending\" until the user enters the code in the app.")
    @PostMapping("/pairing-status")
    public ResponseEntity<PairingStatusResponse> pairingStatus(
            @Valid @RequestBody PairingStatusRequest request) {
        return ResponseEntity.ok(robotPairingService.collect(request.pairingSecret()));
    }

    @Operation(summary = "Refresh the robot's access token",
            description = "Robot only. Takes the device refresh token from pairing-status.")
    @PostMapping("/token/refresh")
    public ResponseEntity<RefreshResponseDto> refreshDeviceToken(
            @Valid @RequestBody RefreshDto request) {
        String token = request.getRefreshToken();
        boolean valid;
        try {
            valid = jwtServices.isValidDeviceRefreshToken(token);
        } catch (RuntimeException e) {
            valid = false;
        }
        if (!valid) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid device refresh token");
        }
        return ResponseEntity.ok(robotPairingService.refreshDevice(token));
    }
}
