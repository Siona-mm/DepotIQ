package com.depotiq.mappers;

import com.depotiq.dtos.profile.ProfileResponse;
import com.depotiq.models.UserProfile;
import org.springframework.stereotype.Component;

@Component
public class UserProfileMapper {
    public ProfileResponse toResponse(UserProfile profile) {
        return new ProfileResponse(profile.getUsername(), profile.getDisplayName(), profile.getEmail(), profile.getJobTitle(), profile.getAvatarData());
    }
}
