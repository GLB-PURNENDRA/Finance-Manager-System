package com.financemanager.controller;

import com.financemanager.dto.BudgetDto;
import com.financemanager.entity.User;
import com.financemanager.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    @Autowired
    private BudgetService budgetService;

    @GetMapping
    public ResponseEntity<List<BudgetDto>> getBudgets(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) String periodMonth) {

        return ResponseEntity.ok(budgetService.getBudgets(currentUser, periodMonth));
    }

    @PostMapping
    public ResponseEntity<BudgetDto> createOrUpdateBudget(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody BudgetDto dto) {

        return ResponseEntity.ok(budgetService.createOrUpdateBudget(currentUser, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteBudget(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id) {

        budgetService.deleteBudget(currentUser, id);
        return ResponseEntity.ok(Map.of("message", "Budget deleted successfully"));
    }
}
