package com.financemanager.controller;

import com.financemanager.dto.*;
import com.financemanager.entity.User;
import com.financemanager.service.IntelligenceService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/intelligence")
public class IntelligenceController {

    @Autowired
    private IntelligenceService intelligenceService;

    @GetMapping("/report")
    public ResponseEntity<IntelligenceReportDto> getFullReport(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(intelligenceService.getFullIntelligenceReport(currentUser));
    }

    @PostMapping("/what-if")
    public ResponseEntity<WhatIfResponseDto> simulateWhatIf(
            @AuthenticationPrincipal User currentUser,
            @RequestBody WhatIfRequestDto request) {

        return ResponseEntity.ok(intelligenceService.simulateWhatIf(currentUser, request));
    }

    @PostMapping("/ask")
    public ResponseEntity<AiQuestionResponseDto> askQuestion(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody AiQuestionRequestDto request) {

        return ResponseEntity.ok(intelligenceService.askMoneyQuestion(currentUser, request.getPrompt()));
    }

    @GetMapping("/history")
    public ResponseEntity<List<ChatMessageDto>> getHistory(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(intelligenceService.getConversationHistory(currentUser));
    }

    @DeleteMapping("/history")
    public ResponseEntity<Map<String, String>> clearHistory(@AuthenticationPrincipal User currentUser) {
        intelligenceService.clearConversationHistory(currentUser);
        return ResponseEntity.ok(Map.of("status", "CLEARED", "message", "Conversation context cleared."));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAiStatus() {
        return ResponseEntity.ok(intelligenceService.getAiStatus());
    }

    @PostMapping("/config")
    public ResponseEntity<Map<String, Object>> updateAiConfig(
            @AuthenticationPrincipal User currentUser,
            @RequestBody Map<String, String> body) {
        String apiKey = body.get("apiKey");
        String model = body.get("model");
        intelligenceService.updateGeminiConfig(apiKey, model);
        return ResponseEntity.ok(intelligenceService.getAiStatus());
    }

    @GetMapping("/actions")
    public ResponseEntity<List<AiActionDto>> getProposedActions(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(intelligenceService.getProposedActions(currentUser));
    }

    @PostMapping("/actions/confirm")
    public ResponseEntity<Map<String, Object>> confirmAction(
            @AuthenticationPrincipal User currentUser,
            @RequestBody AiActionDto action) {

        return ResponseEntity.ok(intelligenceService.executeProposedAction(currentUser, action));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<String>> getStudentCategories() {
        return ResponseEntity.ok(IntelligenceService.STUDENT_CATEGORIES);
    }
}
