package com.financemanager.service;

import com.financemanager.dto.StatementPreviewDto;
import com.financemanager.dto.StatementRowDto;
import com.financemanager.dto.TransactionDto;
import com.financemanager.entity.Role;
import com.financemanager.entity.Transaction;
import com.financemanager.entity.TransactionType;
import com.financemanager.entity.User;
import com.financemanager.exception.BadRequestException;
import com.financemanager.exception.ForbiddenException;
import com.financemanager.repository.TransactionRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class StatementImportService {

    private static final Logger log = LoggerFactory.getLogger(StatementImportService.class);

    private static final Pattern DATE_PATTERN = Pattern.compile(
            "\\b(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|\\d{4}-\\d{2}-\\d{2}|\\d{1,2}[\\s/-](?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*[\\s/-]\\d{2,4})\\b",
            Pattern.CASE_INSENSITIVE
    );

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AuditService auditService;

    public StatementPreviewDto parseCsvStatement(User currentUser, MultipartFile file) {
        if (currentUser.getRole() == Role.PARENT) {
            throw new ForbiddenException("Parents have strictly read-only access and cannot import statements.");
        }

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded statement file is empty.");
        }

        List<StatementRowDto> rows = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new BadRequestException("File contains no data.");
            }

            // Detect column indices from header
            String[] headers = parseCsvLine(headerLine);
            Map<String, Integer> colMap = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                String h = headers[i].trim().toLowerCase();
                if (h.contains("date")) colMap.put("date", i);
                else if (h.contains("type")) colMap.put("type", i);
                else if (h.contains("cat")) colMap.put("category", i);
                else if (h.contains("desc") || h.contains("narration") || h.contains("merchant") || h.contains("particular")) colMap.put("desc", i);
                else if (h.contains("balance")) colMap.put("balance", i);
                else if (h.contains("debit") || h.contains("withdrawal") || h.contains("dr")) colMap.put("debit", i);
                else if (h.contains("credit") || h.contains("deposit") || h.contains("cr")) colMap.put("credit", i);
                else if (h.contains("amount")) colMap.put("amount", i);
                else if (h.contains("method") || h.contains("mode") || h.contains("channel")) colMap.put("method", i);
            }

            String line;
            int lineNum = 1;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] cols = parseCsvLine(line);
                if (cols.length == 0) continue;

                StatementRowDto row = new StatementRowDto();
                row.setRowIndex(lineNum++);

                // Date
                int dateIdx = colMap.getOrDefault("date", 0);
                String dateStr = (dateIdx < cols.length) ? cols[dateIdx].trim() : "";
                LocalDate txDate = parseFlexibleDate(dateStr);
                row.setTransactionDate(txDate);

                // Type
                int typeIdx = colMap.getOrDefault("type", 1);
                String typeStr = (typeIdx < cols.length) ? cols[typeIdx].trim().toUpperCase() : "EXPENSE";
                TransactionType type = typeStr.contains("INCOME") || typeStr.contains("CREDIT") || typeStr.contains("CR") ?
                        TransactionType.INCOME : TransactionType.EXPENSE;

                // Amount, Debit, Credit, Balance
                int debitIdx = colMap.getOrDefault("debit", -1);
                int creditIdx = colMap.getOrDefault("credit", -1);
                int balIdx = colMap.getOrDefault("balance", -1);
                int amountIdx = colMap.getOrDefault("amount", -1);

                BigDecimal debit = (debitIdx >= 0 && debitIdx < cols.length) ? parseAmount(cols[debitIdx]) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                BigDecimal credit = (creditIdx >= 0 && creditIdx < cols.length) ? parseAmount(cols[creditIdx]) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                BigDecimal bal = (balIdx >= 0 && balIdx < cols.length) ? parseAmount(cols[balIdx]) : null;

                BigDecimal amount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                if (debit.compareTo(BigDecimal.ZERO) > 0 && credit.compareTo(BigDecimal.ZERO) == 0) {
                    type = TransactionType.EXPENSE;
                    amount = debit;
                } else if (credit.compareTo(BigDecimal.ZERO) > 0 && debit.compareTo(BigDecimal.ZERO) == 0) {
                    type = TransactionType.INCOME;
                    amount = credit;
                } else if (amountIdx >= 0 && amountIdx < cols.length) {
                    amount = parseAmount(cols[amountIdx].trim());
                    if (type == TransactionType.INCOME) {
                        credit = amount;
                    } else {
                        debit = amount;
                    }
                } else {
                    amount = debit.compareTo(BigDecimal.ZERO) > 0 ? debit : credit;
                }

                row.setType(type);
                row.setAmount(amount);
                row.setDebit(debit);
                row.setCredit(credit);
                row.setBalance(bal);

                // Description
                int descIdx = colMap.getOrDefault("desc", 3);
                String desc = (descIdx < cols.length) ? cols[descIdx].trim() : "Statement Transaction";
                row.setDescription(sanitizeDescription(desc));

                // Category
                int catIdx = colMap.getOrDefault("category", -1);
                String cat = (catIdx >= 0 && catIdx < cols.length) ? cols[catIdx].trim() : "";
                if (cat.isEmpty()) {
                    cat = autoSuggestCategory(row.getDescription(), type);
                }
                row.setCategory(cat);

                // Payment Method
                int methodIdx = colMap.getOrDefault("method", -1);
                String method = (methodIdx >= 0 && methodIdx < cols.length) ? cols[methodIdx].trim() : "";
                if (method.isEmpty()) {
                    method = detectPaymentMethod(row.getDescription(), type);
                }
                row.setPaymentMethod(method);

                rows.add(row);
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to parse statement CSV file: " + e.getMessage());
        }

        // Apply unified duplicate detection against persisted records & intra-batch duplicates
        int duplicateCount = applyDuplicateDetection(currentUser, rows);

        log.info("[Statement Import] CSV statement processed. File='{}', totalRows={}, validRows={}, duplicates={}",
                sanitizeLogFileName(file.getOriginalFilename()), rows.size(), rows.size() - duplicateCount, duplicateCount);

        return new StatementPreviewDto(file.getOriginalFilename(), rows.size(), rows.size() - duplicateCount, duplicateCount, rows);
    }

    public StatementPreviewDto parsePdfStatement(User currentUser, MultipartFile file) {
        if (currentUser.getRole() == Role.PARENT) {
            throw new ForbiddenException("Parents have strictly read-only access and cannot import statements.");
        }

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded statement file is empty.");
        }

        List<StatementRowDto> rows = new ArrayList<>();

        try {
            byte[] bytes = file.getBytes();
            try (PDDocument document = Loader.loadPDF(bytes)) {
                if (document.isEncrypted()) {
                    throw new BadRequestException("The uploaded PDF is password protected. Please provide an unlocked PDF bank statement.");
                }

                if (document.getNumberOfPages() == 0) {
                    throw new BadRequestException("The uploaded PDF contains no pages.");
                }

                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                String fullText = stripper.getText(document);

                // Check for scanned / image-only PDFs that need OCR
                if (fullText == null || fullText.trim().length() < 30 || fullText.split("\\r?\\n").length < 3) {
                    throw new BadRequestException("No readable text found in this PDF. This appears to be a scanned image-only PDF that requires OCR. Please upload an original digital text-based PDF or CSV from your bank.");
                }

                rows = extractTransactionsFromPdfText(fullText);
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Failed to read or parse PDF statement: " + e.getMessage());
        }

        if (rows.isEmpty()) {
            throw new BadRequestException("No valid transaction records could be extracted from this PDF. Please verify that this is a digital text-based bank statement or upload a CSV.");
        }

        // Apply unified duplicate detection against persisted records & intra-batch duplicates
        int duplicateCount = applyDuplicateDetection(currentUser, rows);

        log.info("[Statement Import] PDF statement processed. File='{}', totalRows={}, validRows={}, duplicates={}",
                sanitizeLogFileName(file.getOriginalFilename()), rows.size(), rows.size() - duplicateCount, duplicateCount);

        return new StatementPreviewDto(file.getOriginalFilename(), rows.size(), rows.size() - duplicateCount, duplicateCount, rows);
    }

    /**
     * Unified Duplicate Detection Engine.
     * Compares normalized date, monetary amount (scale 2), transaction type (EXPENSE vs INCOME),
     * and normalized description.
     */
    public int applyDuplicateDetection(User currentUser, List<StatementRowDto> rows) {
        int duplicateCount = 0;
        Set<String> seenInBatch = new HashSet<>();

        for (StatementRowDto row : rows) {
            Optional<Transaction> existingMatch = findExistingDuplicate(currentUser, row);
            if (existingMatch.isPresent()) {
                Transaction matched = existingMatch.get();
                row.setDuplicate(true);
                row.setSelected(false);
                row.setDuplicateReason("Exact match with existing transaction #" + matched.getId() + " on " + row.getTransactionDate());
                duplicateCount++;
            } else {
                // Check intra-batch duplicate (if the file itself contains identical transactions)
                String batchKey = row.getTransactionDate() + "::" + row.getType() + "::" +
                        normalizeAmount(row.getAmount()) + "::" + normalizeDescription(row.getDescription());
                if (seenInBatch.contains(batchKey)) {
                    row.setDuplicate(true);
                    row.setSelected(false);
                    row.setDuplicateReason("Duplicate of earlier row in this uploaded statement");
                    duplicateCount++;
                } else {
                    seenInBatch.add(batchKey);
                    row.setDuplicate(false);
                    row.setSelected(true);
                }
            }
        }
        return duplicateCount;
    }

    /**
     * Finds an existing transaction in the database matching normalized date, amount, type, and description.
     */
    public Optional<Transaction> findExistingDuplicate(User currentUser, StatementRowDto row) {
        if (row == null || row.getTransactionDate() == null || row.getAmount() == null || row.getType() == null) {
            return Optional.empty();
        }

        BigDecimal normAmount = normalizeAmount(row.getAmount());
        LocalDate txDate = row.getTransactionDate();
        TransactionType type = row.getType();

        // 1. Query all transactions for this user on the same date with the same type
        List<Transaction> candidates = transactionRepository.findByOwnerAndTypeAndTransactionDate(
                currentUser, type, txDate);

        for (Transaction existing : candidates) {
            // Normalized Amount comparison (scale-independent)
            if (normalizeAmount(existing.getAmount()).compareTo(normAmount) != 0) {
                continue;
            }

            // Description comparison (normalized, trimmed whitespace, case-insensitive, punctuation-tolerant)
            if (isDescriptionDuplicateMatch(existing.getDescription(), row.getDescription())) {
                return Optional.of(existing);
            }
        }

        return Optional.empty();
    }

    public BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    public String normalizeDescription(String desc) {
        if (desc == null || desc.isBlank()) return "";
        String s = desc.trim().toLowerCase();
        // Strip common bank/UPI prefixes: e.g. "upi/", "upi-", "pos/", "neft-", "imps-", "rtgs-"
        s = s.replaceAll("^(upi[/\\-:]|pos[/\\-:]|neft[/\\-:]|rtgs[/\\-:]|imps[/\\-:])", "");
        // Replace non-alphanumeric characters with space
        s = s.replaceAll("[^a-z0-9\\s]", " ");
        // Collapse multiple spaces into single space
        s = s.replaceAll("\\s+", " ").trim();
        return s;
    }

    public boolean isDescriptionDuplicateMatch(String desc1, String desc2) {
        String n1 = normalizeDescription(desc1);
        String n2 = normalizeDescription(desc2);

        if (n1.isEmpty() && n2.isEmpty()) return true;
        if (n1.isEmpty() || n2.isEmpty()) return false;

        if (n1.equals(n2)) {
            return true;
        }

        // Substring check only if token is at least 4 chars long to avoid false positives on tiny words
        if (n1.length() >= 4 && n2.length() >= 4) {
            if (n1.contains(n2) || n2.contains(n1)) {
                return true;
            }
        }

        return false;
    }

    private List<StatementRowDto> extractTransactionsFromPdfText(String fullText) {
        List<StatementRowDto> rows = new ArrayList<>();
        String[] lines = fullText.split("\\r?\\n");
        int rowIndex = 1;
        BigDecimal lastSeenBalance = null;

        for (int i = 0; i < lines.length; i++) {
            String rawLine = lines[i].trim();
            if (rawLine.isEmpty()) continue;

            String lower = rawLine.toLowerCase();
            // Filter common header and footer noise
            if (lower.startsWith("page ") || lower.contains("opening balance") || lower.contains("closing balance") ||
                lower.contains("statement summary") || lower.contains("account summary") || lower.contains("statement of account") ||
                lower.contains("branch address") || lower.contains("registered office") || lower.contains("generated on") ||
                (lower.contains("date") && lower.contains("balance") && (lower.contains("narration") || lower.contains("particular") || lower.contains("withdrawal") || lower.contains("debit")))) {
                continue;
            }

            Matcher dateMatcher = DATE_PATTERN.matcher(rawLine);
            if (!dateMatcher.find() || dateMatcher.start() > 6) {
                // Not starting with a date. If we already have a previous row, check for multi-line narration continuation:
                if (!rows.isEmpty() && rawLine.length() < 120 && !lower.contains("total")) {
                    StatementRowDto prevRow = rows.get(rows.size() - 1);
                    String combined = prevRow.getDescription() + " " + rawLine;
                    prevRow.setDescription(sanitizeDescription(combined));
                    prevRow.setCategory(autoSuggestCategory(prevRow.getDescription(), prevRow.getType()));
                }
                continue;
            }

            String dateStr = dateMatcher.group(1);
            LocalDate txDate = parseFlexibleDate(dateStr);
            String afterDate = rawLine.substring(dateMatcher.end()).trim();

            // Check if there is an immediately following Value Date (e.g. "15/10/2026 15/10/2026 ...")
            Matcher valDateMatcher = DATE_PATTERN.matcher(afterDate);
            if (valDateMatcher.find() && valDateMatcher.start() <= 2) {
                afterDate = afterDate.substring(valDateMatcher.end()).trim();
            }

            // Find all monetary amounts at the end of the line
            List<AmountMatch> amountMatches = findAmountMatches(afterDate);
            if (amountMatches.isEmpty()) {
                continue;
            }

            // The description is the text before the first amount match
            int firstAmtStart = amountMatches.get(0).start;
            String descPart = afterDate.substring(0, firstAmtStart).trim();
            String description = sanitizeDescription(descPart);

            BigDecimal debit = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            BigDecimal credit = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            BigDecimal balance = null;
            TransactionType type = TransactionType.EXPENSE;
            BigDecimal amount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

            if (amountMatches.size() >= 3) {
                // 3 numbers: [Debit, Credit, Balance]
                AmountMatch m1 = amountMatches.get(amountMatches.size() - 3);
                AmountMatch m2 = amountMatches.get(amountMatches.size() - 2);
                AmountMatch m3 = amountMatches.get(amountMatches.size() - 1);

                BigDecimal num1 = parseAmount(m1.val);
                BigDecimal num2 = parseAmount(m2.val);
                balance = parseAmount(m3.val);

                if (num1.compareTo(BigDecimal.ZERO) > 0 && num2.compareTo(BigDecimal.ZERO) == 0) {
                    debit = num1;
                    type = TransactionType.EXPENSE;
                    amount = debit;
                } else if (num2.compareTo(BigDecimal.ZERO) > 0 && num1.compareTo(BigDecimal.ZERO) == 0) {
                    credit = num2;
                    type = TransactionType.INCOME;
                    amount = credit;
                } else if (num1.compareTo(BigDecimal.ZERO) > 0) {
                    debit = num1;
                    credit = num2;
                    type = TransactionType.EXPENSE;
                    amount = debit;
                } else {
                    credit = num2;
                    debit = num1;
                    type = TransactionType.INCOME;
                    amount = credit;
                }
            } else if (amountMatches.size() == 2) {
                // 2 numbers: [Amount, Balance]
                AmountMatch m1 = amountMatches.get(0);
                AmountMatch m2 = amountMatches.get(1);

                BigDecimal num1 = parseAmount(m1.val);
                balance = parseAmount(m2.val);

                boolean isCredit = false;
                if ("CR".equalsIgnoreCase(m1.drCr)) {
                    isCredit = true;
                } else if ("DR".equalsIgnoreCase(m1.drCr)) {
                    isCredit = false;
                } else if (m1.val.startsWith("+")) {
                    isCredit = true;
                } else if (m1.val.startsWith("-")) {
                    isCredit = false;
                } else if (lastSeenBalance != null && balance.compareTo(lastSeenBalance) > 0) {
                    isCredit = true;
                } else if (lastSeenBalance != null && balance.compareTo(lastSeenBalance) < 0) {
                    isCredit = false;
                } else {
                    isCredit = isCreditDescription(description);
                }

                if (isCredit) {
                    credit = num1;
                    type = TransactionType.INCOME;
                    amount = credit;
                } else {
                    debit = num1;
                    type = TransactionType.EXPENSE;
                    amount = debit;
                }
            } else {
                // 1 number: [Amount]
                AmountMatch m1 = amountMatches.get(0);
                BigDecimal num1 = parseAmount(m1.val);

                boolean isCredit = "CR".equalsIgnoreCase(m1.drCr) || m1.val.startsWith("+") || isCreditDescription(description);
                if (isCredit) {
                    credit = num1;
                    type = TransactionType.INCOME;
                    amount = credit;
                } else {
                    debit = num1;
                    type = TransactionType.EXPENSE;
                    amount = debit;
                }
            }

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            if (balance != null) {
                lastSeenBalance = balance;
            }

            StatementRowDto row = new StatementRowDto();
            row.setRowIndex(rowIndex++);
            row.setTransactionDate(txDate);
            row.setType(type);
            row.setAmount(amount);
            row.setDebit(debit);
            row.setCredit(credit);
            row.setBalance(balance);
            row.setDescription(description);
            row.setCategory(autoSuggestCategory(description, type));
            row.setPaymentMethod(detectPaymentMethod(description, type));
            row.setSelected(true);

            rows.add(row);
        }

        return rows;
    }

    private static class AmountMatch {
        int start;
        int end;
        String val;
        String drCr;

        AmountMatch(int start, int end, String val, String drCr) {
            this.start = start;
            this.end = end;
            this.val = val;
            this.drCr = drCr;
        }
    }

    private List<AmountMatch> findAmountMatches(String text) {
        List<AmountMatch> list = new ArrayList<>();
        Pattern p = Pattern.compile("([+-]?(?:\\d{1,3}(?:,\\d{3})+|\\d+)(?:\\.\\d{1,2})?)(?:\\s*([Dd][Rr]|[Cc][Rr]))?\\b");
        Matcher m = p.matcher(text);
        while (m.find()) {
            String val = m.group(1);
            if (!val.contains(".") && !val.contains(",") && val.length() < 3) {
                continue;
            }
            list.add(new AmountMatch(m.start(), m.end(), val, m.group(2)));
        }
        return list;
    }

    private boolean isCreditDescription(String desc) {
        if (desc == null) return false;
        String d = desc.toLowerCase();
        return d.contains("credit") || d.contains("salary") || d.contains("stipend") ||
               d.contains("refund") || d.contains("reversal") || d.contains("deposit") ||
               d.contains("interest") || d.contains("cashback") || d.contains("dividend") ||
               d.contains("by transfer") || d.contains("inward");
    }

    private String sanitizeDescription(String desc) {
        if (desc == null || desc.isBlank()) return "Bank Transaction";
        String s = desc.trim();
        // Mask 10-18 digit account or card numbers to prevent storing/logging sensitive bank details
        s = s.replaceAll("\\b(\\d{2})\\d{6,12}(\\d{4})\\b", "$1******$2");
        s = s.replaceAll("\\s{2,}", " ");
        return s.isEmpty() ? "Bank Transaction" : s;
    }

    private String sanitizeLogFileName(String name) {
        if (name == null) return "unknown";
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    @Transactional
    public synchronized Map<String, Object> commitImport(User currentUser, List<StatementRowDto> rowsToImport) {
        if (currentUser.getRole() == Role.PARENT) {
            throw new ForbiddenException("Parents have strictly read-only access and cannot import statements.");
        }

        int importedCount = 0;
        int skippedCount = 0;

        for (StatementRowDto row : rowsToImport) {
            if (!row.isSelected()) {
                skippedCount++;
                continue;
            }

            // Compare again before saving to prevent double-click or concurrent duplicate creation
            Optional<Transaction> existingMatch = findExistingDuplicate(currentUser, row);
            if (existingMatch.isPresent()) {
                log.info("[Statement Import Commit] Skipping duplicate on save. Date={}, Amount={}, MatchedTxId={}",
                        row.getTransactionDate(), normalizeAmount(row.getAmount()), existingMatch.get().getId());
                skippedCount++;
                continue;
            }

            TransactionDto dto = new TransactionDto();
            dto.setType(row.getType());
            dto.setAmount(normalizeAmount(row.getAmount()));
            dto.setCategory(row.getCategory());
            dto.setDescription(row.getDescription().trim());
            dto.setTransactionDate(row.getTransactionDate());
            dto.setPaymentMethod(row.getPaymentMethod());
            dto.setSource("IMPORT");

            transactionService.createTransaction(currentUser, dto);
            importedCount++;
        }

        auditService.log(currentUser, "STATEMENT_IMPORTED", "TRANSACTION", null,
                "Imported: " + importedCount + ", Skipped: " + skippedCount);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("importedCount", importedCount);
        response.put("skippedCount", skippedCount);
        response.put("message", "Successfully imported " + importedCount + " transactions.");
        return response;
    }

    private LocalDate parseFlexibleDate(String str) {
        if (str == null || str.isBlank()) return LocalDate.now();
        str = str.replace("/", "-").trim();
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                DateTimeFormatter.ofPattern("MM-dd-yyyy"),
                DateTimeFormatter.ofPattern("d-M-yyyy"),
                DateTimeFormatter.ofPattern("yyyy-M-d"),
                DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("d-MMM-yyyy", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("dd-MM-yy"),
                DateTimeFormatter.ofPattern("d-M-yy")
        );

        for (DateTimeFormatter dtf : formatters) {
            try {
                return LocalDate.parse(str, dtf);
            } catch (Exception ignored) {}
        }
        return LocalDate.now();
    }

    public BigDecimal parseAmount(String str) {
        if (str == null) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        String cleaned = str.replaceAll("[^0-9.]", "");
        if (cleaned.isEmpty()) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        try {
            return new BigDecimal(cleaned).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
    }

    private String detectPaymentMethod(String desc, TransactionType type) {
        if (desc == null) return "Bank Import";
        String d = desc.toLowerCase();
        if (d.contains("upi")) return "UPI";
        if (d.contains("atm") || d.contains("cash")) return "ATM / Cash";
        if (d.contains("pos") || d.contains("card") || d.contains("visa") || d.contains("mastercard")) return "Debit Card";
        if (d.contains("neft") || d.contains("rtgs") || d.contains("imps") || d.contains("transfer") || d.contains("netbanking")) return "Net Banking";
        if (type == TransactionType.INCOME) return "Bank Transfer";
        return "Bank Import";
    }

    private String autoSuggestCategory(String desc, TransactionType type) {
        if (type == TransactionType.INCOME) {
            return "Allowance";
        }
        String d = desc.toLowerCase();
        if (d.contains("mess") || d.contains("canteen") || d.contains("food") || d.contains("swiggy") || d.contains("zomato") || d.contains("cafe")) {
            return "Food & Mess";
        } else if (d.contains("hostel") || d.contains("rent") || d.contains("room") || d.contains("pg")) {
            return "Hostel & Rent";
        } else if (d.contains("book") || d.contains("stationery") || d.contains("xerox") || d.contains("print")) {
            return "Books & Supplies";
        } else if (d.contains("bus") || d.contains("metro") || d.contains("uber") || d.contains("ola") || d.contains("auto") || d.contains("fuel")) {
            return "Transport & Commute";
        } else if (d.contains("spotify") || d.contains("netflix") || d.contains("chatgpt") || d.contains("github") || d.contains("prime")) {
            return "Subscriptions & Tools";
        } else if (d.contains("tuition") || d.contains("exam") || d.contains("college") || d.contains("university") || d.contains("lab")) {
            return "College Fees & Tuition";
        }
        return "Personal & Entertainment";
    }

    private String[] parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString().trim());
        return tokens.toArray(new String[0]);
    }
}
