package com.financemanager.controller;

import com.financemanager.dto.StatementPreviewDto;
import com.financemanager.dto.StatementRowDto;
import com.financemanager.entity.User;
import com.financemanager.exception.BadRequestException;
import com.financemanager.service.StatementImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statements")
public class StatementImportController {

    @Autowired
    private StatementImportService statementImportService;

    @PostMapping("/upload")
    public ResponseEntity<StatementPreviewDto> uploadStatement(
            @AuthenticationPrincipal User currentUser,
            @RequestParam("file") MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded statement file is empty.");
        }

        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        if (fileName.endsWith(".pdf") || "application/pdf".equalsIgnoreCase(file.getContentType())) {
            return ResponseEntity.ok(statementImportService.parsePdfStatement(currentUser, file));
        } else if (fileName.endsWith(".csv") || "text/csv".equalsIgnoreCase(file.getContentType()) || "application/vnd.ms-excel".equalsIgnoreCase(file.getContentType())) {
            return ResponseEntity.ok(statementImportService.parseCsvStatement(currentUser, file));
        } else {
            throw new BadRequestException("Unsupported file format. Please upload a .csv or .pdf bank statement.");
        }
    }

    @PostMapping("/commit")
    public ResponseEntity<Map<String, Object>> commitImport(
            @AuthenticationPrincipal User currentUser,
            @RequestBody List<StatementRowDto> rows) {

        return ResponseEntity.ok(statementImportService.commitImport(currentUser, rows));
    }
}
