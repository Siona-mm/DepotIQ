package com.depotiq.services;

import com.depotiq.dtos.profile.ProfileResponse;
import com.depotiq.dtos.profile.UpdateProfileRequest;
import com.depotiq.mappers.UserProfileMapper;
import com.depotiq.models.UserProfile;
import com.depotiq.repositories.UserProfileRepository;
import org.springframework.stereotype.Service;

@Service
public class UserProfileService {
    private final UserProfileRepository repository;
    private final UserProfileMapper mapper;

    public UserProfileService(UserProfileRepository repository, UserProfileMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public ProfileResponse getProfile(String username) {
        return mapper.toResponse(findOrCreate(username));
    }

    public ProfileResponse updateProfile(String username, UpdateProfileRequest request) {
        UserProfile profile = findOrCreate(username);
        profile.setDisplayName(request.displayName().trim());
        profile.setEmail(blankToNull(request.email()));
        profile.setJobTitle(blankToNull(request.jobTitle()));
        profile.setAvatarData(blankToNull(request.avatarData()));
        return mapper.toResponse(repository.save(profile));
    }

    private UserProfile findOrCreate(String username) {
        return repository.findByUsername(username).orElseGet(() -> {
            UserProfile profile = new UserProfile();
            profile.setUsername(username);
            profile.setDisplayName(username);
            return repository.save(profile);
        });
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
