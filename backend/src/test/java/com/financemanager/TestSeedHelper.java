package com.financemanager;

import com.financemanager.entity.*;
import com.financemanager.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
public class TestSeedHelper {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FamilyLinkRepository familyLinkRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private SavingsGoalRepository savingsGoalRepository;

    @Autowired
    private InvestmentRepository investmentRepository;

    @Autowired
    private SipRecordRepository sipRecordRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void ensureTestData() {
        User student = userRepository.findByEmail("student@university.edu").orElseGet(() -> {
            User u = new User(
                    "student@university.edu",
                    passwordEncoder.encode("Password123!"),
                    "Alex Chen (Student)",
                    Role.STUDENT,
                    "STU-ALEX-8842"
            );
            return userRepository.save(u);
        });

        User parent = userRepository.findByEmail("parent@family.com").orElseGet(() -> {
            User u = new User(
                    "parent@family.com",
                    passwordEncoder.encode("Password123!"),
                    "Sarah Chen (Parent)",
                    Role.PARENT,
                    null
            );
            return userRepository.save(u);
        });

        if (familyLinkRepository.findByParentAndStudent(parent, student).isEmpty()) {
            FamilyLink link = new FamilyLink(parent, student, LinkStatus.ACTIVE, "PARENT");
            link.setApprovedAt(LocalDateTime.now());
            familyLinkRepository.save(link);
        }

        if (transactionRepository.findByOwnerOrderByTransactionDateDescCreatedAtDesc(student).isEmpty()) {
            transactionRepository.save(new Transaction(student, TransactionType.INCOME, new BigDecimal("15000.00"),
                    "Allowance", "Monthly Family Allowance", LocalDate.of(2026, 10, 1), "Bank Transfer", "MANUAL"));
            transactionRepository.save(new Transaction(student, TransactionType.INCOME, new BigDecimal("5000.00"),
                    "Stipend", "Research Lab Assistant Stipend", LocalDate.of(2026, 10, 5), "Direct Deposit", "MANUAL"));
            transactionRepository.save(new Transaction(student, TransactionType.EXPENSE, new BigDecimal("6500.00"),
                    "Hostel & Rent", "University Hostel Room Rent", LocalDate.of(2026, 10, 2), "Net Banking", "MANUAL"));
            transactionRepository.save(new Transaction(student, TransactionType.EXPENSE, new BigDecimal("3200.00"),
                    "Food & Mess", "Campus Monthly Mess Dues", LocalDate.of(2026, 10, 3), "UPI", "MANUAL"));
            transactionRepository.save(new Transaction(student, TransactionType.EXPENSE, new BigDecimal("1200.00"),
                    "Books & Supplies", "Data Structures & Algorithms Textbook", LocalDate.of(2026, 10, 6), "Card", "MANUAL"));
            transactionRepository.save(new Transaction(student, TransactionType.EXPENSE, new BigDecimal("450.00"),
                    "Transport", "Monthly City Bus Student Pass", LocalDate.of(2026, 10, 7), "Cash", "MANUAL"));
            transactionRepository.save(new Transaction(student, TransactionType.EXPENSE, new BigDecimal("199.00"),
                    "Subscriptions", "Music & Code Editor Subscription", LocalDate.of(2026, 10, 8), "UPI", "MANUAL"));
            transactionRepository.save(new Transaction(student, TransactionType.EXPENSE, new BigDecimal("45.00"),
                    "Food & Mess", "Campus Canteen Tea & Biscuit", LocalDate.of(2026, 10, 9), "UPI", "MANUAL"));
            transactionRepository.save(new Transaction(student, TransactionType.EXPENSE, new BigDecimal("55.00"),
                    "Food & Mess", "Campus Canteen Evening Snack", LocalDate.of(2026, 10, 10), "UPI", "MANUAL"));
            transactionRepository.save(new Transaction(student, TransactionType.EXPENSE, new BigDecimal("50.00"),
                    "Food & Mess", "Campus Canteen Tea & Biscuit", LocalDate.of(2026, 10, 12), "UPI", "MANUAL"));
            transactionRepository.save(new Transaction(student, TransactionType.EXPENSE, new BigDecimal("45.00"),
                    "Food & Mess", "Campus Canteen Tea & Biscuit", LocalDate.of(2026, 10, 13), "UPI", "MANUAL"));
            transactionRepository.save(new Transaction(student, TransactionType.EXPENSE, new BigDecimal("850.00"),
                    "Personal", "Weekend Movie & Dinner", LocalDate.of(2026, 10, 14), "UPI", "MANUAL"));
        }

        if (budgetRepository.findByStudentOrderByPeriodMonthDesc(student).isEmpty()) {
            budgetRepository.save(new Budget(student, "Food & Mess", "2026-10", new BigDecimal("4500.00"), new BigDecimal("80.00")));
            budgetRepository.save(new Budget(student, "Books & Supplies", "2026-10", new BigDecimal("2000.00"), new BigDecimal("80.00")));
            budgetRepository.save(new Budget(student, "Personal", "2026-10", new BigDecimal("1500.00"), new BigDecimal("80.00")));
        }

        if (savingsGoalRepository.findByStudentOrderByCreatedAtDesc(student).isEmpty()) {
            savingsGoalRepository.save(new SavingsGoal(student, "Semester Emergency Fund",
                    new BigDecimal("20000.00"), new BigDecimal("12500.00"), LocalDate.of(2026, 12, 31)));
            savingsGoalRepository.save(new SavingsGoal(student, "New Coding Laptop Fund",
                    new BigDecimal("60000.00"), new BigDecimal("18000.00"), LocalDate.of(2027, 6, 30)));
        }

        if (investmentRepository.findByStudentOrderByCreatedAtDesc(student).isEmpty()) {
            investmentRepository.save(new Investment(student, "MUTUAL_FUNDS", "Nifty 50 Index Fund Direct Growth",
                    new BigDecimal("15000.00"), new BigDecimal("16850.00")));
            investmentRepository.save(new Investment(student, "STOCKS", "Clean Energy Tech ETF",
                    new BigDecimal("5000.00"), new BigDecimal("5420.00")));
        }

        if (sipRecordRepository.findByStudentOrderByCreatedAtDesc(student).isEmpty()) {
            sipRecordRepository.save(new SipRecord(student, "Flexi-Cap Long Term Growth SIP",
                    new BigDecimal("1500.00"), LocalDate.of(2026, 1, 5), 5, new BigDecimal("12.50"), 36));
        }
    }
}
