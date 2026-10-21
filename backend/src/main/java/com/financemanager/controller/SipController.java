package com.financemanager.controller;

import com.financemanager.dto.SipDto;
import com.financemanager.entity.User;
import com.financemanager.service.SipService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sips")
public class SipController {

    @Autowired
    private SipService sipService;

    @GetMapping
    public ResponseEntity<List<SipDto>> getSips(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(sipService.getSips(currentUser));
    }

    @PostMapping
    public ResponseEntity<SipDto> createSip(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody SipDto dto) {

        return ResponseEntity.ok(sipService.createSip(currentUser, dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SipDto> updateSip(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id,
            @Valid @RequestBody SipDto dto) {

        return ResponseEntity.ok(sipService.updateSip(currentUser, id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteSip(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long id) {

        sipService.deleteSip(currentUser, id);
        return ResponseEntity.ok(Map.of("message", "SIP record deleted successfully"));
    }
}
