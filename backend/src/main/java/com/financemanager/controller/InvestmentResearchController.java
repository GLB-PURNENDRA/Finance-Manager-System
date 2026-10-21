package com.financemanager.controller;

import com.financemanager.dto.*;
import com.financemanager.entity.User;
import com.financemanager.service.AmfiNavService;
import com.financemanager.service.InvestmentAffordabilityService;
import com.financemanager.service.InvestmentSuitabilityService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/investments/research")
public class InvestmentResearchController {

    @Autowired
    private InvestmentAffordabilityService affordabilityService;

    @Autowired
    private InvestmentSuitabilityService suitabilityService;

    @Autowired
    private AmfiNavService amfiNavService;

    @GetMapping("/assessment")
    public ResponseEntity<FinancialAssessmentDto> getFinancialAssessment(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) Integer lookbackMonths) {
        return ResponseEntity.ok(affordabilityService.calculateAffordability(currentUser, lookbackMonths));
    }

    @GetMapping("/profile")
    public ResponseEntity<InvestorProfileDto> getInvestorProfile(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(affordabilityService.getProfile(currentUser));
    }

    @PostMapping("/profile")
    public ResponseEntity<InvestorProfileDto> saveInvestorProfile(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody InvestorProfileDto dto) {
        return ResponseEntity.ok(affordabilityService.saveProfile(currentUser, dto));
    }

    @GetMapping("/schemes")
    public ResponseEntity<List<MutualFundSchemeDto>> getSchemes(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String broadCategory) {
        List<MutualFundSchemeDto> list = amfiNavService.getAllSchemes();

        if (broadCategory != null && !broadCategory.isBlank()) {
            list = list.stream()
                    .filter(s -> broadCategory.equalsIgnoreCase(s.getBroadCategory()))
                    .toList();
        }
        if (category != null && !category.isBlank()) {
            list = list.stream()
                    .filter(s -> s.getCategory() != null && s.getCategory().toLowerCase().contains(category.toLowerCase()))
                    .toList();
        }
        return ResponseEntity.ok(list);
    }

    @GetMapping("/recommendations")
    public ResponseEntity<List<MutualFundSchemeDto>> getPersonalizedRecommendations(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(suitabilityService.getRecommendedSchemes(currentUser));
    }

    @PostMapping("/compare")
    public ResponseEntity<SchemeComparisonResponseDto> compareSchemes(@RequestBody SchemeComparisonRequestDto request) {
        return ResponseEntity.ok(suitabilityService.compareSchemes(request.getSchemeCodes()));
    }

    @PostMapping("/simulate")
    public ResponseEntity<SipSimulationResponseDto> simulateSip(@RequestBody SipSimulationRequestDto request) {
        return ResponseEntity.ok(suitabilityService.simulateSip(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshAmfiData(@AuthenticationPrincipal User currentUser) {
        amfiNavService.refreshDataFromSources();
        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Refreshed live NAVs and metrics from AMFI and historical registries.",
                "lastSuccessfulRefresh", String.valueOf(amfiNavService.getLastSuccessfulRefresh()),
                "isStale", amfiNavService.isStale(),
                "totalSchemes", amfiNavService.getAllSchemes().size()
        ));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getDataSourceStatus() {
        return ResponseEntity.ok(Map.of(
                "source", "Association of Mutual Funds in India (AMFI) Official NAV Feed",
                "officialUrl", "https://www.amfiindia.com/net-asset-value/nav-download",
                "pricingModel", "End-of-Day Official Net Asset Value (Delayed EOD, not tick-by-tick)",
                "lastSuccessfulRefresh", String.valueOf(amfiNavService.getLastSuccessfulRefresh()),
                "isStale", amfiNavService.isStale(),
                "totalCuratedSchemes", amfiNavService.getAllSchemes().size()
        ));
    }
}
