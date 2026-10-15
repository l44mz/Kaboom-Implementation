package com.example.kaboom.controllers;

import com.example.kaboom.dtos.*;
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
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Registration and Login")
public class AuthController {

    private final UserServices userServices;

    public AuthController(UserServices userServices) {
        this.userServices = userServices;
    }

    @Operation(summary = "Register a new user")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created",
                    content = @Content(schema = @Schema(implementation = RegisterResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed or malformed JSON",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email already registered",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDto> register(@Valid @RequestBody RegisterDto dto) {

        return ResponseEntity.status(HttpStatus.CREATED).body(userServices.register(dto));

    }

    @Operation(summary = "Log in")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(schema = @Schema(implementation = LoginResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed or malformed JSON",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid email or password",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    public LoginResponseDto login(@Valid @RequestBody LoginDto dto) {
        return userServices.login(dto);
    }

    @Operation(summary = "Get a new access token using a refresh token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "New access token issued",
                    content = @Content(schema = @Schema(implementation = RefreshResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "refreshToken missing or blank",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Refresh token invalid, expired, or revoked",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponseDto> refresh(@Valid @RequestBody RefreshDto dto) {
        return ResponseEntity.ok(userServices.refresh(dto));
    }

    @Operation(summary = "Log out by revoking the refresh token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logged out",
                    content = @Content(schema = @Schema(implementation = MessageResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "refreshToken missing or blank",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/logout")
    public ResponseEntity<MessageResponseDto> logout(@Valid @RequestBody LogoutDto dto) {
        userServices.logout(dto);
        return ResponseEntity.ok(new MessageResponseDto("Logged out"));
    }
}