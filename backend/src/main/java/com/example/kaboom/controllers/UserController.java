package com.example.kaboom.controllers;

import com.example.kaboom.dtos.UserDto;
import com.example.kaboom.entities.User;
import com.example.kaboom.services.UserServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication; // NEW
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Handles user profile retrieval, updates, and account deletion.
 * Business logic is delegated to UserServices.
 */
@RestController
@AllArgsConstructor
@RequestMapping("/users")
@Tag(name = "Users", description = "Retrieve, update, and delete user accounts.")
public class UserController {

    private final UserServices userServices;

    /**
     * NEW: Returns the logged-in user's profile.
     * The email comes from the access token (set by JwtAuthFilter).
     */
    @Operation(
            summary = "Get the logged-in user",
            description = "Returns the profile of the user who owns the access token."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid access token"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/me")
    public ResponseEntity<UserDto> getMe(Authentication authentication) {
        return ResponseEntity.ok(userServices.getByEmail(authentication.getName()));
    }

    /**
     * Retrieves a user's profile by UUID.
     */
    @Operation(
            summary = "Get user by ID",
            description = "Retrieves a user account using its UUID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(userServices.getUserById(id));
    }

    /**
     * Updates the specified user's profile.
     * The service method will be aligned with the existing implementation later.
     */
    @Operation(
            summary = "Update user profile",
            description = "Updates the user's account information."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid update data"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "409", description = "Email already in use")
    })
    @PutMapping("/me")
    public ResponseEntity<UserDto> updateMyProfile(
            @RequestParam UUID userId,
            @Valid @RequestBody UserDto userDto) {

        // TODO: Replace the supplied userId with the authenticated user's ID.
        return ResponseEntity.ok(userServices.updateUser(userId, userDto));
    }

    /**
     * Deletes the specified user's account.
     */
    @Operation(
            summary = "Delete user account",
            description = "Deletes the user's account and its associated data."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account deleted successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyAccount(@RequestParam UUID userId) {

        // TODO: Replace the supplied userId with the authenticated user's ID.
        userServices.deleteUser(userId);
        return ResponseEntity.ok().build();
    }
}