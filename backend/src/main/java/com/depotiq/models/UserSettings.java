package com.depotiq.models;

import com.depotiq.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_settings")
public class UserSettings extends BaseEntity {
    @Column(nullable = false, unique = true, length = 100)
    private String username;
    @Column(name = "default_horizon", nullable = false)
    private Integer defaultHorizon = 7;
    @Column(name = "safety_stock_days", nullable = false)
    private Integer safetyStockDays = 3;
    @Column(name = "alert_threshold", nullable = false)
    private Integer alertThreshold = 250;
    @Column(name = "auto_refresh", nullable = false)
    private Boolean autoRefresh = true;
    @Column(name = "require_approval", nullable = false)
    private Boolean requireApproval = true;
    @Column(name = "allow_overrides", nullable = false)
    private Boolean allowOverrides = true;
    @Column(name = "email_alerts", nullable = false)
    private Boolean emailAlerts = false;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Integer getDefaultHorizon() { return defaultHorizon; }
    public void setDefaultHorizon(Integer defaultHorizon) { this.defaultHorizon = defaultHorizon; }
    public Integer getSafetyStockDays() { return safetyStockDays; }
    public void setSafetyStockDays(Integer safetyStockDays) { this.safetyStockDays = safetyStockDays; }
    public Integer getAlertThreshold() { return alertThreshold; }
    public void setAlertThreshold(Integer alertThreshold) { this.alertThreshold = alertThreshold; }
    public Boolean getAutoRefresh() { return autoRefresh; }
    public void setAutoRefresh(Boolean autoRefresh) { this.autoRefresh = autoRefresh; }
    public Boolean getRequireApproval() { return requireApproval; }
    public void setRequireApproval(Boolean requireApproval) { this.requireApproval = requireApproval; }
    public Boolean getAllowOverrides() { return allowOverrides; }
    public void setAllowOverrides(Boolean allowOverrides) { this.allowOverrides = allowOverrides; }
    public Boolean getEmailAlerts() { return emailAlerts; }
    public void setEmailAlerts(Boolean emailAlerts) { this.emailAlerts = emailAlerts; }
}
