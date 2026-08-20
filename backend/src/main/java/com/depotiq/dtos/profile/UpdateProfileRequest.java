package com.depotiq.dtos.profile;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 150) String displayName,
        @Email @Size(max = 255) String email,
        @Size(max = 150) String jobTitle,
        String avatarData
) {
}
