package com.financemanager.controller;

import com.financemanager.dto.DashboardSummaryDto;
import com.financemanager.entity.User;
import com.financemanager.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardSummaryDto> getDashboardSummary(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) Long studentId) {
        return ResponseEntity.ok(reportService.getDashboardSummary(currentUser, studentId));
    }

    @GetMapping("/export-csv")
    public ResponseEntity<byte[]> exportCsv(@AuthenticationPrincipal User currentUser) {
        String csvContent = reportService.exportTransactionsCsv(currentUser);
        byte[] bytes = csvContent.getBytes();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"finance_transactions.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(bytes);
    }
}
