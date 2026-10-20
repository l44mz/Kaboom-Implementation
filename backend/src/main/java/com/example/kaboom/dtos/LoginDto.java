
package com.example.kaboom.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents the login request submitted by a user.
 * Validates the email address and password before authentication.
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class LoginDto {

    /** The user's email address used to log in. */
    @NotBlank
    @Email
    private String email;

    /** The user's plain-text password supplied for authentication. */
    @NotBlank
    private String password;
}
