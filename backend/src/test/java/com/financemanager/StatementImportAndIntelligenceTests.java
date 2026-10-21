package com.financemanager;

import com.financemanager.dto.*;
import com.financemanager.entity.User;
import com.financemanager.repository.UserRepository;
import com.financemanager.service.IntelligenceService;
import com.financemanager.service.StatementImportService;
import com.financemanager.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class StatementImportAndIntelligenceTests {

    @Autowired
    private StatementImportService statementImportService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private IntelligenceService intelligenceService;

    @Autowired
    private UserRepository userRepository;

    private User student;

    @BeforeEach
    void setUp() {
        student = userRepository.findByEmail("student@university.edu").orElseThrow();
    }

    @Test
    @DisplayName("Verify CSV Statement parsing, column mapping, and duplicate detection")
    void testStatementImportAndDuplicateDetection() {
        // Prepare CSV content: 1 new transaction and 1 exact duplicate of seeded transaction #4 (3200 Food & Mess on 2026-10-03)
        String csvContent = "Date,Type,Amount,Description,Category,Payment Method\n" +
                "2026-10-03,EXPENSE,3200.00,Campus Monthly Mess Dues,Food & Mess,UPI\n" +
                "2026-10-18,EXPENSE,250.00,Campus Stationery Notebook,Books & Supplies,Cash\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "statement.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        StatementPreviewDto preview = statementImportService.parseCsvStatement(student, file);
        assertNotNull(preview);
        assertEquals(2, preview.getTotalRows());
        assertEquals(1, preview.getDuplicateRows());
        assertEquals(1, preview.getValidRows());

        // First row must be flagged duplicate
        assertTrue(preview.getRows().get(0).isDuplicate());
        assertFalse(preview.getRows().get(0).isSelected());

        // Second row is new and selected
        assertFalse(preview.getRows().get(1).isDuplicate());
        assertTrue(preview.getRows().get(1).isSelected());
    }

    @Test
    @DisplayName("Verify Money Leak Detector identifies repetitive canteen micro-expenses")
    void testMoneyLeakDetector() {
        List<IntelligenceReportDto.MoneyLeakItem> leaks = intelligenceService.detectMoneyLeaks(student);
        assertNotNull(leaks);
        assertFalse(leaks.isEmpty(), "Should detect repeated canteen tea & biscuits");

        IntelligenceReportDto.MoneyLeakItem topLeak = leaks.get(0);
        assertTrue(topLeak.getFrequency() >= 2);
        assertTrue(topLeak.getTotalSpent().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("Verify What-If Simulator projects net cash flow without altering records")
    void testWhatIfSimulator() {
        WhatIfRequestDto req = new WhatIfRequestDto();
        req.setMonthlyIncomeAdjustment(new BigDecimal("2000.00"));
        req.setCategoryExpenseAdjustments(Map.of("Food & Mess", new BigDecimal("500.00")));
        req.setMonthlySipAdjustment(new BigDecimal("1000.00"));

        WhatIfResponseDto res = intelligenceService.simulateWhatIf(student, req);
        assertNotNull(res);
        assertNotNull(res.getExplanation());
        assertTrue(res.getDisclaimer().contains("SIMULATION ONLY"));
        assertTrue(res.getProjectedAnnualSavingsImpact().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("Verify Ask Your Money AI provides verified financial answers")
    void testAskMoneyAi() {
        AiQuestionResponseDto response = intelligenceService.askMoneyQuestion(student, "Which category did I spend the most on?");
        assertNotNull(response);
        assertNotNull(response.getAnswer());
        assertFalse(response.getEvidenceRecords().isEmpty());
    }

    @Test
    @DisplayName("Verify PDF Statement parsing with Debit, Credit, Balance extraction and duplicate detection")
    void testPdfStatementImportAndDuplicateDetection() throws Exception {
        byte[] pdfBytes;
        try (org.apache.pdfbox.pdmodel.PDDocument doc = new org.apache.pdfbox.pdmodel.PDDocument()) {
            org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage();
            doc.addPage(page);
            try (org.apache.pdfbox.pdmodel.PDPageContentStream cs = new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA), 10);
                cs.newLineAtOffset(50, 700);
                cs.showText("HDFC BANK ACCOUNT STATEMENT");
                cs.newLineAtOffset(0, -20);
                cs.showText("Date Narration Chq/Ref Withdrawal Deposit Balance");
                cs.newLineAtOffset(0, -15);
                // Duplicate of seeded txn: Campus Monthly Mess Dues 3200 on 2026-10-03
                cs.showText("2026-10-03 Campus Monthly Mess Dues 528392019 3200.00 0.00 16800.00");
                cs.newLineAtOffset(0, -15);
                // New Expense:
                cs.showText("2026-10-18 Campus Stationery Books 9928120 450.00 0.00 16350.00");
                cs.newLineAtOffset(0, -15);
                // New Income:
                cs.showText("2026-10-19 Freelance Stipend Credit 882710 0.00 2500.00 18850.00");
                cs.endText();
            }
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            doc.save(baos);
            pdfBytes = baos.toByteArray();
        }

        MockMultipartFile file = new MockMultipartFile(
                "file", "hdfc_statement.pdf", "application/pdf", pdfBytes);

        StatementPreviewDto preview = statementImportService.parsePdfStatement(student, file);
        assertNotNull(preview);
        assertEquals(3, preview.getTotalRows());
        assertEquals(1, preview.getDuplicateRows());
        assertEquals(2, preview.getValidRows());

        // Row 1: Duplicate flagged & unselected
        StatementRowDto r1 = preview.getRows().get(0);
        assertTrue(r1.isDuplicate());
        assertFalse(r1.isSelected());
        assertEquals(new BigDecimal("3200.00"), r1.getDebit());
        assertEquals(new BigDecimal("16800.00"), r1.getBalance());

        // Row 2: New expense
        StatementRowDto r2 = preview.getRows().get(1);
        assertFalse(r2.isDuplicate());
        assertTrue(r2.isSelected());
        assertEquals(com.financemanager.entity.TransactionType.EXPENSE, r2.getType());
        assertEquals(new BigDecimal("450.00"), r2.getDebit());
        assertEquals(new BigDecimal("16350.00"), r2.getBalance());
        assertEquals("Books & Supplies", r2.getCategory());

        // Row 3: New income
        StatementRowDto r3 = preview.getRows().get(2);
        assertFalse(r3.isDuplicate());
        assertTrue(r3.isSelected());
        assertEquals(com.financemanager.entity.TransactionType.INCOME, r3.getType());
        assertEquals(new BigDecimal("2500.00"), r3.getCredit());
        assertEquals(new BigDecimal("18850.00"), r3.getBalance());
        assertEquals("Allowance", r3.getCategory());
    }

    @Test
    @DisplayName("Verify scanned / image-only PDF rejects with clear OCR error")
    void testScannedPdfRejectionWithOcrError() throws Exception {
        byte[] emptyPdfBytes;
        try (org.apache.pdfbox.pdmodel.PDDocument doc = new org.apache.pdfbox.pdmodel.PDDocument()) {
            org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage();
            doc.addPage(page);
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            doc.save(baos);
            emptyPdfBytes = baos.toByteArray();
        }

        MockMultipartFile file = new MockMultipartFile(
                "file", "scanned_statement.pdf", "application/pdf", emptyPdfBytes);

        com.financemanager.exception.BadRequestException ex = assertThrows(
                com.financemanager.exception.BadRequestException.class,
                () -> statementImportService.parsePdfStatement(student, file)
        );
        assertTrue(ex.getMessage().contains("OCR") || ex.getMessage().contains("scanned"),
                "Error should inform the user that scanned PDFs require OCR");
    }

    @Test
    @DisplayName("Verify password-protected PDF rejects with clear password error")
    void testPasswordProtectedPdfRejection() throws Exception {
        byte[] encryptedPdfBytes;
        try (org.apache.pdfbox.pdmodel.PDDocument doc = new org.apache.pdfbox.pdmodel.PDDocument()) {
            org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage();
            doc.addPage(page);
            try (org.apache.pdfbox.pdmodel.PDPageContentStream cs = new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA), 10);
                cs.newLineAtOffset(50, 700);
                cs.showText("Protected Account Statement 15/10/2026");
                cs.endText();
            }
            org.apache.pdfbox.pdmodel.encryption.AccessPermission ap = new org.apache.pdfbox.pdmodel.encryption.AccessPermission();
            org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy spp =
                    new org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy("owner123", "secret", ap);
            doc.protect(spp);

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            doc.save(baos);
            encryptedPdfBytes = baos.toByteArray();
        }

        MockMultipartFile file = new MockMultipartFile(
                "file", "locked_statement.pdf", "application/pdf", encryptedPdfBytes);

        com.financemanager.exception.BadRequestException ex = assertThrows(
                com.financemanager.exception.BadRequestException.class,
                () -> statementImportService.parsePdfStatement(student, file)
        );
        assertTrue(ex.getMessage().toLowerCase().contains("password"),
                "Error should inform the user that the PDF is password-protected");
    }

    @Test
    @DisplayName("Verify manual expense is correctly flagged as duplicate when same transaction appears in CSV")
    void testManualExpenseVsCsvDuplicateDetection() {
        // 1. Manually add an expense
        TransactionDto manualTx = new TransactionDto();
        manualTx.setType(com.financemanager.entity.TransactionType.EXPENSE);
        manualTx.setAmount(new BigDecimal("420.00"));
        manualTx.setDescription("Campus Stationery Supplies");
        manualTx.setCategory("Books & Supplies");
        manualTx.setTransactionDate(java.time.LocalDate.of(2026, 10, 15));
        manualTx.setPaymentMethod("UPI");
        TransactionDto savedManual = transactionService.createTransaction(student, manualTx);

        // 2. CSV containing the same transaction with whitespace/casing variations
        String csv = "Date,Type,Amount,Description,Category\n" +
                "2026-10-15,EXPENSE,420.00,  campus stationery supplies  ,Books & Supplies\n";
        MockMultipartFile file = new MockMultipartFile("file", "test_manual.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        StatementPreviewDto preview = statementImportService.parseCsvStatement(student, file);
        assertNotNull(preview);
        assertEquals(1, preview.getTotalRows());
        assertEquals(1, preview.getDuplicateRows());

        StatementRowDto row = preview.getRows().get(0);
        assertTrue(row.isDuplicate());
        assertFalse(row.isSelected());
        assertTrue(row.getDuplicateReason().contains("transaction #" + savedManual.getId()));
    }

    @Test
    @DisplayName("Verify CSV import then same CSV uploaded again flags duplicates")
    void testCsvImportThenSameCsvUploadedAgain() {
        String csv = "Date,Type,Amount,Description,Category\n" +
                "2026-10-23,EXPENSE,175.50,Campus Evening Canteen,Food & Mess\n";
        MockMultipartFile file = new MockMultipartFile("file", "canteen.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        // First upload & commit
        StatementPreviewDto preview1 = statementImportService.parseCsvStatement(student, file);
        assertEquals(1, preview1.getTotalRows());
        assertEquals(0, preview1.getDuplicateRows());
        assertTrue(preview1.getRows().get(0).isSelected());

        statementImportService.commitImport(student, preview1.getRows());

        // Second upload of same file
        StatementPreviewDto preview2 = statementImportService.parseCsvStatement(student, file);
        assertEquals(1, preview2.getTotalRows());
        assertEquals(1, preview2.getDuplicateRows());
        assertTrue(preview2.getRows().get(0).isDuplicate());
        assertFalse(preview2.getRows().get(0).isSelected());
    }

    @Test
    @DisplayName("Verify different expenses with same amount on same date are NOT flagged as duplicates")
    void testDifferentExpensesWithSameAmountOnSameDate() {
        // 1. Existing expense in database
        TransactionDto tx1 = new TransactionDto();
        tx1.setType(com.financemanager.entity.TransactionType.EXPENSE);
        tx1.setAmount(new BigDecimal("150.00"));
        tx1.setDescription("Campus Canteen Lunch");
        tx1.setCategory("Food & Mess");
        tx1.setTransactionDate(java.time.LocalDate.of(2026, 10, 14));
        transactionService.createTransaction(student, tx1);

        // 2. CSV with different expense on same date with same amount
        String csv = "Date,Type,Amount,Description,Category\n" +
                "2026-10-14,EXPENSE,150.00,Campus Stationery Notebook,Books & Supplies\n";
        MockMultipartFile file = new MockMultipartFile("file", "diff_exp.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        StatementPreviewDto preview = statementImportService.parseCsvStatement(student, file);
        assertNotNull(preview);
        assertEquals(1, preview.getTotalRows());
        assertEquals(0, preview.getDuplicateRows());

        StatementRowDto row = preview.getRows().get(0);
        assertFalse(row.isDuplicate(), "Different descriptions with same amount/date must NOT be flagged as duplicate");
        assertTrue(row.isSelected());
    }

    @Test
    @DisplayName("Verify income versus expense with same amount on same date are NOT flagged as duplicates")
    void testIncomeVsExpenseWithSameAmount() {
        // 1. Existing income in database
        TransactionDto incomeTx = new TransactionDto();
        incomeTx.setType(com.financemanager.entity.TransactionType.INCOME);
        incomeTx.setAmount(new BigDecimal("500.00"));
        incomeTx.setDescription("Bank Transfer");
        incomeTx.setCategory("Allowance");
        incomeTx.setTransactionDate(java.time.LocalDate.of(2026, 10, 16));
        transactionService.createTransaction(student, incomeTx);

        // 2. CSV with expense with same description, date, and amount
        String csv = "Date,Type,Amount,Description,Category\n" +
                "2026-10-16,EXPENSE,500.00,Bank Transfer,Other\n";
        MockMultipartFile file = new MockMultipartFile("file", "expense.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        StatementPreviewDto preview = statementImportService.parseCsvStatement(student, file);
        assertNotNull(preview);
        assertEquals(1, preview.getTotalRows());
        assertEquals(0, preview.getDuplicateRows());

        StatementRowDto row = preview.getRows().get(0);
        assertFalse(row.isDuplicate(), "INCOME vs EXPENSE with same amount must NOT be flagged as duplicate");
        assertTrue(row.isSelected());
    }
}
