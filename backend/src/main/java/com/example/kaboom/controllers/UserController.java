package com.example.kaboom.controllers;

import com.example.kaboom.dtos.ChangePasswordRequest;
import com.example.kaboom.dtos.MessageResponseDto;
import com.example.kaboom.dtos.OwnedItemDto;
import com.example.kaboom.dtos.UpdateProfileRequest;
import com.example.kaboom.dtos.UserDto;
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
        return ResponseEntity.ok(userMapper.toDto(currentUser()));
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
     * Updates the authenticated user's name and email.
     */
    @Operation(
            summary = "Update my profile",
            description = "Changes the authenticated user's name and email. 409 if the email is taken."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid name or email"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "409", description = "Email already in use")
    })
    @PutMapping("/me")
    public ResponseEntity<UserDto> updateMyProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        User user = currentUser();
        return ResponseEntity.ok(
                userServices.updateProfile(user.getId(), request.getName(), request.getEmail())
        );
    }

    /**
     * Changes the authenticated user's password.
     */
    @Operation(
            summary = "Change my password",
            description = "Checks the current password, then saves the new one (8+ characters)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password changed"),
            @ApiResponse(responseCode = "400", description = "Current password is wrong or the new one is too short"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @PutMapping("/me/password")
    public ResponseEntity<MessageResponseDto> changeMyPassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        userServices.changePassword(
                getAuthenticatedEmail(),
                request.getCurrentPassword(),
                request.getNewPassword());
        return ResponseEntity.ok(new MessageResponseDto("Password changed"));
    }

    /**
     * Retrieves the items owned by the authenticated user.
     */
    @Operation(
            summary = "Get my items",
            description = "Returns the shop items the authenticated user owns, and which ones are equipped."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Owned items retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @GetMapping("/me/items")
    public ResponseEntity<List<OwnedItemDto>> getMyItems() {
        return ResponseEntity.ok(
                purchaseService.getUserItems(getAuthenticatedEmail())
        );
    }

    /**
     * Equips or unequips one of the authenticated user's items.
     */
    @Operation(
            summary = "Equip or unequip an item",
            description = "equipped=true puts the item on Sparky (and takes off the other items "
                    + "of the same category); equipped=false takes it off."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item updated"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "The user doesn't own this item")
    })
    @PatchMapping("/me/items/{productId}")
    public ResponseEntity<OwnedItemDto> setItemEquipped(
            @PathVariable UUID productId,
            @RequestParam boolean equipped) {
        return ResponseEntity.ok(
                purchaseService.setEquipped(getAuthenticatedEmail(), productId, equipped)
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
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyAccount() {
        userServices.deleteUser(currentUser().getId());
        return ResponseEntity.ok().build();
    }

    private User currentUser() {
        return userRepository.findByEmail(getAuthenticatedEmail())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));
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
