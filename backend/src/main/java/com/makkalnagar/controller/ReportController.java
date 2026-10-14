package com.makkalnagar.controller;

import com.makkalnagar.service.ReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/departments")
    public ResponseEntity<List<Map<String, Object>>> getDepartmentReport() {
        return ResponseEntity.ok(reportService.getDepartmentReport());
    }

    @GetMapping("/categories")
    public ResponseEntity<List<Map<String, Object>>> getCategoryReport() {
        return ResponseEntity.ok(reportService.getCategoryReport());
    }

    @GetMapping("/status")
    public ResponseEntity<List<Map<String, Object>>> getStatusReport() {
        return ResponseEntity.ok(reportService.getStatusReport());
    }

    @GetMapping("/resolution-time")
    public ResponseEntity<List<Map<String, Object>>> getResolutionTimeReport() {
        return ResponseEntity.ok(reportService.getResolutionTimeReport());
    }

    @GetMapping("/pending-age")
    public ResponseEntity<List<Map<String, Object>>> getPendingAgeReport() {
        return ResponseEntity.ok(reportService.getPendingAgeReport());
    }

    @GetMapping("/export.csv")
    public ResponseEntity<String> exportCsv() {
        String csvData = reportService.exportComplaintsCsv();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"makkal-nagar-complaints-report.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }
}
