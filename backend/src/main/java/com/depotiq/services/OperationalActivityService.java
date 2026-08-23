package com.depotiq.services;

import com.depotiq.dtos.activity.OperationalActivityResponse;
import com.depotiq.models.OperationalActivityLog;
import com.depotiq.repositories.OperationalActivityLogRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OperationalActivityService {
    private final OperationalActivityLogRepository repository;

    public OperationalActivityService(OperationalActivityLogRepository repository) {
        this.repository = repository;
    }

    public void record(String type, String actor, String referenceType, Long referenceId, String label, String detail) {
        OperationalActivityLog entry = new OperationalActivityLog();
        entry.setActivityType(type);
        entry.setActor(actor == null || actor.isBlank() ? "System" : actor);
        entry.setReferenceType(referenceType);
        entry.setReferenceId(referenceId);
        entry.setReferenceLabel(label);
        entry.setDetail(detail);
        repository.save(entry);
    }

    public List<OperationalActivityResponse> recent() {
        return repository.findTop100ByOrderByCreatedAtDesc().stream()
                .map(entry -> new OperationalActivityResponse(entry.getId(), entry.getActivityType(), entry.getActor(), entry.getReferenceType(), entry.getReferenceId(), entry.getReferenceLabel(), entry.getDetail(), entry.getCreatedAt()))
                .toList();
    }
}
