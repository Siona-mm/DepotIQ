package com.depotiq.services;

import com.depotiq.dtos.settings.SettingsResponse;
import com.depotiq.dtos.settings.UpdateSettingsRequest;
import com.depotiq.mappers.UserSettingsMapper;
import com.depotiq.models.UserSettings;
import com.depotiq.repositories.UserSettingsRepository;
import org.springframework.stereotype.Service;

@Service
public class UserSettingsService {
    private final UserSettingsRepository repository;
    private final UserSettingsMapper mapper;

    public UserSettingsService(UserSettingsRepository repository, UserSettingsMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public SettingsResponse get(String username) { return mapper.toResponse(findOrCreate(username)); }

    public SettingsResponse update(String username, UpdateSettingsRequest request) {
        UserSettings settings = findOrCreate(username);
        settings.setDefaultHorizon(request.defaultHorizon());
        settings.setSafetyStockDays(request.safetyStockDays());
        settings.setAlertThreshold(request.alertThreshold());
        settings.setAutoRefresh(request.autoRefresh());
        settings.setRequireApproval(request.requireApproval());
        settings.setAllowOverrides(request.allowOverrides());
        settings.setEmailAlerts(request.emailAlerts());
        return mapper.toResponse(repository.save(settings));
    }

    public SettingsResponse reset(String username) {
        repository.findByUsername(username).ifPresent(repository::delete);
        return mapper.toResponse(findOrCreate(username));
    }

    public boolean allowsRecommendationOverrides(String username) {
        return Boolean.TRUE.equals(findOrCreate(username).getAllowOverrides());
    }

    private UserSettings findOrCreate(String username) {
        return repository.findByUsername(username).orElseGet(() -> {
            UserSettings settings = new UserSettings();
            settings.setUsername(username);
            return repository.save(settings);
        });
    }
}
