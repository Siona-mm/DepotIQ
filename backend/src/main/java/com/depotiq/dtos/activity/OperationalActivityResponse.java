package com.depotiq.dtos.activity;

import java.time.LocalDateTime;

public record OperationalActivityResponse(Long id, String activityType, String actor, String referenceType, Long referenceId, String referenceLabel, String detail, LocalDateTime createdAt) { }
