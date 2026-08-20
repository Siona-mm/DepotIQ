package com.depotiq.controllers;

import com.depotiq.dtos.profile.ProfileResponse;
import com.depotiq.dtos.profile.UpdateProfileRequest;
import com.depotiq.services.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class UserProfileController {
    private final UserProfileService profileService;

    public UserProfileController(UserProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/me")
    public ProfileResponse getProfile(Authentication authentication) {
        return profileService.getProfile(authentication.getName());
    }

    @PutMapping("/me")
    public ProfileResponse updateProfile(Authentication authentication, @Valid @RequestBody UpdateProfileRequest request) {
        return profileService.updateProfile(authentication.getName(), request);
    }
}
