package com.depotiq.mappers;

import com.depotiq.dtos.settings.SettingsResponse;
import com.depotiq.models.UserSettings;
import org.springframework.stereotype.Component;

@Component
public class UserSettingsMapper {
    public SettingsResponse toResponse(UserSettings settings) {
        return new SettingsResponse(settings.getDefaultHorizon(), settings.getSafetyStockDays(), settings.getAlertThreshold(), settings.getAutoRefresh(), settings.getRequireApproval(), settings.getAllowOverrides(), settings.getEmailAlerts());
    }
}
