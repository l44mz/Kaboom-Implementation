
package com.example.kaboom.controllers;

import com.example.kaboom.dtos.*;
import com.example.kaboom.entities.Robot;
import com.example.kaboom.services.FocusSessionService;
import com.example.kaboom.services.RobotPairingService;
import com.example.kaboom.services.RobotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Handles requests for retrieving and selecting robots.
 *
 * Robot business logic is delegated to RobotService.
 * Users must only be able to access robots associated with their own accounts.
 */
@RestController
@RequestMapping("/robots")
@Tag(
        name = "Robots",
        description = "Retrieve the user's robot and manage robot selection."
)
public class RobotController {

    private final RobotService robotService;
    private final FocusSessionService focusSessionService;
    private final RobotPairingService robotPairingService;

    public RobotController(RobotService robotService, FocusSessionService focusSessionService,  RobotPairingService robotPairingService) {
        this.robotService = robotService;
        this.focusSessionService = focusSessionService;
        this.robotPairingService = robotPairingService;
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
            @ApiResponse(
                    responseCode = "200",
                    description = "Robot retrieved successfully",
                    content = @Content(
                            schema = @Schema(implementation = Robot.class)
                    )
            ),
            @ApiResponse(responseCode = "401", description = "Missing or invalid authentication"),
            @ApiResponse(responseCode = "404", description = "No robot is associated with the user"),
            @ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    @GetMapping("/me")
    public ResponseEntity<RobotResponse> getMyRobot(Authentication authentication) {
        return ResponseEntity.ok(robotService.getRobotForUser(authentication.getName()));
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


   //-------------------------------------------------------------------------
    

    @Operation(summary = "Report a focus state",
            description = "Called by the robot on state changes and as a heartbeat.")
    @PostMapping("/{robotId}/focus-states")
    public ResponseEntity<FocusStateResponse> reportFocusState(
            @PathVariable UUID robotId,
            @Valid @RequestBody FocusStateRequest request) {

        // TODO: when device JWT exists, return 403 unless its robot claim equals robotId.
        return ResponseEntity.ok(focusSessionService.recordState(robotId, request));
    }

    //------------------------------------------------------------------------

    @Operation(summary = "Request a pairing code", description = "Robot only. No token.")
    @PostMapping("/pairing-codes")
    public ResponseEntity<PairingCodeResponse> requestPairingCode(
            @Valid @RequestBody PairingCodeRequest request) {
        return ResponseEntity.ok(robotPairingService.requestCode(request.serialNumber()));
    }

    @Operation(summary = "Claim a pairing code", description = "App only. Requires a user JWT.")
    @PostMapping("/pair")
    public ResponseEntity<Robot> pair(
            Authentication authentication,
            @Valid @RequestBody PairRequest request) {

        return ResponseEntity.ok(
                robotPairingService.claim(authentication.getName(), request.pairingCode()));
    }

    @Operation(summary = "Collect device tokens", description = "Robot only. The secret is the credential.")
    @PostMapping("/pairing-status")
    public ResponseEntity<PairingStatusResponse> pairingStatus(
            @Valid @RequestBody PairingStatusRequest request) {
        return ResponseEntity.ok(robotPairingService.collect(request.pairingSecret()));
    }


}
