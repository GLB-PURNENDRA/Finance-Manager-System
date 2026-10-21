package com.financemanager.service;

import com.financemanager.dto.SavingsGoalDto;
import com.financemanager.entity.Role;
import com.financemanager.entity.SavingsGoal;
import com.financemanager.entity.User;
import com.financemanager.exception.ForbiddenException;
import com.financemanager.exception.ResourceNotFoundException;
import com.financemanager.repository.SavingsGoalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SavingsGoalService {

    @Autowired
    private SavingsGoalRepository savingsGoalRepository;

    @Autowired
    private FamilyLinkService familyLinkService;

    @Autowired
    private AuditService auditService;

    public List<SavingsGoalDto> getGoals(User currentUser) {
        User targetStudent;
        if (currentUser.getRole() == Role.PARENT) {
            targetStudent = familyLinkService.getActiveStudentForParent(currentUser);
        } else {
            targetStudent = currentUser;
        }

        return savingsGoalRepository.findByStudentOrderByCreatedAtDesc(targetStudent)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public SavingsGoalDto createGoal(User currentUser, SavingsGoalDto dto) {
        if (currentUser.getRole() == Role.PARENT) {
            throw new ForbiddenException("Parents have strictly read-only access and cannot alter savings goals.");
        }

        SavingsGoal goal = new SavingsGoal(
                currentUser,
                dto.getName().trim(),
                dto.getTargetAmount(),
                dto.getSavedAmount() != null ? dto.getSavedAmount() : BigDecimal.ZERO,
                dto.getTargetDate()
        );

        if (goal.getSavedAmount().compareTo(goal.getTargetAmount()) >= 0) {
            goal.setStatus("COMPLETED");
        }

        goal = savingsGoalRepository.save(goal);
        auditService.log(currentUser, "GOAL_CREATED", "SAVINGS_GOAL", goal.getId().toString(), "Name: " + goal.getName());
        return toDto(goal);
    }

    public SavingsGoalDto updateGoal(User currentUser, Long id, SavingsGoalDto dto) {
        if (currentUser.getRole() == Role.PARENT) {
            throw new ForbiddenException("Parents have strictly read-only access and cannot alter savings goals.");
        }

        SavingsGoal goal = savingsGoalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Savings goal not found."));

        if (!goal.getStudent().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You can only modify your own savings goals.");
        }

        goal.setName(dto.getName().trim());
        goal.setTargetAmount(dto.getTargetAmount());
        if (dto.getSavedAmount() != null) {
            goal.setSavedAmount(dto.getSavedAmount());
        }
        goal.setTargetDate(dto.getTargetDate());

        if (goal.getSavedAmount().compareTo(goal.getTargetAmount()) >= 0) {
            goal.setStatus("COMPLETED");
        } else {
            goal.setStatus("IN_PROGRESS");
        }

        goal = savingsGoalRepository.save(goal);
        auditService.log(currentUser, "GOAL_UPDATED", "SAVINGS_GOAL", goal.getId().toString(), "Saved: " + goal.getSavedAmount());
        return toDto(goal);
    }

    public void deleteGoal(User currentUser, Long id) {
        if (currentUser.getRole() == Role.PARENT) {
            throw new ForbiddenException("Parents have strictly read-only access and cannot alter savings goals.");
        }

        SavingsGoal goal = savingsGoalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Savings goal not found."));

        if (!goal.getStudent().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You can only delete your own savings goals.");
        }

        savingsGoalRepository.delete(goal);
        auditService.log(currentUser, "GOAL_DELETED", "SAVINGS_GOAL", id.toString(), "Deleted by student");
    }

    public SavingsGoalDto toDto(SavingsGoal goal) {
        SavingsGoalDto dto = new SavingsGoalDto();
        dto.setId(goal.getId());
        dto.setName(goal.getName());
        dto.setTargetAmount(goal.getTargetAmount());
        dto.setSavedAmount(goal.getSavedAmount());
        dto.setTargetDate(goal.getTargetDate());
        dto.setStatus(goal.getStatus());

        BigDecimal remaining = goal.getTargetAmount().subtract(goal.getSavedAmount());
        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            remaining = BigDecimal.ZERO;
        }
        dto.setRemainingAmount(remaining);

        if (goal.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal progress = goal.getSavedAmount().multiply(new BigDecimal("100"))
                    .divide(goal.getTargetAmount(), 2, RoundingMode.HALF_UP);
            dto.setProgressPercentage(progress);
        } else {
            dto.setProgressPercentage(BigDecimal.ZERO);
        }

        return dto;
    }
}
