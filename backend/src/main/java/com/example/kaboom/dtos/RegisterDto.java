
package com.example.kaboom.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Contains the input fields required to register a new user.
 * Validation annotations ensure that the submitted data meets basic requirements.
 */
@Getter
@Setter
@Schema(description = "Request body for registering a new Kaboom user.")
public class RegisterDto {

    @Schema(description = "User's display name", example = "Sara", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Name must not be blank")
    private String name;

    @Schema(description = "User's email address", example = "sara@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Email must not be blank")
    @Email(message = "Email must be valid")
    private String email;

    @Schema(description = "User's password. Must contain between 8 and 72 characters.", example = "SecurePass123", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Password must not be blank")
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    private String password;

    public RegisterDto() {
    }
}
