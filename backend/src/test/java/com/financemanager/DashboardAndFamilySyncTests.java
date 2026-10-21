package com.financemanager;

import com.financemanager.dto.DashboardSummaryDto;
import com.financemanager.dto.TransactionDto;
import com.financemanager.entity.*;
import com.financemanager.exception.ForbiddenException;
import com.financemanager.repository.*;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class DashboardAndFamilySyncTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FamilyLinkRepository familyLinkRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ReportService reportService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private FamilyLinkService familyLinkService;

    private User student1;
    private User student2;
    private User parent;

    @BeforeEach
    void setup() {
        student1 = userRepository.findByEmail("student@university.edu").orElseThrow();
        parent = userRepository.findByEmail("parent@family.com").orElseThrow();

        // Ensure student2 exists
        student2 = userRepository.findByEmail("secondstudent@university.edu")
                .orElseGet(() -> userRepository.save(new User(
                        "secondstudent@university.edu", "password", "Second Student", Role.STUDENT, "STU-SECOND-1234"
                )));
    }

    @Test
    @DisplayName("1. Student dashboard accurately totals income, expense, cash flow, and balance from transactions")
    void testStudentDashboardTotalsAccurate() {
        transactionRepository.deleteAll();

        TransactionDto income1 = new TransactionDto();
        income1.setType(TransactionType.INCOME);
        income1.setAmount(new BigDecimal("10000.00"));
        income1.setCategory("Allowance");
        income1.setDescription("Allowance from family");
        income1.setTransactionDate(LocalDate.now());
        transactionService.createTransaction(student1, income1);

        TransactionDto exp1 = new TransactionDto();
        exp1.setType(TransactionType.EXPENSE);
        exp1.setAmount(new BigDecimal("2500.00"));
        exp1.setCategory("Food & Mess");
        exp1.setDescription("Campus meals");
        exp1.setTransactionDate(LocalDate.now());
        transactionService.createTransaction(student1, exp1);

        TransactionDto exp2 = new TransactionDto();
        exp2.setType(TransactionType.EXPENSE);
        exp2.setAmount(new BigDecimal("1500.00"));
        exp2.setCategory("Books & Supplies");
        exp2.setDescription("Textbooks");
        exp2.setTransactionDate(LocalDate.now());
        transactionService.createTransaction(student1, exp2);

        DashboardSummaryDto dash = reportService.getDashboardSummary(student1);

        assertEquals("STUDENT", dash.getUserRole());
        assertFalse(dash.isReadOnly());
        assertEquals(new BigDecimal("10000.00"), dash.getTotalIncome().setScale(2));
        assertEquals(new BigDecimal("4000.00"), dash.getTotalExpenses().setScale(2));
        assertEquals(new BigDecimal("6000.00"), dash.getNetCashFlow().setScale(2));
        assertEquals(new BigDecimal("6000.00"), dash.getTrackedBalance().setScale(2));
        assertEquals(3, dash.getRecentTransactions().size());
        assertEquals(new BigDecimal("2500.00"), dash.getCategoryExpenses().get("Food & Mess").setScale(2));
        assertEquals(new BigDecimal("1500.00"), dash.getCategoryExpenses().get("Books & Supplies").setScale(2));
    }

    @Test
    @DisplayName("2. Parent with multiple active student links never throws NonUniqueResultException")
    void testParentMultipleActiveStudentsNoCrash() {
        transactionRepository.deleteAll();
        familyLinkRepository.deleteAll();

        FamilyLink link1 = new FamilyLink(parent, student1, LinkStatus.ACTIVE, "PARENT");
        link1.setApprovedAt(LocalDateTime.now().minusDays(2));
        familyLinkRepository.save(link1);

        FamilyLink link2 = new FamilyLink(parent, student2, LinkStatus.ACTIVE, "PARENT");
        link2.setApprovedAt(LocalDateTime.now().minusDays(1));
        familyLinkRepository.save(link2);

        // Seed transactions for student 1
        TransactionDto s1Exp = new TransactionDto();
        s1Exp.setType(TransactionType.EXPENSE);
        s1Exp.setAmount(new BigDecimal("800.00"));
        s1Exp.setCategory("Food & Mess");
        s1Exp.setDescription("Student 1 snack");
        s1Exp.setTransactionDate(LocalDate.now());
        transactionService.createTransaction(student1, s1Exp);

        // Seed transactions for student 2
        TransactionDto s2Exp = new TransactionDto();
        s2Exp.setType(TransactionType.EXPENSE);
        s2Exp.setAmount(new BigDecimal("1200.00"));
        s2Exp.setCategory("Books & Supplies");
        s2Exp.setDescription("Student 2 books");
        s2Exp.setTransactionDate(LocalDate.now());
        transactionService.createTransaction(student2, s2Exp);

        // Query dashboard without studentId (defaults to most recently approved: student2)
        DashboardSummaryDto defaultDash = assertDoesNotThrow(() -> reportService.getDashboardSummary(parent));
        assertEquals("PARENT", defaultDash.getUserRole());
        assertTrue(defaultDash.isReadOnly());
        assertEquals(2, defaultDash.getAvailableStudents().size());
        assertEquals(student2.getId(), defaultDash.getStudentId());
        assertEquals(new BigDecimal("1200.00"), defaultDash.getTotalExpenses().setScale(2));
        assertEquals(BigDecimal.ZERO, defaultDash.getTotalIncome()); // Income never leaked

        // Query dashboard specifically for student 1
        DashboardSummaryDto s1Dash = reportService.getDashboardSummary(parent, student1.getId());
        assertEquals(student1.getId(), s1Dash.getStudentId());
        assertEquals(new BigDecimal("800.00"), s1Dash.getTotalExpenses().setScale(2));

        // Query transactions for parent with student selection
        List<TransactionDto> s1Tx = transactionService.getTransactions(parent, null, null, null, null, student1.getId());
        assertEquals(1, s1Tx.size());
        assertEquals("Student 1 snack", s1Tx.get(0).getDescription());

        List<TransactionDto> s2Tx = transactionService.getTransactions(parent, null, null, null, null, student2.getId());
        assertEquals(1, s2Tx.size());
        assertEquals("Student 2 books", s2Tx.get(0).getDescription());
    }

    @Test
    @DisplayName("3. Parent with no active links returns UNLINKED status without throwing exception")
    void testParentUnlinkedGracefulHandling() {
        familyLinkRepository.deleteAll();

        DashboardSummaryDto dash = assertDoesNotThrow(() -> reportService.getDashboardSummary(parent));
        assertEquals("PARENT", dash.getUserRole());
        assertEquals("UNLINKED", dash.getLinkStatus());
        assertEquals(BigDecimal.ZERO, dash.getTotalExpenses());
        assertEquals(BigDecimal.ZERO, dash.getTotalIncome());
        assertEquals(BigDecimal.ZERO, dash.getNetCashFlow());
        assertTrue(dash.getRecentTransactions().isEmpty());
        assertTrue(dash.getCategoryExpenses().isEmpty());

        // Transactions endpoint for unlinked parent throws ForbiddenException (security requirement)
        assertThrows(ForbiddenException.class, () -> transactionService.getTransactions(parent, null, null, null, null));
    }

    @Test
    @DisplayName("4. Manual and imported transactions reflect identically in persistent storage and dashboard")
    void testManualAndImportedTransactionsConsistency() {
        transactionRepository.deleteAll();

        // 1 manual
        TransactionDto manual = new TransactionDto();
        manual.setType(TransactionType.EXPENSE);
        manual.setAmount(new BigDecimal("300.00"));
        manual.setCategory("Food & Mess");
        manual.setDescription("Manual Sandwich");
        manual.setTransactionDate(LocalDate.now());
        manual.setSource("MANUAL");
        transactionService.createTransaction(student1, manual);

        // 1 imported
        TransactionDto imported = new TransactionDto();
        imported.setType(TransactionType.EXPENSE);
        imported.setAmount(new BigDecimal("700.00"));
        imported.setCategory("Books & Supplies");
        imported.setDescription("Imported Stationary");
        imported.setTransactionDate(LocalDate.now());
        imported.setSource("IMPORT");
        transactionService.createTransaction(student1, imported);

        DashboardSummaryDto dash = reportService.getDashboardSummary(student1);
        assertEquals(new BigDecimal("1000.00"), dash.getTotalExpenses().setScale(2));
        assertEquals(2, dash.getRecentTransactions().size());

        List<Transaction> allInRepo = transactionRepository.findByOwnerOrderByTransactionDateDescCreatedAtDesc(student1);
        assertEquals(2, allInRepo.size());
        assertTrue(allInRepo.stream().anyMatch(t -> "MANUAL".equals(t.getSource())));
        assertTrue(allInRepo.stream().anyMatch(t -> "IMPORT".equals(t.getSource())));
    }
}
