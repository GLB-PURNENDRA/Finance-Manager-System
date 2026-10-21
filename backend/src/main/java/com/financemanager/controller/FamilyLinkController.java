package com.financemanager.controller;

import com.financemanager.dto.FamilyLinkDto;
import com.financemanager.dto.LinkRequestDto;
import com.financemanager.entity.User;
import com.financemanager.service.FamilyLinkService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/family")
public class FamilyLinkController {

    @Autowired
    private FamilyLinkService familyLinkService;

    @GetMapping("/links")
    public ResponseEntity<List<FamilyLinkDto>> getMyLinks(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(familyLinkService.getMyLinks(currentUser));
    }

    @PostMapping("/request")
    public ResponseEntity<FamilyLinkDto> requestLink(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody LinkRequestDto request) {

        return ResponseEntity.ok(familyLinkService.requestLink(currentUser, request));
    }

    @PostMapping("/links/{id}/approve")
    public ResponseEntity<FamilyLinkDto> approveLink(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id) {

        return ResponseEntity.ok(familyLinkService.approveLink(currentUser, id));
    }

    @PostMapping("/links/{id}/revoke")
    public ResponseEntity<FamilyLinkDto> revokeLink(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id) {

        return ResponseEntity.ok(familyLinkService.revokeLink(currentUser, id));
    }
}
