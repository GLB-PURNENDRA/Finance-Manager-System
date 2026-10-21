package com.financemanager.service;

import com.financemanager.entity.*;
import com.financemanager.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FinancialContextService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private SavingsGoalRepository savingsGoalRepository;

    @Autowired
    private SipRecordRepository sipRecordRepository;

    @Autowired
    private FamilyLinkService familyLinkService;

    @Autowired
    private InvestorProfileRepository investorProfileRepository;

    @Autowired
    private AmfiNavService amfiNavService;

    public static class FinancialContext {
        public User student;
        public Role viewerRole;
        public YearMonth currentYm;
        public YearMonth previousYm;
        public BigDecimal currentMonthIncome;
        public BigDecimal currentMonthExpense;
        public BigDecimal currentMonthNet;
        public BigDecimal previousMonthIncome;
        public BigDecimal previousMonthExpense;
        public BigDecimal previousMonthNet;
        public Map<String, BigDecimal> currentCategoryTotals = new LinkedHashMap<>();
        public Map<String, BigDecimal> previousCategoryTotals = new LinkedHashMap<>();
        public String topCategory;
        public BigDecimal topCategoryAmount;
        public List<Budget> budgets = new ArrayList<>();
        public List<SavingsGoal> goals = new ArrayList<>();
        public List<SipRecord> sips = new ArrayList<>();
        public List<Transaction> recentTransactions = new ArrayList<>();
        public List<String> evidenceLines = new ArrayList<>();
        public String contextPromptText;
    }

    public FinancialContext buildContext(User currentUser) {
        User student = (currentUser.getRole() == Role.PARENT)
                ? familyLinkService.getActiveStudentForParent(currentUser)
                : currentUser;

        FinancialContext ctx = new FinancialContext();
        ctx.student = student;
        ctx.viewerRole = currentUser.getRole();

        YearMonth curYm = YearMonth.now();
        YearMonth prevYm = curYm.minusMonths(1);
        ctx.currentYm = curYm;
        ctx.previousYm = prevYm;

        // 1. Current Month Totals
        LocalDate curStart = curYm.atDay(1);
        LocalDate curEnd = curYm.atEndOfMonth();

        BigDecimal curInc = transactionRepository.sumAmountByOwnerAndTypeAndDateRange(student, TransactionType.INCOME, curStart, curEnd);
        ctx.currentMonthIncome = curInc != null ? curInc : BigDecimal.ZERO;

        BigDecimal curExp = transactionRepository.sumAmountByOwnerAndTypeAndDateRange(student, TransactionType.EXPENSE, curStart, curEnd);
        ctx.currentMonthExpense = curExp != null ? curExp : BigDecimal.ZERO;

        ctx.currentMonthNet = ctx.currentMonthIncome.subtract(ctx.currentMonthExpense);

        // 2. Previous Month Totals
        LocalDate prevStart = prevYm.atDay(1);
        LocalDate prevEnd = prevYm.atEndOfMonth();

        BigDecimal prevInc = transactionRepository.sumAmountByOwnerAndTypeAndDateRange(student, TransactionType.INCOME, prevStart, prevEnd);
        ctx.previousMonthIncome = prevInc != null ? prevInc : BigDecimal.ZERO;

        BigDecimal prevExp = transactionRepository.sumAmountByOwnerAndTypeAndDateRange(student, TransactionType.EXPENSE, prevStart, prevEnd);
        ctx.previousMonthExpense = prevExp != null ? prevExp : BigDecimal.ZERO;

        ctx.previousMonthNet = ctx.previousMonthIncome.subtract(ctx.previousMonthExpense);

        // 3. Category Breakdowns
        List<Transaction> curExpenses = transactionRepository.findFiltered(student, curStart, curEnd, null, TransactionType.EXPENSE);
        for (Transaction t : curExpenses) {
            ctx.currentCategoryTotals.merge(t.getCategory(), t.getAmount(), BigDecimal::add);
        }

        List<Transaction> prevExpenses = transactionRepository.findFiltered(student, prevStart, prevEnd, null, TransactionType.EXPENSE);
        for (Transaction t : prevExpenses) {
            ctx.previousCategoryTotals.merge(t.getCategory(), t.getAmount(), BigDecimal::add);
        }

        // Top spending category
        String topCat = "None";
        BigDecimal topAmt = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> e : ctx.currentCategoryTotals.entrySet()) {
            if (e.getValue().compareTo(topAmt) > 0) {
                topAmt = e.getValue();
                topCat = e.getKey();
            }
        }
        ctx.topCategory = topCat;
        ctx.topCategoryAmount = topAmt;

        // 4. Budgets
        ctx.budgets = budgetRepository.findByStudentAndPeriodMonth(student, curYm.toString());

        // 5. Goals
        ctx.goals = savingsGoalRepository.findByStudentOrderByCreatedAtDesc(student);

        // 6. SIPs (Only included for Student view or authorized parent)
        ctx.sips = sipRecordRepository.findByStudentOrderByCreatedAtDesc(student);

        // 7. Recent Transactions
        List<Transaction> allTx = transactionRepository.findByOwnerOrderByTransactionDateDescCreatedAtDesc(student);
        ctx.recentTransactions = allTx.stream().limit(8).collect(Collectors.toList());

        // 8. Build Evidence Lines
        ctx.evidenceLines.add(String.format("Current Period (%s): Recorded Expenses ₹%s (Across %d transactions)",
                curYm, ctx.currentMonthExpense, curExpenses.size()));
        if (currentUser.getRole() == Role.STUDENT) {
            ctx.evidenceLines.add(String.format("Current Period (%s): Income ₹%s | Net Balance ₹%s",
                    curYm, ctx.currentMonthIncome, ctx.currentMonthNet));
        }
        ctx.evidenceLines.add(String.format("Previous Period (%s): Recorded Expenses ₹%s",
                prevYm, ctx.previousMonthExpense));
        ctx.evidenceLines.add(String.format("Highest Spending Category (%s): '%s' (₹%s)",
                curYm, ctx.topCategory, ctx.topCategoryAmount));

        // 9. Structured Prompt Text for AI Context
        StringBuilder sb = new StringBuilder();
        sb.append("=== VERIFIED FINANCIAL LEDGER DATA (STUDENT: ").append(student.getDisplayName()).append(") ===\n");
        sb.append("Viewer Role: ").append(currentUser.getRole()).append(" (");
        if (currentUser.getRole() == Role.PARENT) {
            sb.append("Parent Read-Only Mode - Only authorized student expenses and summaries are accessible)\n");
        } else {
            sb.append("Student Owner Mode - Full access)\n");
        }

        sb.append(String.format("\n[1. CURRENT MONTH (%s)]\n", curYm));
        if (currentUser.getRole() == Role.STUDENT) {
            sb.append(String.format("- Total Income: ₹%s\n", ctx.currentMonthIncome));
            sb.append(String.format("- Total Expenses: ₹%s\n", ctx.currentMonthExpense));
            sb.append(String.format("- Net Cash Flow: ₹%s\n", ctx.currentMonthNet));
        } else {
            sb.append(String.format("- Total Permitted Expenses: ₹%s\n", ctx.currentMonthExpense));
        }

        sb.append(String.format("\n[2. PREVIOUS MONTH (%s)]\n", prevYm));
        if (currentUser.getRole() == Role.STUDENT) {
            sb.append(String.format("- Total Income: ₹%s\n", ctx.previousMonthIncome));
            sb.append(String.format("- Total Expenses: ₹%s\n", ctx.previousMonthExpense));
            sb.append(String.format("- Net Cash Flow: ₹%s\n", ctx.previousMonthNet));
        } else {
            sb.append(String.format("- Total Permitted Expenses: ₹%s\n", ctx.previousMonthExpense));
        }

        BigDecimal expenseDelta = ctx.currentMonthExpense.subtract(ctx.previousMonthExpense);
        sb.append(String.format("- Expense Month-over-Month Change: %s₹%s\n",
                expenseDelta.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "", expenseDelta));

        sb.append("\n[3. CATEGORY SPENDING THIS MONTH]\n");
        if (ctx.currentCategoryTotals.isEmpty()) {
            sb.append("- No category transactions recorded yet this month.\n");
        } else {
            for (Map.Entry<String, BigDecimal> e : ctx.currentCategoryTotals.entrySet()) {
                BigDecimal prevAmt = ctx.previousCategoryTotals.getOrDefault(e.getKey(), BigDecimal.ZERO);
                BigDecimal diff = e.getValue().subtract(prevAmt);
                sb.append(String.format("- %s: ₹%s (Last month: ₹%s, Difference: %s₹%s)\n",
                        e.getKey(), e.getValue(), prevAmt, diff.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "", diff));
            }
        }

        sb.append("\n[4. ACTIVE BUDGETS & STATUS]\n");
        if (ctx.budgets.isEmpty()) {
            sb.append("- No monthly budgets configured for this month.\n");
        } else {
            for (Budget b : ctx.budgets) {
                BigDecimal spent = ctx.currentCategoryTotals.getOrDefault(b.getCategory(), BigDecimal.ZERO);
                double pct = b.getLimitAmount().compareTo(BigDecimal.ZERO) > 0
                        ? spent.divide(b.getLimitAmount(), 4, RoundingMode.HALF_UP).doubleValue() * 100
                        : 0.0;
                String status = pct >= 100.0 ? "EXCEEDED" : (pct >= b.getWarningThreshold().doubleValue() ? "WARNING" : "HEALTHY");
                sb.append(String.format("- %s: Limit ₹%s | Spent ₹%s (%.1f%%) | Status: %s\n",
                        b.getCategory(), b.getLimitAmount(), spent, pct, status));
            }
        }

        sb.append("\n[5. SAVINGS GOALS]\n");
        if (ctx.goals.isEmpty()) {
            sb.append("- No savings goals configured.\n");
        } else {
            for (SavingsGoal g : ctx.goals) {
                double pct = g.getTargetAmount().compareTo(BigDecimal.ZERO) > 0
                        ? g.getSavedAmount().divide(g.getTargetAmount(), 4, RoundingMode.HALF_UP).doubleValue() * 100
                        : 0.0;
                sb.append(String.format("- %s: Target ₹%s | Saved ₹%s (%.1f%%) | Deadline: %s | Status: %s\n",
                        g.getName(), g.getTargetAmount(), g.getSavedAmount(), pct,
                        g.getTargetDate() != null ? g.getTargetDate().toString() : "Open", g.getStatus()));
            }
        }

        if (currentUser.getRole() == Role.STUDENT) {
            sb.append("\n[6. ACTIVE SYSTEMATIC INVESTMENT PLANS (SIP)]\n");
            if (ctx.sips.isEmpty()) {
                sb.append("- No SIP investments registered.\n");
            } else {
                for (SipRecord s : ctx.sips) {
                    sb.append(String.format("- %s: ₹%s/month | Expected Return: %s%% | Status: %s\n",
                            s.getFundName(), s.getMonthlyAmount(), s.getAssumedReturnRate(), s.getPaymentStatus()));
                }
            }
        }

        sb.append("\n[7. RECENT TRANSACTIONS]\n");
        if (ctx.recentTransactions.isEmpty()) {
            sb.append("- No transactions recorded.\n");
        } else {
            for (Transaction tx : ctx.recentTransactions) {
                sb.append(String.format("- %s | %s | ₹%s | Category: %s | '%s'\n",
                        tx.getTransactionDate(), tx.getType(), tx.getAmount(), tx.getCategory(), tx.getDescription()));
            }
        }

        sb.append("\n[8. INVESTOR PROFILE & RISK ASSESSMENT]\n");
        Optional<InvestorProfile> profileOpt = investorProfileRepository.findByStudent(student);
        if (profileOpt.isPresent()) {
            InvestorProfile p = profileOpt.get();
            sb.append(String.format("- Risk Tolerance: %s\n", p.getRiskTolerance()));
            sb.append(String.format("- Investment Horizon: %s\n", p.getInvestmentHorizon()));
            sb.append(String.format("- Primary Goal: %s\n", p.getPrimaryGoal()));
            sb.append(String.format("- Confirmed Emergency Fund: ₹%s (Target: %d months)\n",
                    p.getExistingEmergencyFund() != null ? p.getExistingEmergencyFund() : BigDecimal.ZERO,
                    p.getEmergencyFundTargetMonths()));
            sb.append(String.format("- Confirmed Debt/EMI Obligations: ₹%s/month\n",
                    p.getMonthlyDebtObligations() != null ? p.getMonthlyDebtObligations() : BigDecimal.ZERO));
            sb.append(String.format("- Questionnaire Completed: %s\n", p.getQuestionnaireCompleted()));
        } else {
            sb.append("- Investor questionnaire has not been completed yet (Defaults: Moderate risk, 3-5Y horizon, wealth creation).\n");
        }

        sb.append("\n[9. VERIFIED REAL AMFI MUTUAL FUND BENCHMARKS (Daily EOD NAV Feed)]\n");
        sb.append(String.format("- Data Source: Association of Mutual Funds in India (AMFI) Official Feed (Last Refresh: %s, Stale: %s)\n",
                amfiNavService.getLastSuccessfulRefresh(), amfiNavService.isStale()));
        for (var s : amfiNavService.getAllSchemes().stream().limit(6).toList()) {
            sb.append(String.format("- Code %s: %s | Category: %s | NAV: ₹%s (Date: %s) | 1Y: %s%% | 3Y: %s%% | 3Y Volatility: %s%% | Exp: %s%% | Risk: %s\n",
                    s.getSchemeCode(), s.getSchemeName(), s.getCategory(), s.getNav(), s.getNavDate(),
                    s.getReturn1Y(), s.getReturn3Y(), s.getVolatility3Y(), s.getExpenseRatioEstimated(), s.getRiskometer()));
        }

        ctx.contextPromptText = sb.toString();
        return ctx;
    }
}
