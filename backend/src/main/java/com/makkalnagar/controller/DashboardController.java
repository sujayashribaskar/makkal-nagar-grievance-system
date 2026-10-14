package com.makkalnagar.controller;

import com.makkalnagar.dto.ChartDataResponse;
import com.makkalnagar.dto.DashboardSummaryResponse;
import com.makkalnagar.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getSummary() {
        return ResponseEntity.ok(dashboardService.getSummary());
    }

    @GetMapping("/charts")
    public ResponseEntity<ChartDataResponse> getCharts() {
        return ResponseEntity.ok(dashboardService.getCharts());
    }
}
