package com.financemanager;

import com.financemanager.dto.BudgetDto;
import com.financemanager.dto.DashboardSummaryDto;
import com.financemanager.dto.FamilyLinkDto;
import com.financemanager.dto.TransactionDto;
import com.financemanager.entity.Role;
import com.financemanager.entity.TransactionType;
import com.financemanager.entity.User;
import com.financemanager.exception.ForbiddenException;
import com.financemanager.repository.UserRepository;
import com.financemanager.service.BudgetService;
import com.financemanager.service.FamilyLinkService;
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
public class ParentLinkingAndAccessTests {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private BudgetService budgetService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private FamilyLinkService familyLinkService;

    @Autowired
    private UserRepository userRepository;

    private User student;
    private User parent;

    @BeforeEach
    void setUp() {
        student = userRepository.findByEmail("student@university.edu").orElseThrow();
        parent = userRepository.findByEmail("parent@family.com").orElseThrow();
    }

    @Test
    @DisplayName("Verify Parent can view permitted student expenses in read-only mode")
    void testParentReadPermittedExpenses() {
        DashboardSummaryDto summary = reportService.getDashboardSummary(parent);

        assertNotNull(summary);
        assertTrue(summary.isReadOnly());
        assertEquals("PARENT", summary.getUserRole());
        assertEquals(student.getDisplayName(), summary.getStudentName());
        assertTrue(summary.getTotalExpenses().compareTo(BigDecimal.ZERO) > 0);
        // Student income must NOT be exposed to parent
        assertEquals(BigDecimal.ZERO, summary.getTotalIncome());

        // Transactions list must contain only EXPENSES
        List<TransactionDto> txs = transactionService.getTransactions(parent, null, null, null, null);
        assertFalse(txs.isEmpty());
        for (TransactionDto tx : txs) {
            assertEquals(TransactionType.EXPENSE, tx.getType());
        }
    }

    @Test
    @DisplayName("Verify Parent mutation requests are strictly rejected with 403 Forbidden")
    void testParentMutationsForbidden() {
        // 1. Transaction creation attempt by parent
        TransactionDto txDto = new TransactionDto();
        txDto.setType(TransactionType.EXPENSE);
        txDto.setAmount(new BigDecimal("100.00"));
        txDto.setCategory("Food & Mess");
        txDto.setDescription("Parent Added Snack");
        txDto.setTransactionDate(LocalDate.now());

        assertThrows(ForbiddenException.class, () -> transactionService.createTransaction(parent, txDto),
                "Parent creating transaction must fail with ForbiddenException");

        // 2. Budget modification attempt by parent
        BudgetDto budgetDto = new BudgetDto();
        budgetDto.setCategory("Food & Mess");
        budgetDto.setPeriodMonth("2026-10");
        budgetDto.setLimitAmount(new BigDecimal("5000.00"));

        assertThrows(ForbiddenException.class, () -> budgetService.createOrUpdateBudget(parent, budgetDto),
                "Parent modifying budget must fail with ForbiddenException");
    }

    @Test
    @DisplayName("Verify that revoking family link immediately cuts off Parent access")
    void testRevokeLinkCutsOffAccess() {
        List<FamilyLinkDto> links = familyLinkService.getMyLinks(student);
        assertFalse(links.isEmpty());
        Long linkId = links.get(0).getId();

        // Student revokes link
        familyLinkService.revokeLink(student, linkId);

        // Parent should now be rejected when querying student data
        assertThrows(ForbiddenException.class, () -> transactionService.getTransactions(parent, null, null, null, null));
    }
}
