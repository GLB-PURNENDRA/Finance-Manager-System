package com.financemanager.service;

import com.financemanager.dto.InvestmentDto;
import com.financemanager.entity.Investment;
import com.financemanager.entity.Role;
import com.financemanager.entity.User;
import com.financemanager.exception.ForbiddenException;
import com.financemanager.exception.ResourceNotFoundException;
import com.financemanager.repository.InvestmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class InvestmentService {

    @Autowired
    private InvestmentRepository investmentRepository;

    @Autowired
    private FamilyLinkService familyLinkService;

    @Autowired
    private AuditService auditService;

    public List<InvestmentDto> getInvestments(User currentUser) {
        User targetStudent;
        if (currentUser.getRole() == Role.PARENT) {
            targetStudent = familyLinkService.getActiveStudentForParent(currentUser);
        } else {
            targetStudent = currentUser;
        }

        return investmentRepository.findByStudentOrderByCreatedAtDesc(targetStudent)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public InvestmentDto createInvestment(User currentUser, InvestmentDto dto) {
        if (currentUser.getRole() == Role.PARENT) {
            throw new ForbiddenException("Parents have strictly read-only access and cannot alter investments.");
        }

        Investment inv = new Investment(
                currentUser,
                dto.getAssetType().trim().toUpperCase(),
                dto.getAssetName().trim(),
                dto.getInvestedAmount(),
                dto.getCurrentValue()
        );

        inv = investmentRepository.save(inv);
        auditService.log(currentUser, "INVESTMENT_CREATED", "INVESTMENT", inv.getId().toString(),
                "Asset: " + inv.getAssetName());
        return toDto(inv);
    }

    public InvestmentDto updateInvestment(User currentUser, Long id, InvestmentDto dto) {
        if (currentUser.getRole() == Role.PARENT) {
            throw new ForbiddenException("Parents have strictly read-only access and cannot alter investments.");
        }

        Investment inv = investmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Investment not found."));

        if (!inv.getStudent().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You can only modify your own investments.");
        }

        inv.setAssetType(dto.getAssetType().trim().toUpperCase());
        inv.setAssetName(dto.getAssetName().trim());
        inv.setInvestedAmount(dto.getInvestedAmount());
        inv.setCurrentValue(dto.getCurrentValue());
        inv.setLastUpdated(LocalDateTime.now());

        inv = investmentRepository.save(inv);
        auditService.log(currentUser, "INVESTMENT_UPDATED", "INVESTMENT", inv.getId().toString(),
                "Current Value: " + inv.getCurrentValue());
        return toDto(inv);
    }

    public void deleteInvestment(User currentUser, Long id) {
        if (currentUser.getRole() == Role.PARENT) {
            throw new ForbiddenException("Parents have strictly read-only access and cannot alter investments.");
        }

        Investment inv = investmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Investment not found."));

        if (!inv.getStudent().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You can only delete your own investments.");
        }

        investmentRepository.delete(inv);
        auditService.log(currentUser, "INVESTMENT_DELETED", "INVESTMENT", id.toString(), "Deleted by student");
    }

    public InvestmentDto toDto(Investment inv) {
        InvestmentDto dto = new InvestmentDto();
        dto.setId(inv.getId());
        dto.setAssetType(inv.getAssetType());
        dto.setAssetName(inv.getAssetName());
        dto.setInvestedAmount(inv.getInvestedAmount());
        dto.setCurrentValue(inv.getCurrentValue());
        dto.setLastUpdated(inv.getLastUpdated());

        BigDecimal pnl = inv.getCurrentValue().subtract(inv.getInvestedAmount());
        dto.setProfitLoss(pnl);

        if (inv.getInvestedAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal pnlPct = pnl.multiply(new BigDecimal("100"))
                    .divide(inv.getInvestedAmount(), 2, RoundingMode.HALF_UP);
            dto.setProfitLossPercentage(pnlPct);
        } else {
            dto.setProfitLossPercentage(BigDecimal.ZERO);
        }

        return dto;
    }
}
