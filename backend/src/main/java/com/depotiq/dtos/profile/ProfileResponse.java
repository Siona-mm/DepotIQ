package com.depotiq.dtos.profile;

public record ProfileResponse(
        String username,
        String displayName,
        String email,
        String jobTitle,
        String avatarData
) {
}
