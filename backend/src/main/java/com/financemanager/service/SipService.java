package com.financemanager.service;

import com.financemanager.dto.SipDto;
import com.financemanager.entity.Role;
import com.financemanager.entity.SipRecord;
import com.financemanager.entity.User;
import com.financemanager.exception.ForbiddenException;
import com.financemanager.exception.ResourceNotFoundException;
import com.financemanager.repository.SipRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SipService {

    @Autowired
    private SipRecordRepository sipRecordRepository;

    @Autowired
    private FamilyLinkService familyLinkService;

    @Autowired
    private AuditService auditService;

    public List<SipDto> getSips(User currentUser) {
        User targetStudent;
        if (currentUser.getRole() == Role.PARENT) {
            targetStudent = familyLinkService.getActiveStudentForParent(currentUser);
        } else {
            targetStudent = currentUser;
        }

        return sipRecordRepository.findByStudentOrderByCreatedAtDesc(targetStudent)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public SipDto createSip(User currentUser, SipDto dto) {
        if (currentUser.getRole() == Role.PARENT) {
            throw new ForbiddenException("Parents have strictly read-only access and cannot alter SIP records.");
        }

        SipRecord sip = new SipRecord(
                currentUser,
                dto.getFundName().trim(),
                dto.getMonthlyAmount(),
                dto.getStartDate(),
                dto.getDueDayOfMonth(),
                dto.getAssumedReturnRate(),
                dto.getDurationMonths()
        );

        sip = sipRecordRepository.save(sip);
        auditService.log(currentUser, "SIP_CREATED", "SIP", sip.getId().toString(),
                "Fund: " + sip.getFundName() + ", Monthly: " + sip.getMonthlyAmount());
        return toDto(sip);
    }

    public SipDto updateSip(User currentUser, Long id, SipDto dto) {
        if (currentUser.getRole() == Role.PARENT) {
            throw new ForbiddenException("Parents have strictly read-only access and cannot alter SIP records.");
        }

        SipRecord sip = sipRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SIP record not found."));

        if (!sip.getStudent().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You can only modify your own SIP records.");
        }

        sip.setFundName(dto.getFundName().trim());
        sip.setMonthlyAmount(dto.getMonthlyAmount());
        sip.setStartDate(dto.getStartDate());
        sip.setDueDayOfMonth(dto.getDueDayOfMonth());
        if (dto.getPaymentStatus() != null) {
            sip.setPaymentStatus(dto.getPaymentStatus());
        }
        if (dto.getAssumedReturnRate() != null) {
            sip.setAssumedReturnRate(dto.getAssumedReturnRate());
        }
        if (dto.getDurationMonths() != null) {
            sip.setDurationMonths(dto.getDurationMonths());
        }

        sip = sipRecordRepository.save(sip);
        auditService.log(currentUser, "SIP_UPDATED", "SIP", sip.getId().toString(),
                "Monthly: " + sip.getMonthlyAmount());
        return toDto(sip);
    }

    public void deleteSip(User currentUser, Long id) {
        if (currentUser.getRole() == Role.PARENT) {
            throw new ForbiddenException("Parents have strictly read-only access and cannot alter SIP records.");
        }

        SipRecord sip = sipRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SIP record not found."));

        if (!sip.getStudent().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You can only delete your own SIP records.");
        }

        sipRecordRepository.delete(sip);
        auditService.log(currentUser, "SIP_DELETED", "SIP", id.toString(), "Deleted by student");
    }

    public SipDto toDto(SipRecord sip) {
        SipDto dto = new SipDto();
        dto.setId(sip.getId());
        dto.setFundName(sip.getFundName());
        dto.setMonthlyAmount(sip.getMonthlyAmount());
        dto.setStartDate(sip.getStartDate());
        dto.setDueDayOfMonth(sip.getDueDayOfMonth());
        dto.setPaymentStatus(sip.getPaymentStatus());
        dto.setAssumedReturnRate(sip.getAssumedReturnRate());
        dto.setDurationMonths(sip.getDurationMonths());

        // Calculations
        BigDecimal p = sip.getMonthlyAmount();
        int n = sip.getDurationMonths();
        BigDecimal totalInvested = p.multiply(BigDecimal.valueOf(n));
        dto.setTotalInvested(totalInvested);

        double rate = sip.getAssumedReturnRate().doubleValue();
        if (rate > 0) {
            double i = (rate / 100.0) / 12.0; // monthly interest rate
            double futureValueDouble = p.doubleValue() * ((Math.pow(1.0 + i, n) - 1.0) / i) * (1.0 + i);
            BigDecimal fv = BigDecimal.valueOf(futureValueDouble).setScale(2, RoundingMode.HALF_UP);
            dto.setEstimatedFutureValue(fv);
            dto.setEstimatedWealthGain(fv.subtract(totalInvested));
        } else {
            dto.setEstimatedFutureValue(totalInvested);
            dto.setEstimatedWealthGain(BigDecimal.ZERO);
        }

        return dto;
    }
}
