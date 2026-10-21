package com.financemanager.controller;

import com.financemanager.dto.SavingsGoalDto;
import com.financemanager.entity.User;
import com.financemanager.service.SavingsGoalService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/goals")
public class SavingsGoalController {

    @Autowired
    private SavingsGoalService savingsGoalService;

    @GetMapping
    public ResponseEntity<List<SavingsGoalDto>> getGoals(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(savingsGoalService.getGoals(currentUser));
    }

    @PostMapping
    public ResponseEntity<SavingsGoalDto> createGoal(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody SavingsGoalDto dto) {

        return ResponseEntity.ok(savingsGoalService.createGoal(currentUser, dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SavingsGoalDto> updateGoal(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @Valid @RequestBody SavingsGoalDto dto) {

        return ResponseEntity.ok(savingsGoalService.updateGoal(currentUser, id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteGoal(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id) {

        savingsGoalService.deleteGoal(currentUser, id);
        return ResponseEntity.ok(Map.of("message", "Savings goal deleted successfully"));
    }
}
