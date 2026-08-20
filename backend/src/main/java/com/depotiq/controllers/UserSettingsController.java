package com.depotiq.controllers;

import com.depotiq.dtos.settings.SettingsResponse;
import com.depotiq.dtos.settings.UpdateSettingsRequest;
import com.depotiq.services.UserSettingsService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
public class UserSettingsController {
    private final UserSettingsService service;

    public UserSettingsController(UserSettingsService service) { this.service = service; }

    @GetMapping("/me")
    public SettingsResponse get(Authentication authentication) { return service.get(authentication.getName()); }

    @PutMapping("/me")
    public SettingsResponse update(Authentication authentication, @Valid @RequestBody UpdateSettingsRequest request) { return service.update(authentication.getName(), request); }

    @DeleteMapping("/me")
    public SettingsResponse reset(Authentication authentication) { return service.reset(authentication.getName()); }
}
