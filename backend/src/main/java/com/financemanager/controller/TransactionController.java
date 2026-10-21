package com.financemanager.controller;

import com.financemanager.dto.TransactionDto;
import com.financemanager.entity.TransactionType;
import com.financemanager.entity.User;
import com.financemanager.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @GetMapping
    public ResponseEntity<List<TransactionDto>> getTransactions(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) Long studentId) {

        return ResponseEntity.ok(transactionService.getTransactions(currentUser, startDate, endDate, category, type, studentId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionDto> getTransactionById(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id) {

        return ResponseEntity.ok(transactionService.getTransactionById(currentUser, id));
    }

    @PostMapping
    public ResponseEntity<TransactionDto> createTransaction(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody TransactionDto dto) {

        return ResponseEntity.ok(transactionService.createTransaction(currentUser, dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionDto> updateTransaction(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @Valid @RequestBody TransactionDto dto) {

        return ResponseEntity.ok(transactionService.updateTransaction(currentUser, id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteTransaction(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id) {

        transactionService.deleteTransaction(currentUser, id);
        return ResponseEntity.ok(Map.of("message", "Transaction deleted successfully"));
    }
}
