
package com.example.kaboom.controllers;

import com.example.kaboom.dtos.UserDto;
import com.example.kaboom.entities.Product;
import com.example.kaboom.entities.User;
import com.example.kaboom.mappers.UserMapper;
import com.example.kaboom.repositories.UserRepository;
import com.example.kaboom.services.PurchaseService;
import com.example.kaboom.services.UserServices;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/users")
@Tag(name = "Users", description = "Retrieve and manage user accounts.")
public class UserController {

    private final UserServices userServices;
    private final PurchaseService purchaseService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    /**
     * Retrieves the authenticated user's profile.
     */
    @Operation(
            summary = "Get my profile",
            description = "Returns the authenticated user's ID, name, email, coins, and role."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User profile retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/me")
    public ResponseEntity<UserDto> getMyProfile() {
        String email = getAuthenticatedEmail();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        return ResponseEntity.ok(userMapper.toDto(user));
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
     * Updates the authenticated user's profile.
     */
    @Operation(
            summary = "Update my profile",
            description = "Updates the authenticated user's name and email."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid update data"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "409", description = "Email already in use")
    })
    @PutMapping("/me")
    public ResponseEntity<UserDto> updateMyProfile(
            @Valid @RequestBody UserDto userDto) {

        String email = getAuthenticatedEmail();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        return ResponseEntity.ok(
                userServices.updateUser(user.getId(), userDto)
        );
    }

    /**
     * Retrieves the products owned by the authenticated user.
     */
    @Operation(
            summary = "Get user's owned items",
            description = "Returns the products purchased by the authenticated user."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Owned items retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @GetMapping("/me/items")
    public ResponseEntity<List<Product>> getMyItems() {
        return ResponseEntity.ok(
                purchaseService.getUserItems(getAuthenticatedEmail())
        );
    }

    /**
     * Deletes the authenticated user's account.
     */
    @Operation(
            summary = "Delete my account",
            description = "Deletes the authenticated user's account."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account deleted successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyAccount() {
        String email = getAuthenticatedEmail();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));

        userServices.deleteUser(user.getId());

        return ResponseEntity.ok().build();
    }

    private String getAuthenticatedEmail() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required"
            );
        }

        return authentication.getName();
    }
}
