package com.depotiq.dtos.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateCredentialsRequest(
        @NotBlank
        @Size(min = 3, max = 50)
        @Pattern(
                regexp = "^[A-Za-z0-9._-]+$",
                message = "Username may contain letters, numbers, periods, underscores, and hyphens"
        )
        String username,
        @NotBlank @Size(max = 72) String currentPassword,
        @Pattern(
                regexp = "^$|^.{8,72}$",
                message = "New password must contain between 8 and 72 characters"
        )
        String newPassword
) {
}
