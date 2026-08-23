package com.depotiq.controllers;

import com.depotiq.dtos.activity.OperationalActivityResponse;
import com.depotiq.services.OperationalActivityService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/activity")
public class OperationalActivityController {
    private final OperationalActivityService service;
    public OperationalActivityController(OperationalActivityService service) { this.service = service; }
    @GetMapping
    public List<OperationalActivityResponse> recent() { return service.recent(); }
}
