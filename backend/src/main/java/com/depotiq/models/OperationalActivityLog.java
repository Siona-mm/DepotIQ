package com.depotiq.models;

import com.depotiq.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "operational_activity_logs")
public class OperationalActivityLog extends BaseEntity {
    @Column(name = "activity_type", nullable = false, length = 50)
    private String activityType;
    @Column(nullable = false, length = 100)
    private String actor;
    @Column(name = "reference_type", nullable = false, length = 50)
    private String referenceType;
    @Column(name = "reference_id", nullable = false)
    private Long referenceId;
    @Column(name = "reference_label", nullable = false, length = 150)
    private String referenceLabel;
    @Column(length = 500)
    private String detail;
    public String getActivityType() { return activityType; }
    public void setActivityType(String activityType) { this.activityType = activityType; }
    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }
    public String getReferenceType() { return referenceType; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }
    public Long getReferenceId() { return referenceId; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }
    public String getReferenceLabel() { return referenceLabel; }
    public void setReferenceLabel(String referenceLabel) { this.referenceLabel = referenceLabel; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
}
