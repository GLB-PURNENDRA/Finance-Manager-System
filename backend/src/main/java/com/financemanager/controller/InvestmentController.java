package com.financemanager.controller;

import com.financemanager.dto.InvestmentDto;
import com.financemanager.entity.User;
import com.financemanager.service.InvestmentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/investments")
public class InvestmentController {

    @Autowired
    private InvestmentService investmentService;

    @GetMapping
    public ResponseEntity<List<InvestmentDto>> getInvestments(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(investmentService.getInvestments(currentUser));
    }

    @PostMapping
    public ResponseEntity<InvestmentDto> createInvestment(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody InvestmentDto dto) {

        return ResponseEntity.ok(investmentService.createInvestment(currentUser, dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InvestmentDto> updateInvestment(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @Valid @RequestBody InvestmentDto dto) {

        return ResponseEntity.ok(investmentService.updateInvestment(currentUser, id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteInvestment(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id) {

        investmentService.deleteInvestment(currentUser, id);
        return ResponseEntity.ok(Map.of("message", "Investment deleted successfully"));
    }
}
