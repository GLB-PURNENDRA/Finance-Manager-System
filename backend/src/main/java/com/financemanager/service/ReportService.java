package com.financemanager.service;

import com.financemanager.dto.DashboardSummaryDto;
import com.financemanager.dto.TransactionDto;
import com.financemanager.entity.*;
import com.financemanager.exception.ForbiddenException;
import com.financemanager.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private SavingsGoalRepository savingsGoalRepository;

    @Autowired
    private InvestmentRepository investmentRepository;

    @Autowired
    private FamilyLinkService familyLinkService;

    @Autowired
    private TransactionService transactionService;

    public DashboardSummaryDto getDashboardSummary(User currentUser) {
        return getDashboardSummary(currentUser, null);
    }

    public DashboardSummaryDto getDashboardSummary(User currentUser, Long studentId) {
        DashboardSummaryDto summary = new DashboardSummaryDto();

        if (currentUser.getRole() == Role.PARENT) {
            // Parent view
            summary.setUserRole("PARENT");
            summary.setReadOnly(true);

            List<com.financemanager.dto.FamilyLinkDto> availableStudents = familyLinkService.getActiveLinksForParent(currentUser);
            summary.setAvailableStudents(availableStudents);

            Optional<FamilyLink> activeLink = familyLinkService.getActiveLinkForParent(currentUser, studentId);
            if (activeLink.isEmpty()) {
                summary.setLinkStatus("UNLINKED");
                summary.setTotalIncome(BigDecimal.ZERO);
                summary.setTotalExpenses(BigDecimal.ZERO);
                summary.setNetCashFlow(BigDecimal.ZERO);
                summary.setTrackedBalance(BigDecimal.ZERO);
                summary.setCategoryExpenses(Collections.emptyMap());
                summary.setRecentTransactions(Collections.emptyList());
                summary.setMonthlyTrends(computeMonthlyTrends(Collections.emptyList(), true));
                return summary;
            }

            FamilyLink link = activeLink.get();
            summary.setLinkStatus(link.getStatus().name());
            User student = link.getStudent();
            summary.setStudentId(student.getId());
            summary.setStudentName(student.getDisplayName());

            // Parent sees only permitted expenses
            List<Transaction> studentExpenses = transactionRepository.findByOwnerAndTypeOrderByTransactionDateDesc(
                    student, TransactionType.EXPENSE);

            BigDecimal totalExpenses = studentExpenses.stream()
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            summary.setTotalExpenses(totalExpenses);
            summary.setTotalIncome(BigDecimal.ZERO); // Not shared with parent
            summary.setNetCashFlow(BigDecimal.ZERO.subtract(totalExpenses));
            summary.setTrackedBalance(BigDecimal.ZERO);

            // Category breakdown of expenses
            Map<String, BigDecimal> categoryExpenses = new HashMap<>();
            for (Transaction tx : studentExpenses) {
                categoryExpenses.merge(tx.getCategory(), tx.getAmount(), BigDecimal::add);
            }
            summary.setCategoryExpenses(categoryExpenses);

            // Recent permitted transactions
            summary.setRecentTransactions(studentExpenses.stream()
                    .limit(10)
                    .map(transactionService::toDto)
                    .collect(Collectors.toList()));

            // Monthly trends for expenses
            summary.setMonthlyTrends(computeMonthlyTrends(studentExpenses, true));

            // Budgets count
            String currentMonth = YearMonth.now().toString();
            summary.setActiveBudgetsCount(budgetRepository.findByStudentAndPeriodMonth(student, currentMonth).size());
            summary.setActiveGoalsCount(savingsGoalRepository.findByStudentOrderByCreatedAtDesc(student).size());

        } else {
            // Student view
            summary.setUserRole("STUDENT");
            summary.setReadOnly(false);
            summary.setStudentId(currentUser.getId());
            summary.setStudentName(currentUser.getDisplayName());

            List<Transaction> allTx = transactionRepository.findByOwnerOrderByTransactionDateDescCreatedAtDesc(currentUser);

            BigDecimal totalIncome = BigDecimal.ZERO;
            BigDecimal totalExpenses = BigDecimal.ZERO;
            Map<String, BigDecimal> categoryExpenses = new HashMap<>();

            for (Transaction tx : allTx) {
                if (tx.getType() == TransactionType.INCOME) {
                    totalIncome = totalIncome.add(tx.getAmount());
                } else {
                    totalExpenses = totalExpenses.add(tx.getAmount());
                    categoryExpenses.merge(tx.getCategory(), tx.getAmount(), BigDecimal::add);
                }
            }

            summary.setTotalIncome(totalIncome);
            summary.setTotalExpenses(totalExpenses);
            summary.setNetCashFlow(totalIncome.subtract(totalExpenses));
            // Tracked balance = 0 + totalIncome - totalExpenses
            summary.setTrackedBalance(totalIncome.subtract(totalExpenses));
            summary.setCategoryExpenses(categoryExpenses);

            // Portfolio values
            List<Investment> investments = investmentRepository.findByStudentOrderByCreatedAtDesc(currentUser);
            BigDecimal totalInvested = BigDecimal.ZERO;
            BigDecimal totalCurrent = BigDecimal.ZERO;
            for (Investment inv : investments) {
                totalInvested = totalInvested.add(inv.getInvestedAmount());
                totalCurrent = totalCurrent.add(inv.getCurrentValue());
            }
            summary.setTotalInvested(totalInvested);
            summary.setTotalCurrentPortfolioValue(totalCurrent);
            summary.setTotalPortfolioProfitLoss(totalCurrent.subtract(totalInvested));

            String currentMonth = YearMonth.now().toString();
            summary.setActiveBudgetsCount(budgetRepository.findByStudentAndPeriodMonth(currentUser, currentMonth).size());
            summary.setActiveGoalsCount(savingsGoalRepository.findByStudentOrderByCreatedAtDesc(currentUser).size());

            summary.setRecentTransactions(allTx.stream()
                    .limit(10)
                    .map(transactionService::toDto)
                    .collect(Collectors.toList()));

            summary.setMonthlyTrends(computeMonthlyTrends(allTx, false));
        }

        return summary;
    }

    private List<Map<String, Object>> computeMonthlyTrends(List<Transaction> transactions, boolean expenseOnly) {
        Map<String, BigDecimal[]> monthMap = new TreeMap<>(); // Key: "YYYY-MM", Value: [income, expense]

        // Populate last 6 months
        YearMonth current = YearMonth.now();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = current.minusMonths(i);
            monthMap.put(ym.toString(), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }

        for (Transaction tx : transactions) {
            String ym = YearMonth.from(tx.getTransactionDate()).toString();
            if (monthMap.containsKey(ym)) {
                BigDecimal[] pair = monthMap.get(ym);
                if (tx.getType() == TransactionType.INCOME && !expenseOnly) {
                    pair[0] = pair[0].add(tx.getAmount());
                } else if (tx.getType() == TransactionType.EXPENSE) {
                    pair[1] = pair[1].add(tx.getAmount());
                }
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("MMM yyyy");
        for (Map.Entry<String, BigDecimal[]> entry : monthMap.entrySet()) {
            YearMonth ym = YearMonth.parse(entry.getKey());
            Map<String, Object> item = new HashMap<>();
            item.put("monthKey", entry.getKey());
            item.put("monthLabel", ym.format(displayFormatter));
            item.put("income", entry.getValue()[0]);
            item.put("expense", entry.getValue()[1]);
            item.put("net", entry.getValue()[0].subtract(entry.getValue()[1]));
            result.add(item);
        }
        return result;
    }

    public String exportTransactionsCsv(User currentUser) {
        List<TransactionDto> list = transactionService.getTransactions(currentUser, null, null, null, null);
        StringBuilder sb = new StringBuilder();
        sb.append("ID,Date,Type,Category,Description,Amount,Payment Method,Source\n");
        for (TransactionDto tx : list) {
            sb.append(tx.getId()).append(",")
                    .append(tx.getTransactionDate()).append(",")
                    .append(tx.getType()).append(",")
                    .append(escapeCsv(tx.getCategory())).append(",")
                    .append(escapeCsv(tx.getDescription())).append(",")
                    .append(tx.getAmount()).append(",")
                    .append(escapeCsv(tx.getPaymentMethod() != null ? tx.getPaymentMethod() : "")).append(",")
                    .append(tx.getSource() != null ? tx.getSource() : "MANUAL")
                    .append("\n");
        }
        return sb.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
