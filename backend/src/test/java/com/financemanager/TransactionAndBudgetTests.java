package com.financemanager;

import com.financemanager.dto.BudgetDto;
import com.financemanager.dto.DashboardSummaryDto;
import com.financemanager.dto.TransactionDto;
import com.financemanager.entity.TransactionType;
import com.financemanager.entity.User;
import com.financemanager.repository.UserRepository;
import com.financemanager.service.BudgetService;
import com.financemanager.service.ReportService;
import com.financemanager.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class TransactionAndBudgetTests {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestSeedHelper testSeedHelper;

    private User student;

    @BeforeEach
    void setUp() {
        testSeedHelper.ensureTestData();
        student = userRepository.findByEmail("student@university.edu").orElseThrow();
    }

    @Test
    @DisplayName("Verify Student can create, update, and delete own transactions")
    void testTransactionCrud() {
        // Create
        TransactionDto dto = new TransactionDto();
        dto.setType(TransactionType.EXPENSE);
        dto.setAmount(new BigDecimal("750.00"));
        dto.setCategory("Books & Supplies");
        dto.setDescription("Operating Systems Textbook");
        dto.setTransactionDate(LocalDate.of(2026, 10, 15));
        dto.setPaymentMethod("UPI");

        TransactionDto created = transactionService.createTransaction(student, dto);
        assertNotNull(created.getId());
        assertEquals(new BigDecimal("750.00"), created.getAmount());

        // Update
        created.setAmount(new BigDecimal("800.00"));
        TransactionDto updated = transactionService.updateTransaction(student, created.getId(), created);
        assertEquals(new BigDecimal("800.00"), updated.getAmount());

        // Delete
        transactionService.deleteTransaction(student, created.getId());
        assertThrows(RuntimeException.class, () -> transactionService.getTransactionById(student, created.getId()));
    }

    @Test
    @DisplayName("Verify Dashboard calculation accuracy: income, expenses, and net cash flow")
    void testDashboardCalculations() {
        DashboardSummaryDto summary = reportService.getDashboardSummary(student);

        assertNotNull(summary);
        assertTrue(summary.getTotalIncome().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(summary.getTotalExpenses().compareTo(BigDecimal.ZERO) > 0);

        BigDecimal expectedNet = summary.getTotalIncome().subtract(summary.getTotalExpenses());
        assertEquals(expectedNet, summary.getNetCashFlow());
        assertEquals(expectedNet, summary.getTrackedBalance());
    }

    @Test
    @DisplayName("Verify Budget calculations, warning threshold, and overbudget detection")
    void testBudgetCalculations() {
        BudgetDto budget = new BudgetDto();
        budget.setCategory("Food & Mess");
        budget.setPeriodMonth("2026-10");
        budget.setLimitAmount(new BigDecimal("3000.00"));
        budget.setWarningThreshold(new BigDecimal("80.00"));

        BudgetDto saved = budgetService.createOrUpdateBudget(student, budget);
        assertNotNull(saved);

        // Actual spent in seeded Food & Mess is 3200 + 45 + 55 + 50 = 3350
        assertTrue(saved.getActualSpent().compareTo(new BigDecimal("3000.00")) > 0);
        assertTrue(saved.isOverBudget());
        assertTrue(saved.isWarningTriggered());
        assertTrue(saved.getRemainingAmount().compareTo(BigDecimal.ZERO) < 0);
    }
}
