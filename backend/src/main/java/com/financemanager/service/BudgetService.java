package com.financemanager.service;

import com.financemanager.dto.BudgetDto;
import com.financemanager.entity.Budget;
import com.financemanager.entity.Role;
import com.financemanager.entity.User;
import com.financemanager.exception.ForbiddenException;
import com.financemanager.exception.ResourceNotFoundException;
import com.financemanager.repository.BudgetRepository;
import com.financemanager.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BudgetService {

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private FamilyLinkService familyLinkService;

    @Autowired
    private AuditService auditService;

    public List<BudgetDto> getBudgets(User currentUser, String periodMonth) {
        User targetStudent;
        if (currentUser.getRole() == Role.PARENT) {
            targetStudent = familyLinkService.getActiveStudentForParent(currentUser);
        } else {
            targetStudent = currentUser;
        }

        List<Budget> budgets;
        if (periodMonth != null && !periodMonth.isBlank()) {
            budgets = budgetRepository.findByStudentAndPeriodMonth(targetStudent, periodMonth.trim());
        } else {
            budgets = budgetRepository.findByStudentOrderByPeriodMonthDesc(targetStudent);
        }

        return budgets.stream().map(b -> toDtoWithCalculations(b, targetStudent)).collect(Collectors.toList());
    }

    public BudgetDto createOrUpdateBudget(User currentUser, BudgetDto dto) {
        if (currentUser.getRole() == Role.PARENT) {
            throw new ForbiddenException("Parents have strictly read-only access and cannot alter budgets.");
        }

        String periodMonth = dto.getPeriodMonth().trim();
        String category = dto.getCategory().trim();

        Optional<Budget> existing = budgetRepository.findByStudentAndCategoryAndPeriodMonth(currentUser, category, periodMonth);
        Budget budget;
        if (existing.isPresent()) {
            budget = existing.get();
            budget.setLimitAmount(dto.getLimitAmount());
            if (dto.getWarningThreshold() != null) {
                budget.setWarningThreshold(dto.getWarningThreshold());
            }
        } else {
            budget = new Budget(
                    currentUser,
                    category,
                    periodMonth,
                    dto.getLimitAmount(),
                    dto.getWarningThreshold() != null ? dto.getWarningThreshold() : new BigDecimal("80.00")
            );
        }

        budget = budgetRepository.save(budget);
        auditService.log(currentUser, "BUDGET_SAVED", "BUDGET", budget.getId().toString(),
                "Category: " + budget.getCategory() + ", Limit: " + budget.getLimitAmount());

        return toDtoWithCalculations(budget, currentUser);
    }

    public void deleteBudget(User currentUser, Long id) {
        if (currentUser.getRole() == Role.PARENT) {
            throw new ForbiddenException("Parents have strictly read-only access and cannot alter budgets.");
        }

        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found."));

        if (!budget.getStudent().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You can only delete your own budgets.");
        }

        budgetRepository.delete(budget);
        auditService.log(currentUser, "BUDGET_DELETED", "BUDGET", id.toString(), "Deleted by student");
    }

    public BudgetDto toDtoWithCalculations(Budget budget, User student) {
        BudgetDto dto = new BudgetDto();
        dto.setId(budget.getId());
        dto.setCategory(budget.getCategory());
        dto.setPeriodMonth(budget.getPeriodMonth());
        dto.setLimitAmount(budget.getLimitAmount());
        dto.setWarningThreshold(budget.getWarningThreshold());

        // Calculate actual spending for this category in the budget month
        YearMonth ym = YearMonth.parse(budget.getPeriodMonth());
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();

        BigDecimal actualSpent = transactionRepository.sumExpenseByOwnerAndCategoryAndDateRange(
                student, budget.getCategory(), startDate, endDate);

        if (actualSpent == null) actualSpent = BigDecimal.ZERO;

        dto.setActualSpent(actualSpent);
        BigDecimal remaining = budget.getLimitAmount().subtract(actualSpent);
        dto.setRemainingAmount(remaining);

        if (budget.getLimitAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal percentage = actualSpent.multiply(new BigDecimal("100"))
                    .divide(budget.getLimitAmount(), 2, RoundingMode.HALF_UP);
            dto.setPercentageSpent(percentage);
            dto.setOverBudget(actualSpent.compareTo(budget.getLimitAmount()) > 0);
            dto.setWarningTriggered(percentage.compareTo(budget.getWarningThreshold()) >= 0);
        }

        return dto;
    }
}
