package com.financemanager.service;

import com.financemanager.dto.*;
import com.financemanager.entity.*;
import com.financemanager.exception.BadRequestException;
import com.financemanager.exception.ForbiddenException;
import com.financemanager.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class IntelligenceService {

    private static final Logger log = LoggerFactory.getLogger(IntelligenceService.class);

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
    private BudgetService budgetService;

    @Autowired
    private SavingsGoalService savingsGoalService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private InvestmentAffordabilityService affordabilityService;

    @Autowired
    private InvestmentSuitabilityService suitabilityService;

    @Autowired
    private AmfiNavService amfiNavService;

    @Autowired
    private FinancialContextService financialContextService;

    @Autowired
    private GeminiClientService geminiClientService;

    @Autowired
    private AiConversationMessageRepository aiConversationMessageRepository;

    // Standard Student Finance Categories
    public static final List<String> STUDENT_CATEGORIES = List.of(
            "Food & Mess",
            "Hostel & Rent",
            "Books & Supplies",
            "College Fees & Tuition",
            "Transport & Commute",
            "Subscriptions & Tools",
            "Personal & Entertainment",
            "Healthcare & Emergency",
            "Other"
    );

    private User resolveStudent(User currentUser) {
        if (currentUser.getRole() == Role.PARENT) {
            return familyLinkService.getActiveStudentForParent(currentUser);
        }
        return currentUser;
    }

    // 1. Money Leak Detector
    public List<IntelligenceReportDto.MoneyLeakItem> detectMoneyLeaks(User currentUser) {
        User student = resolveStudent(currentUser);
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);

        List<Transaction> expenses = transactionRepository.findFiltered(
                student, thirtyDaysAgo, LocalDate.now(), null, TransactionType.EXPENSE);

        Map<String, List<Transaction>> grouped = new HashMap<>();
        for (Transaction tx : expenses) {
            if (tx.getAmount().compareTo(new BigDecimal("300")) <= 0) {
                String key = tx.getCategory() + "::" + tx.getDescription().trim().toLowerCase();
                grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(tx);
            }
        }

        List<IntelligenceReportDto.MoneyLeakItem> leaks = new ArrayList<>();
        for (Map.Entry<String, List<Transaction>> entry : grouped.entrySet()) {
            List<Transaction> list = entry.getValue();
            if (list.size() >= 2) {
                String[] parts = entry.getKey().split("::");
                String cat = parts[0];
                String pattern = parts[1];

                BigDecimal sum = list.stream().map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                List<String> details = list.stream()
                        .map(t -> t.getTransactionDate() + ": ₹" + t.getAmount() + " (" + t.getDescription() + ")")
                        .collect(Collectors.toList());

                String explanation = String.format(
                        "Found %d micro-transactions for '%s' totaling ₹%s over the past 30 days. Recurring small purchases often accumulate unnoticed.",
                        list.size(), pattern, sum);

                leaks.add(new IntelligenceReportDto.MoneyLeakItem(cat, pattern, list.size(), sum, explanation, details));
            }
        }

        return leaks;
    }

    // 2. What-If Simulator
    public WhatIfResponseDto simulateWhatIf(User currentUser, WhatIfRequestDto request) {
        User student = resolveStudent(currentUser);

        YearMonth ym = YearMonth.now();
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        BigDecimal baseIncome = transactionRepository.sumAmountByOwnerAndTypeAndDateRange(student, TransactionType.INCOME, start, end);
        if (baseIncome == null) baseIncome = BigDecimal.ZERO;

        BigDecimal baseExpense = transactionRepository.sumAmountByOwnerAndTypeAndDateRange(student, TransactionType.EXPENSE, start, end);
        if (baseExpense == null) baseExpense = BigDecimal.ZERO;

        BigDecimal baseNet = baseIncome.subtract(baseExpense);

        BigDecimal incomeAdj = request.getMonthlyIncomeAdjustment() != null ? request.getMonthlyIncomeAdjustment() : BigDecimal.ZERO;
        BigDecimal sipAdj = request.getMonthlySipAdjustment() != null ? request.getMonthlySipAdjustment() : BigDecimal.ZERO;

        BigDecimal simulatedIncome = baseIncome.add(incomeAdj);

        BigDecimal expenseReduction = BigDecimal.ZERO;
        Map<String, BigDecimal> categoryImpacts = new HashMap<>();

        if (request.getCategoryExpenseAdjustments() != null) {
            for (Map.Entry<String, BigDecimal> entry : request.getCategoryExpenseAdjustments().entrySet()) {
                categoryImpacts.put(entry.getKey(), entry.getValue());
                expenseReduction = expenseReduction.add(entry.getValue());
            }
        }

        BigDecimal simulatedExpense = baseExpense.subtract(expenseReduction);
        if (simulatedExpense.compareTo(BigDecimal.ZERO) < 0) {
            simulatedExpense = BigDecimal.ZERO;
        }

        BigDecimal simulatedNet = simulatedIncome.subtract(simulatedExpense).subtract(sipAdj);
        BigDecimal monthlySavingsGain = simulatedNet.subtract(baseNet);
        BigDecimal projectedAnnualSavings = monthlySavingsGain.multiply(new BigDecimal("12"));

        WhatIfResponseDto response = new WhatIfResponseDto();
        response.setBaselineIncome(baseIncome);
        response.setBaselineExpense(baseExpense);
        response.setBaselineNetCashFlow(baseNet);
        response.setSimulatedIncome(simulatedIncome);
        response.setSimulatedExpense(simulatedExpense);
        response.setSimulatedNetCashFlow(simulatedNet);
        response.setProjectedAnnualSavingsImpact(projectedAnnualSavings);
        response.setCategoryImpacts(categoryImpacts);

        String expl = String.format(
                "Adjusting monthly spending by -₹%s and monthly income by +₹%s improves monthly cash flow by ₹%s, projecting an extra ₹%s in annual savings capacity.",
                expenseReduction, incomeAdj, monthlySavingsGain, projectedAnnualSavings);
        response.setExplanation(expl);

        return response;
    }

    // 6. Financial Anomaly Radar
    public List<IntelligenceReportDto.AnomalyItem> detectAnomalies(User currentUser) {
        User student = resolveStudent(currentUser);
        List<Transaction> transactions = transactionRepository.findByOwnerOrderByTransactionDateDescCreatedAtDesc(student);

        List<IntelligenceReportDto.AnomalyItem> anomalies = new ArrayList<>();

        Map<String, List<BigDecimal>> categoryAmounts = new HashMap<>();
        for (Transaction tx : transactions) {
            if (tx.getType() == TransactionType.EXPENSE) {
                categoryAmounts.computeIfAbsent(tx.getCategory(), k -> new ArrayList<>()).add(tx.getAmount());
            }
        }

        for (Transaction tx : transactions) {
            if (tx.getType() == TransactionType.EXPENSE) {
                List<BigDecimal> list = categoryAmounts.get(tx.getCategory());
                if (list != null && list.size() >= 3) {
                    double avg = list.stream().mapToDouble(BigDecimal::doubleValue).average().orElse(0.0);
                    if (avg > 0 && tx.getAmount().doubleValue() > avg * 2.5 && tx.getAmount().compareTo(new BigDecimal("1000")) > 0) {
                        anomalies.add(new IntelligenceReportDto.AnomalyItem(
                                "OUTLIER",
                                "Unusually High Transaction: " + tx.getDescription(),
                                String.format("Transaction of ₹%s is 2.5x higher than the typical average of ₹%.2f in %s.",
                                        tx.getAmount(), avg, tx.getCategory()),
                                tx.getAmount(),
                                tx.getCategory(),
                                tx.getTransactionDate().toString()
                        ));
                    }
                }
            }
        }

        for (int i = 0; i < transactions.size(); i++) {
            Transaction a = transactions.get(i);
            for (int j = i + 1; j < transactions.size(); j++) {
                Transaction b = transactions.get(j);
                if (a.getType() == b.getType() &&
                        a.getAmount().compareTo(b.getAmount()) == 0 &&
                        a.getDescription().equalsIgnoreCase(b.getDescription())) {
                    long daysDiff = Math.abs(ChronoUnit.DAYS.between(a.getTransactionDate(), b.getTransactionDate()));
                    if (daysDiff <= 2) {
                        anomalies.add(new IntelligenceReportDto.AnomalyItem(
                                "SUSPECTED_DUPLICATE",
                                "Potential Duplicate: " + a.getDescription(),
                                String.format("Found duplicate amount ₹%s for '%s' recorded on %s and %s (%d day(s) apart).",
                                        a.getAmount(), a.getDescription(), a.getTransactionDate(), b.getTransactionDate(), daysDiff),
                                a.getAmount(),
                                a.getCategory(),
                                a.getTransactionDate().toString()
                        ));
                        break;
                    }
                }
            }
        }

        return anomalies;
    }

    // 8. Goal-Based Money Coach
    public List<IntelligenceReportDto.GoalCoachItem> getGoalCoachSuggestions(User currentUser) {
        User student = resolveStudent(currentUser);
        List<SavingsGoal> goals = savingsGoalRepository.findByStudentOrderByCreatedAtDesc(student);

        List<IntelligenceReportDto.GoalCoachItem> suggestions = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (SavingsGoal goal : goals) {
            if ("COMPLETED".equalsIgnoreCase(goal.getStatus())) continue;

            BigDecimal remaining = goal.getTargetAmount().subtract(goal.getSavedAmount());
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) continue;

            long months = 1;
            if (goal.getTargetDate() != null && goal.getTargetDate().isAfter(today)) {
                months = Math.max(1, ChronoUnit.MONTHS.between(today, goal.getTargetDate()));
            }

            BigDecimal recMonthly = remaining.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
            String reasoning = String.format(
                    "To reach ₹%s by %s (%d month(s) remaining), save approximately ₹%s each month.",
                    goal.getTargetAmount(),
                    goal.getTargetDate() != null ? goal.getTargetDate().toString() : "target date",
                    months, recMonthly);

            String tradeOff = "Tip: Trimming ₹500 from Personal Spending and ₹500 from Subscriptions helps cover this allocation comfortably.";

            suggestions.add(new IntelligenceReportDto.GoalCoachItem(
                    goal.getName(), goal.getTargetAmount(), goal.getSavedAmount(), recMonthly, reasoning, tradeOff));
        }

        return suggestions;
    }

    // 9. Month-End Predictor
    public IntelligenceReportDto.MonthEndPrediction predictMonthEnd(User currentUser) {
        User student = resolveStudent(currentUser);
        LocalDate today = LocalDate.now();
        YearMonth ym = YearMonth.from(today);

        int totalDays = ym.lengthOfMonth();
        int elapsedDays = Math.max(1, today.getDayOfMonth());

        BigDecimal spentSoFar = transactionRepository.sumAmountByOwnerAndTypeAndDateRange(
                student, TransactionType.EXPENSE, ym.atDay(1), today);
        if (spentSoFar == null) spentSoFar = BigDecimal.ZERO;

        BigDecimal dailyBurnRate = spentSoFar.divide(BigDecimal.valueOf(elapsedDays), 2, RoundingMode.HALF_UP);
        BigDecimal projectedMonthEnd = dailyBurnRate.multiply(BigDecimal.valueOf(totalDays)).setScale(2, RoundingMode.HALF_UP);

        BigDecimal lowBand = projectedMonthEnd.multiply(new BigDecimal("0.88")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal highBand = projectedMonthEnd.multiply(new BigDecimal("1.12")).setScale(2, RoundingMode.HALF_UP);

        IntelligenceReportDto.MonthEndPrediction pred = new IntelligenceReportDto.MonthEndPrediction();
        pred.setDaysElapsed(elapsedDays);
        pred.setTotalDaysInMonth(totalDays);
        pred.setSpentSoFar(spentSoFar);
        pred.setDailyBurnRate(dailyBurnRate);
        pred.setEstimatedMonthEndSpend(projectedMonthEnd);
        pred.setUncertaintyLow(lowBand);
        pred.setUncertaintyHigh(highBand);
        pred.setConfidenceMessage(String.format(
                "Based on current burn rate of ₹%s/day across %d elapsed days, projected month-end expenses are estimated between ₹%s and ₹%s.",
                dailyBurnRate, elapsedDays, lowBand, highBand));

        return pred;
    }

    // Comprehensive Intelligence Report
    public IntelligenceReportDto getFullIntelligenceReport(User currentUser) {
        IntelligenceReportDto report = new IntelligenceReportDto();
        report.setMoneyLeaks(detectMoneyLeaks(currentUser));

        BigDecimal leakTotal = report.getMoneyLeaks().stream()
                .map(IntelligenceReportDto.MoneyLeakItem::getTotalSpent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        report.setTotalLeakExposure(leakTotal);

        report.setAnomalies(detectAnomalies(currentUser));
        report.setCoachSuggestions(getGoalCoachSuggestions(currentUser));
        report.setMonthEndPrediction(predictMonthEnd(currentUser));

        return report;
    }

    // 3. Ask Your Money AI — Fully Conversational with Grounding & Dual-Engine Fallback
    public AiQuestionResponseDto askMoneyQuestion(User currentUser, String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new BadRequestException("Prompt cannot be empty.");
        }

        String userPrompt = prompt.trim();
        log.info("[AI Request Flow] Question received. userId={}, role={}, promptPreview='{}'",
                currentUser.getId(), currentUser.getRole(), userPrompt.substring(0, Math.min(50, userPrompt.length())));

        // 1. Build Grounded Financial Context (Authorization and Parent read-only enforced here)
        FinancialContextService.FinancialContext ctx = financialContextService.buildContext(currentUser);
        log.info("[AI Context Flow] Financial context built. studentId={}, currentYm={}, categoryCount={}",
                ctx.student.getId(), ctx.currentYm, ctx.currentCategoryTotals.size());

        // 2. Fetch Active Conversation History for Context Turns
        List<AiConversationMessage> history = aiConversationMessageRepository.findByUserOrderByCreatedAtAsc(currentUser);
        log.info("[AI History Flow] Retrieved history: {} past messages for userId={}", history.size(), currentUser.getId());

        String answer;
        String modelUsed;
        boolean isRealLlm = false;

        // 3. If Real LLM (Google Gemini) is Configured, dispatch request
        if (geminiClientService.isConfigured()) {
            log.info("[AI Dispatch] GEMINI_API_KEY is configured. Dispatching to Gemini model: {}", geminiClientService.getModelName());
            String systemInstruction = buildSystemPrompt(ctx);
            GeminiClientService.ProviderResult result = geminiClientService.generateChatResponse(systemInstruction, history, userPrompt);

            if (result.isSuccess()) {
                answer = result.getContent();
                modelUsed = "Shyam's Finance AI";
                isRealLlm = true;
                log.info("[AI Dispatch Success] Gemini response received successfully (length={} chars).", answer.length());
            } else {
                log.warn("[AI Provider Failure] Gemini call failed. Status: {}, Category: {}, Error: {}",
                        result.getHttpStatusCode(), result.getErrorCategory(), result.getErrorMessage());
                if ("TIMEOUT".equalsIgnoreCase(result.getErrorCategory())) {
                    answer = String.format(
                            "⏱️ **AI Response Delay Notice (Network Timeout - HTTP 504)**\n\n" +
                            "Shyam's Finance AI took longer than %d seconds to complete across your network connection.\n\n" +
                            "Please check your network connection and try again.",
                            geminiClientService.getTimeoutSeconds());
                } else {
                    answer = String.format(
                            "⚠️ **AI Provider Error (%s - HTTP %d)**\n\n" +
                            "%s\n\n" +
                            "---\n" +
                            "*Please verify your Google Gemini API key and quota in Google AI Studio.*",
                            result.getErrorCategory(), result.getHttpStatusCode(), result.getErrorMessage());
                }
                modelUsed = "Shyam's Finance AI Error (" + result.getErrorCategory() + ")";
                isRealLlm = false;
            }
        } else {
            // No API Key provided: Clearly communicate that setup is required
            log.info("[AI Status] GEMINI_API_KEY is not configured on the server. Explaining setup requirement.");
            answer = "### ⚙️ AI Provider Setup Required\n\n" +
                     "No AI model provider is currently configured for **Shyam's Finance AI**.\n\n" +
                     "To enable real-time conversational responses:\n" +
                     "1. Obtain an API key from [Google AI Studio](https://aistudio.google.com/).\n" +
                     "2. Set the `GEMINI_API_KEY` environment variable on the server, or enter it in the **AI Assistant Settings** modal.\n\n" +
                     "*(Conversational AI reasoning requires an active model provider. Static or hardcoded mock answers are disabled.)*";
            modelUsed = "No Provider Configured (Setup Required)";
            isRealLlm = false;
        }

        // 4. Save Conversation Turns to Database
        aiConversationMessageRepository.save(new AiConversationMessage(currentUser, "user", userPrompt));
        aiConversationMessageRepository.save(new AiConversationMessage(currentUser, "model", answer));

        // 5. Audit Log the AI interaction
        auditService.log(currentUser, "AI_QUESTION_ASKED", "INTELLIGENCE", null,
                "Asked AI assistant (Model: " + modelUsed + ")");

        log.info("[AI Request Completed] Handled question for userId={}, modelUsed={}, isRealLlm={}",
                currentUser.getId(), modelUsed, isRealLlm);

        return new AiQuestionResponseDto(answer, ctx.currentYm.toString(), ctx.evidenceLines, modelUsed, isRealLlm);
    }

    private String buildSystemPrompt(FinancialContextService.FinancialContext ctx) {
        return "You are 'Shyam's Finance AI', the Financial Intelligence Assistant for 'Finance System Manager', a fintech platform for students and families.\n" +
                "You have direct access to verified database ledger records for the student below.\n\n" +
                "STRICT INSTRUCTIONS:\n" +
                "1. Base answers strictly on the verified numbers in the CONTEXT below. Do NOT hallucinate transactions, balances, or dates.\n" +
                "2. Clearly distinguish recorded facts, calculated estimates, and general suggestions.\n" +
                "3. If the user asks in Hindi or Hinglish, respond naturally in Hindi/Hinglish (e.g. 'Aapka sabse bada kharcha Food & Mess par hai...').\n" +
                "4. Maintain conversation context across turns. Answer follow-up questions (e.g., 'Why?', 'What should I do next?') using the conversation history.\n" +
                "5. Explain calculations simply in rupees (₹) with clear markdown bullet points and bolding.\n" +
                "6. If the viewer is a Parent, respect read-only mode and do not reveal unshared student private details.\n" +
                "7. If requested information is not in the data, honestly say it is not recorded instead of guessing.\n\n" +
                ctx.contextPromptText;
    }

    // Context-Aware Deterministic Conversational Engine (Local Knowledge & Verified Ledger Engine)
    public String generateDeterministicConversationalResponse(
            FinancialContextService.FinancialContext ctx,
            List<AiConversationMessage> history,
            String prompt,
            User currentUser) {

        String p = prompt.toLowerCase().trim();

        // Extract last assistant message and last user message from conversation history
        String lastAssistantMsg = "";
        String lastUserMsg = "";
        if (history != null && !history.isEmpty()) {
            for (int i = history.size() - 1; i >= 0; i--) {
                AiConversationMessage m = history.get(i);
                if ("model".equalsIgnoreCase(m.getRole()) && lastAssistantMsg.isEmpty()) {
                    lastAssistantMsg = m.getContent().toLowerCase();
                } else if ("user".equalsIgnoreCase(m.getRole()) && lastUserMsg.isEmpty()) {
                    lastUserMsg = m.getContent().toLowerCase();
                }
                if (!lastAssistantMsg.isEmpty() && !lastUserMsg.isEmpty()) break;
            }
        }

        // 1. Identity & Greeting Intents ("Hello, who are you?", "Who are you?", "Hi", "Namaste")
        if (p.contains("who are you") || p.contains("what are you") || p.contains("aap kaun ho") ||
                p.equals("hi") || p.equals("hello") || p.equals("namaste") ||
                p.startsWith("hi ") || p.startsWith("hello ") || p.startsWith("namaste ") ||
                p.startsWith("hey ") || p.contains("introduce yourself") || p.contains("kya kar sakte ho")) {

            boolean isHindi = p.contains("namaste") || p.contains("kaun ho") || p.contains("kya kar sakte");
            if (isHindi) {
                return String.format(
                        "### 🤖 Namaste! Main aapka Financial Assistant hoon\n\n" +
                        "Main Finance System Manager ka AI assistant hoon. Main aapke verified financial ledger records se live data analyze karke financial guidance deta hoon.\n\n" +
                        "**Main yeh sab bata sakta hoon:**\n" +
                        "• **Spending Analysis**: Aapka paisa kahan ja raha hai aur sabse zyada kharcha kis category mein hai (Abhi: **%s**, ₹%s).\n" +
                        "• **Month Comparison**: Pichle mahine se kharche ka comparison.\n" +
                        "• **Financial Concepts**: Compound interest, SIP, budgeting rules aur savings strategies.\n" +
                        "• **Budget Impact**: Kisi naye kharche ka budget par asar.\n\n" +
                        "*Aap mujhse koi bhi financial sawal pooch sakte hain!*",
                        ctx.topCategory, ctx.topCategoryAmount);
            }

            return String.format(
                    "### 🤖 Hello! I am your Financial Intelligence Assistant\n\n" +
                    "I am the conversational finance assistant built into **Finance System Manager**. I analyze your verified ledger records and provide actionable financial guidance.\n\n" +
                    "**What I can do for you:**\n" +
                    "• **Analyze Spending**: Pinpoint where your money is going across categories (Current highest: **%s**, ₹%s).\n" +
                    "• **Month-over-Month Comparison**: Compare your current spending against previous periods.\n" +
                    "• **Explain Financial Concepts**: Clarify compound interest, the 50/30/20 rule, SIPs, and inflation.\n" +
                    "• **Budget & Goals Guidance**: Calculate target monthly savings and assess transaction impact on budget limits.\n\n" +
                    "Feel free to ask questions like *'Explain compound interest'*, *'Give me three ways to reduce unnecessary spending'*, or *'Where is most of my money going?'*.",
                    ctx.topCategory, ctx.topCategoryAmount);
        }

        // 2. Educational Financial Concept: Compound Interest ("Explain compound interest")
        if (p.contains("compound interest") || p.contains("compounding") || p.contains("chakravridhi byaj")) {
            return "### 📈 Understanding Compound Interest\n\n" +
                    "**Compound interest** is interest calculated on both the initial principal and the accumulated interest from prior periods. In simple terms, it is **'interest earning interest'**, allowing your money to grow exponentially over time.\n\n" +
                    "#### The Mathematical Formula:\n" +
                    "$$\\mathbf{A = P \\left(1 + \\frac{r}{n}\\right)^{nt}}$$\n\n" +
                    "• **A**: Final amount accumulated\n" +
                    "• **P**: Initial principal invested\n" +
                    "• **r**: Annual interest rate (e.g., 0.12 for 12%)\n" +
                    "• **n**: Number of times interest compounds per year (e.g., 12 for monthly compounding)\n" +
                    "• **t**: Time invested in years\n\n" +
                    "#### Real-World Example for Students:\n" +
                    "If you invest **₹1,000/month** in a mutual fund SIP yielding an assumed **12% annual return** for **5 years**:\n" +
                    "• **Total Invested**: ₹60,000\n" +
                    "• **Estimated Maturity Value**: ~**₹82,486** (over ₹22,480 generated purely through compounding!)\n\n" +
                    "💡 *Key Takeaway: The earlier you start investing—even small amounts like ₹500 or ₹1,000—the more dramatic the exponential compounding effect becomes.*";
        }

        // 3. Actionable Spending Advice: Three Ways to Reduce Unnecessary Spending
        if (p.contains("three ways") || p.contains("reduce unnecessary spending") || p.contains("ways to reduce spending") ||
                p.contains("cut expenses") || p.contains("kam kharch") || p.contains("bachat kaise kare")) {
            return String.format(
                    "### 💡 Three Practical Ways to Reduce Unnecessary Spending\n\n" +
                    "Based on financial discipline principles and your student spending profile (where **%s** is currently your largest expenditure category at **₹%s**):\n\n" +
                    "1. **Apply the 24-Hour Rule for Discretionary Purchases**:\n" +
                    "   Before making non-essential purchases (gadgets, apparel, impulsive takeout), wait 24 hours. Over 65%% of impulse buying urges subside once the emotional dopamine rush fades.\n\n" +
                    "2. **Audit Recurring Subscriptions & Micro-Spends**:\n" +
                    "   Daily micro-purchases under ₹200 (such as snacks, premium delivery fees, or unused streaming memberships) accumulate silently. Check your **Money Leak Detector** tab to spot recurring small expenses.\n\n" +
                    "3. **Adopt Zero-Based Category Budgeting**:\n" +
                    "   Assign every rupee a job at the beginning of the month. In the **Budgets** tab, set a hard cap for your top discretionary categories (e.g., capping %s) with an 80%% warning threshold so you receive early warnings before overspending.\n\n" +
                    "👉 *Follow-up: Ask me 'What should I do next?' or 'How does this affect my savings goal?' to create an immediate plan!*",
                    ctx.topCategory, ctx.topCategoryAmount, ctx.topCategory);
        }

        // 4. Contextual Follow-Up Handling ("Based on the previous answer", "What should I do next?", "Why?")
        if (p.contains("what should i do next") || p.contains("next step") || p.equals("why?") ||
                p.contains("based on the previous") || p.contains("iske baad") || p.contains("follow up")) {

            if (lastAssistantMsg.contains("three ways") || lastAssistantMsg.contains("reduce unnecessary spending")) {
                return String.format(
                        "### 🎯 Step-by-Step Implementation of Your Savings Plan\n\n" +
                        "Following the 3 spending reduction strategies we discussed:\n\n" +
                        "1. **Immediate Step Today**: Navigate to the **Budgets** tab and create a monthly budget cap of **₹%s** for **%s**.\n" +
                        "2. **Automate Savings**: In the **Savings Goals** tab, set a monthly target. Even saving ₹500/month from trimmed dining/snacks frees up ₹6,000/year.\n" +
                        "3. **Start Small with SIP**: If you have not registered a SIP, invest ₹500–₹1,000/month in an index fund to put compound interest to work immediately.\n\n" +
                        "Would you like me to project your savings velocity for next month?",
                        ctx.topCategoryAmount.multiply(new BigDecimal("0.85")).setScale(0, RoundingMode.UP), ctx.topCategory);
            } else if (lastAssistantMsg.contains("compound interest")) {
                return "### 🎯 Next Steps: Putting Compound Interest to Work\n\n" +
                        "1. **Open the SIP Planner Tab**: Explore setting up a Systematic Investment Plan with an assumed return rate.\n" +
                        "2. **Target Consistency Over Size**: Starting with ₹500/month today is far more effective than waiting 3 years to start with ₹2,000/month.\n" +
                        "3. **Track Your Milestones**: Link your investment records in the **Investment Portfolio** view to track your progress.\n\n" +
                        "Ask me *'How much can I save next month?'* to see how much cash you can deploy!";
            } else if (lastAssistantMsg.contains("spending breakdown") || lastAssistantMsg.contains("most of your money")) {
                return String.format(
                        "### 🎯 Next Steps for Managing %s\n\n" +
                        "1. **Set an Early Warning Cap**: In the Budgets tab, cap this category at **₹%s**.\n" +
                        "2. **Run What-If Simulation**: Test what happens if you reduce spending in this category by ₹500/month in the **What-If Simulator** tab.\n" +
                        "3. **Track Micro-transactions**: Review transactions under ₹300 in this category to prevent small purchases from accumulating.",
                        ctx.topCategory, ctx.topCategoryAmount.multiply(new BigDecimal("1.10")).setScale(0, RoundingMode.UP));
            } else {
                return String.format(
                        "### 🎯 Recommended Priority Actions\n\n" +
                        "1. **Review Top Category**: Your highest spend this month is **%s** at **₹%s**.\n" +
                        "2. **Ensure Active Budgets**: Ensure all major categories have defined monthly spending limits.\n" +
                        "3. **Inspect Money Leaks**: Check the Money Leak Detector tab for recurring micro-expenses.",
                        ctx.topCategory, ctx.topCategoryAmount);
            }
        }

        // 5. Where is money going / Category breakdown
        if ((p.contains("where") && p.contains("money")) || p.contains("largest") || p.contains("top category") ||
                p.contains("most of my money") || p.contains("kahan ja raha") || p.contains("sabse zyada kharch") || p.contains("breakdown")) {

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("### 📊 Verified Spending Breakdown for %s\n\n", ctx.currentYm));
            sb.append(String.format("Most of your recorded money is going towards **%s**, totaling **₹%s** (accounting for **%.1f%%** of your total expenses of ₹%s).\n\n",
                    ctx.topCategory, ctx.topCategoryAmount,
                    ctx.currentMonthExpense.compareTo(BigDecimal.ZERO) > 0
                            ? (ctx.topCategoryAmount.doubleValue() / ctx.currentMonthExpense.doubleValue()) * 100 : 0.0,
                    ctx.currentMonthExpense));

            sb.append("**Category Summary This Month:**\n");
            for (Map.Entry<String, BigDecimal> e : ctx.currentCategoryTotals.entrySet()) {
                double pct = ctx.currentMonthExpense.compareTo(BigDecimal.ZERO) > 0
                        ? (e.getValue().doubleValue() / ctx.currentMonthExpense.doubleValue()) * 100 : 0.0;
                sb.append(String.format("• **%s**: ₹%s (%.1f%%)\n", e.getKey(), e.getValue(), pct));
            }

            sb.append("\n💡 *Action: Setting a budget cap for '").append(ctx.topCategory).append("' can prevent budget overruns.*");
            return sb.toString();
        }

        // 6. Month-over-Month Comparison / Why expenses higher
        if (!p.contains("fund") && !p.contains("scheme") && (p.contains("higher") || p.contains("more") || p.contains("increase") ||
                p.contains("compare") || p.contains("last month") || p.contains("pichle mahine") || p.contains("kyun badha"))) {

            BigDecimal curExp = ctx.currentMonthExpense != null ? ctx.currentMonthExpense : BigDecimal.ZERO;
            BigDecimal prevExp = ctx.previousMonthExpense != null ? ctx.previousMonthExpense : BigDecimal.ZERO;
            BigDecimal diff = curExp.subtract(prevExp);
            boolean isHigher = diff.compareTo(BigDecimal.ZERO) > 0;

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("### 📈 Monthly Comparison: %s vs %s\n\n", ctx.currentYm, ctx.previousYm));
            sb.append(String.format("• **%s Expenses**: ₹%s\n", ctx.currentYm, curExp));
            sb.append(String.format("• **%s Expenses**: ₹%s\n", ctx.previousYm, prevExp));
            sb.append(String.format("• **Net Difference**: %s₹%s (%s than last month)\n\n",
                    isHigher ? "+" : "", diff.abs(), isHigher ? "higher" : "lower"));

            sb.append("**Category Drivers:**\n");
            boolean foundChange = false;
            for (Map.Entry<String, BigDecimal> e : ctx.currentCategoryTotals.entrySet()) {
                BigDecimal prev = ctx.previousCategoryTotals.getOrDefault(e.getKey(), BigDecimal.ZERO);
                BigDecimal catDiff = e.getValue().subtract(prev);
                if (catDiff.abs().compareTo(new BigDecimal("100")) > 0) {
                    foundChange = true;
                    sb.append(String.format("• **%s**: ₹%s vs ₹%s (%s₹%s)\n",
                            e.getKey(), e.getValue(), prev, catDiff.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "", catDiff));
                }
            }
            if (!foundChange) {
                sb.append("• Category expenditures have remained steady across months.\n");
            }

            if (isHigher) {
                sb.append("\n🔍 *Summary: The increase was primarily driven by higher spending in your top categories.*");
            } else {
                sb.append("\n👏 *Great job! Your current month spending is below the previous month.*");
            }
            return sb.toString();
        }

        // 7. How much can I save next month / Savings capacity
        if (p.contains("how much can i save") || p.contains("save next month") || p.contains("savings potential") ||
                p.contains("kitna bacha") || p.contains("savings capacity") || p.contains("savings velocity")) {

            if (currentUser.getRole() == Role.STUDENT) {
                BigDecimal projectedSavings = ctx.currentMonthNet.compareTo(BigDecimal.ZERO) > 0
                        ? ctx.currentMonthNet
                        : new BigDecimal("2000.00");
                BigDecimal trimmedDiscretionary = ctx.topCategoryAmount.multiply(new BigDecimal("0.15")).setScale(0, RoundingMode.HALF_UP);

                return String.format(
                        "### 💰 Next Month Savings Projection\n\n" +
                        "Based on your recorded ledger (Income: **₹%s**, Expenses: **₹%s**, Net Cash Flow: **₹%s**):\n\n" +
                        "1. **Baseline Target**: You can realistically save approximately **₹%s** next month.\n" +
                        "2. **Optimization Potential**: Trimming 15%% from **%s** recovers an additional **₹%s**.\n" +
                        "3. **Annual Impact**: Saving ₹%s each month builds **₹%s/year** in liquid capital.\n\n" +
                        "💡 *Recommendation: Set up an automated recurring savings goal or SIP on the day your allowance arrives.*",
                        ctx.currentMonthIncome, ctx.currentMonthExpense, ctx.currentMonthNet,
                        projectedSavings, ctx.topCategory, trimmedDiscretionary,
                        projectedSavings, projectedSavings.multiply(new BigDecimal("12")));
            } else {
                return String.format(
                        "### 📊 Student Monthly Expense Baseline\n\n" +
                        "The student has recorded **₹%s** in expenses for %s (compared to ₹%s in %s). " +
                        "Monthly recurring allowance requirements are estimated at **₹%s/month**.",
                        ctx.currentMonthExpense, ctx.currentYm, ctx.previousMonthExpense, ctx.previousYm, ctx.currentMonthExpense);
            }
        }

        // 8. Hypothetical transaction impact (e.g. "I spent ₹500 on Food yesterday")
        if (p.contains("spent") && (p.contains("yesterday") || p.contains("today") || p.contains("affect") || p.contains("budget"))) {
            String matchedCat = ctx.topCategory;
            for (String cat : STUDENT_CATEGORIES) {
                if (p.contains(cat.toLowerCase()) || (cat.contains("&") && p.contains(cat.split("&")[0].trim().toLowerCase()))) {
                    matchedCat = cat;
                    break;
                }
            }
            final String targetCategory = matchedCat;

            BigDecimal amount = new BigDecimal("500");
            Matcher m = Pattern.compile("(\\d+)(\\.\\d+)?").matcher(p);
            if (m.find()) {
                amount = new BigDecimal(m.group());
            }

            Budget relevantBudget = ctx.budgets.stream()
                    .filter(b -> b.getCategory().equalsIgnoreCase(targetCategory))
                    .findFirst().orElse(null);

            BigDecimal currentSpend = ctx.currentCategoryTotals.getOrDefault(matchedCat, BigDecimal.ZERO);
            BigDecimal newSpend = currentSpend.add(amount);

            if (relevantBudget != null) {
                double pct = newSpend.divide(relevantBudget.getLimitAmount(), 4, RoundingMode.HALF_UP).doubleValue() * 100;
                return String.format(
                        "### 💳 Budget Impact for %s\n\n" +
                        "• **Category**: %s\n" +
                        "• **Current Recorded Spend**: ₹%s\n" +
                        "• **After ₹%s Expense**: ₹%s of ₹%s budget limit (**%.1f%%** used)\n" +
                        "• **Status**: %s\n\n" +
                        "💡 *Remaining allowance for this month: **₹%s**.*",
                        matchedCat, matchedCat, currentSpend, amount, newSpend,
                        relevantBudget.getLimitAmount(), pct,
                        pct >= 100 ? "⚠️ **OVER BUDGET**" : (pct >= relevantBudget.getWarningThreshold().doubleValue() ? "⚠️ **WARNING THRESHOLD REACHED**" : "✅ **HEALTHY**"),
                        relevantBudget.getLimitAmount().subtract(newSpend).max(BigDecimal.ZERO));
            } else {
                return String.format(
                        "### 💳 Expense Impact for %s\n\n" +
                        "Adding ₹%s to **%s** increases its monthly spend from ₹%s to **₹%s**.\n\n" +
                        "⚠️ *Note: You do not currently have a set monthly budget limit for %s. Consider creating a cap of ₹%s to monitor spending.*",
                        matchedCat, amount, matchedCat, currentSpend, newSpend, matchedCat,
                        newSpend.multiply(new BigDecimal("1.25")).setScale(0, RoundingMode.UP));
            }
        }

        // 9. Savings Goals & Targets
        if (p.contains("savings goal") || p.contains("reach my goal") || p.contains("target") || p.contains("goal")) {
            if (ctx.goals.isEmpty()) {
                return "You do not have any active savings goals yet. Create one in the **Savings Goals** tab (e.g. 'Emergency Fund') and I will help calculate the optimal monthly target!";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("### 🎯 Active Savings Goals Strategy\n\n");
            for (SavingsGoal g : ctx.goals) {
                BigDecimal remaining = g.getTargetAmount().subtract(g.getSavedAmount());
                sb.append(String.format("• **%s**: Saved ₹%s of ₹%s (Remaining: **₹%s** | Deadline: %s)\n",
                        g.getName(), g.getSavedAmount(), g.getTargetAmount(), remaining,
                        g.getTargetDate() != null ? g.getTargetDate().toString() : "Flexible"));
            }

            sb.append("\n**Action Plan:**\n");
            sb.append(String.format("1. Redirect ₹500/month from non-essential spending in **%s** directly into your highest priority goal.\n", ctx.topCategory));
            sb.append("2. Set an automated recurring SIP or savings milestone on the day your monthly income arrives.");
            return sb.toString();
        }

        // 10. Money Leaks & Micro-Purchases
        if (p.contains("leak") || p.contains("micro") || p.contains("waste") || p.contains("small purchase")) {
            List<IntelligenceReportDto.MoneyLeakItem> leaks = detectMoneyLeaks(currentUser);
            if (leaks.isEmpty()) {
                return "✅ **No significant micro-spending leaks detected** in the past 30 days! Your small recurring transactions appear well controlled.";
            }
            StringBuilder sb = new StringBuilder();
            sb.append("### 🔍 Detected Money Leaks (Past 30 Days)\n\n");
            for (IntelligenceReportDto.MoneyLeakItem leak : leaks) {
                sb.append(String.format("• **%s** in *%s*: %d micro-transactions totaling **₹%s**.\n",
                        leak.getPattern(), leak.getCategory(), leak.getFrequency(), leak.getTotalSpent()));
            }
            sb.append("\n💡 *Small purchases under ₹300 accumulate rapidly unnoticed over time.*");
            return sb.toString();
        }

        // 11. SIP Affordability Assessment ("Meri income aur expenses ke hisaab se kitni SIP affordable hai?", "How much SIP is affordable?")
        if (p.contains("affordable") || p.contains("kitni sip") || p.contains("kitna invest") ||
                p.contains("sip capacity") || (p.contains("how much") && p.contains("sip")) || p.contains("can i invest")) {

            FinancialAssessmentDto assess = affordabilityService.calculateAffordability(currentUser, 6);
            boolean isHindi = p.contains("meri") || p.contains("hisaab") || p.contains("kitni") || p.contains("karna chahiye") || p.contains("bachat");

            if (isHindi) {
                StringBuilder sb = new StringBuilder();
                sb.append("### 📊 Aapki Monthly SIP Affordability Assessment\n\n");
                sb.append(String.format("Aapke verified ledger records ke hisaab se (**%s**):\n\n", assess.getAssessmentPeriod()));
                sb.append(String.format("• **Average Monthly Income**: ₹%s\n", assess.getAverageMonthlyIncome()));
                sb.append(String.format("• **Essential Expenses (Hostel, Mess, Bills)**: ₹%s\n", assess.getAverageMonthlyEssentialExpense()));
                sb.append(String.format("• **Existing SIP Commitments**: ₹%s/month\n", assess.getExistingMonthlySipCommitments()));
                sb.append(String.format("• **Net Discretionary Surplus**: ₹%s\n\n", assess.getNetMonthlyDiscretionarySurplus()));

                if (!assess.isEmergencyFundAdequate()) {
                    sb.append(String.format("⚠️ **Emergency Fund Alert**: Aapka liquid emergency reserve abhi sirf **%.1f mahine** cover karta hai (Target: 3 mahine).\n",
                            assess.getEmergencyReserveMonthsCovered()));
                    sb.append("Pehle emergency liquid fund ya bank savings me paise bachaayein, fir high-risk equity SIP shuru karein.\n\n");
                }

                sb.append(String.format("💡 **Sifarish (Recommended Affordable SIP Range)**:\n" +
                        "• **Safe / Conservative Capacity**: **₹%s/month**\n" +
                        "• **Optimal Recommended Capacity**: **₹%s/month**\n" +
                        "• **Max Prudent Limit**: **₹%s/month**\n\n",
                        assess.getConservativeRecommendedSip(), assess.getRecommendedSip(), assess.getMaxPrudentSip()));
                sb.append("⚠️ *Zaroori Note: Mutual fund investments market risks ke adheen hain. Essential kharche ke paise kabhi market me invest na karein.*");
                return sb.toString();
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("### 📊 Personalized Monthly SIP Affordability Assessment\n\n");
                sb.append(String.format("Computed from your authenticated ledger records (**%s**):\n\n", assess.getAssessmentPeriod()));
                sb.append(String.format("• **Average Monthly Income**: ₹%s\n", assess.getAverageMonthlyIncome()));
                sb.append(String.format("• **Essential Expenses (Rent, Food, Utilities)**: ₹%s\n", assess.getAverageMonthlyEssentialExpense()));
                sb.append(String.format("• **Existing SIP Commitments**: ₹%s/month\n", assess.getExistingMonthlySipCommitments()));
                sb.append(String.format("• **Net Discretionary Surplus (after 15%% safety buffer)**: ₹%s\n\n", assess.getNetMonthlyDiscretionarySurplus()));

                if (!assess.isEmergencyFundAdequate()) {
                    sb.append(String.format("🛡️ **Emergency Reserve Priority**: Your liquid reserve covers **%.1f months** of essential burn rate (Recommended: 3 months).\n",
                            assess.getEmergencyReserveMonthsCovered()));
                    sb.append("Financial Safety Rule: Prioritize parking surplus into an emergency Liquid Fund or bank fixed deposit before committing to volatile equity SIPs.\n\n");
                }

                sb.append(String.format("🎯 **Affordable Monthly SIP Range**:\n" +
                        "• **Conservative Capacity**: **₹%s/month**\n" +
                        "• **Recommended Capacity**: **₹%s/month**\n" +
                        "• **Maximum Prudent Allocation**: **₹%s/month**\n\n",
                        assess.getConservativeRecommendedSip(), assess.getRecommendedSip(), assess.getMaxPrudentSip()));
                sb.append("⚠️ *Regulatory Disclosure: Mutual fund investments are subject to market risks. Calculations are deterministic financial planning estimates and not guaranteed investment returns.*");
                return sb.toString();
            }
        }

        // 12. Scheme Comparison ("Compare these funds using current available data", "Compare mutual funds")
        if (p.contains("compare") && (p.contains("fund") || p.contains("scheme") || p.contains("mutual") || p.contains("nav"))) {
            SchemeComparisonResponseDto comp = suitabilityService.compareSchemes(List.of("120716", "122639", "120828", "120503"));
            StringBuilder sb = new StringBuilder();
            sb.append("### 🔍 Verified Scheme Comparison (Official AMFI Data)\n\n");
            sb.append("| Scheme Name | Category | Latest NAV | NAV Date | 1Y Return | 3Y CAGR | 3Y Volatility | Expense Ratio | Riskometer |\n");
            sb.append("| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |\n");

            for (var s : comp.getSchemes()) {
                sb.append(String.format("| **%s** | %s | ₹%s | %s | %s%% | %s%% | %s%% | %s%% | %s |\n",
                        s.getSchemeName().replace(" - Direct Plan - Growth", "").replace(" Option", ""),
                        s.getCategory(), s.getNav(), s.getNavDate(),
                        s.getReturn1Y() != null ? s.getReturn1Y() : "N/A",
                        s.getReturn3Y() != null ? s.getReturn3Y() : "N/A",
                        s.getVolatility3Y() != null ? s.getVolatility3Y() : "N/A",
                        s.getExpenseRatioEstimated() != null ? s.getExpenseRatioEstimated() : "N/A",
                        s.getRiskometer()));
            }

            sb.append("\n**Key Comparison Insights:**\n");
            for (String diff : comp.getKeyDifferences()) {
                sb.append("• ").append(diff).append("\n");
            }
            sb.append("\n⚠️ *Responsible Research Note: Do not rank funds by past returns alone. Lower 3-year volatility and minimal expense ratios often lead to better investor outcomes over long compounding horizons.*");
            return sb.toString();
        }

        // 13. Suitability & Risk Profile Match ("Why does this option match my risk profile?")
        if (p.contains("match my risk") || p.contains("why does this option match") ||
                p.contains("risk profile match") || p.contains("suitability") || p.contains("match karta")) {

            InvestorProfileDto prof = affordabilityService.getProfile(currentUser);
            List<MutualFundSchemeDto> recs = suitabilityService.getRecommendedSchemes(currentUser);

            StringBuilder sb = new StringBuilder();
            sb.append("### 🎯 Suitability Matching Analysis\n\n");
            sb.append(String.format("Your current recorded profile: **Risk Tolerance: %s**, **Horizon: %s**, **Primary Goal: %s**.\n\n",
                    prof.getRiskTolerance(), prof.getInvestmentHorizon(), prof.getPrimaryGoal()));
            sb.append("**Eligible Shortlisted Options & Matching Rationale:**\n\n");

            for (var s : recs) {
                sb.append(String.format("1. **%s** (%s) — Match Score: **%d/100**\n", s.getSchemeName(), s.getCategory(), s.getMatchScore()));
                sb.append(String.format("   • **Why it matches**: %s\n", s.getSuitabilityMatchReason()));
                sb.append(String.format("   • **Verified AMFI NAV**: ₹%s (As of %s) | Expense: %s%% | Risk: %s\n\n",
                        s.getNav(), s.getNavDate(), s.getExpenseRatioEstimated(), s.getRiskometer()));
            }

            sb.append("💡 *Educational Disclosure: Shortlists are derived deterministically based on your risk tolerance and horizon. No fund is universally best for all investors.*");
            return sb.toString();
        }

        // 14. What-If SIP Simulation ("What if I invest ₹1,000 or ₹2,000 monthly?", "What if I invest")
        if (p.contains("what if i invest") || p.contains("agar main invest") || p.contains("1000 or 2000") ||
                p.contains("1000 ya 2000") || (p.contains("invest") && (p.contains("1000") || p.contains("2000")))) {

            SipSimulationResponseDto sim1000 = suitabilityService.simulateSip(new SipSimulationRequestDto(new BigDecimal("1000"), 5, new BigDecimal("12.0")));
            SipSimulationResponseDto sim2000 = suitabilityService.simulateSip(new SipSimulationRequestDto(new BigDecimal("2000"), 5, new BigDecimal("12.0")));

            return String.format(
                    "### 📈 SIP Compounding Scenario Comparison (5-Year Horizon at 12%% Assumed Return)\n\n" +
                    "| Monthly Contribution | Total Principal Invested | Projected Maturity Value | Estimated Wealth Gain |\n" +
                    "| :--- | :--- | :--- | :--- |\n" +
                    "| **₹1,000/month** | ₹%s | **₹%s** | +₹%s |\n" +
                    "| **₹2,000/month** | ₹%s | **₹%s** | +₹%s |\n\n" +
                    "**Compounding Acceleration:**\n" +
                    "• Doubling your SIP from ₹1,000 to ₹2,000 doubles your wealth generation, creating over **₹%s** in pure compounded returns.\n" +
                    "• If extended to **10 years**, a ₹2,000/month SIP accumulates an estimated **₹4,64,678** on a ₹2,40,000 principal investment.\n\n" +
                    "⚠️ *Hypothetical Illustrative Projection: Assumed 12%% annual rate is for mathematical demonstration only. Actual mutual fund returns fluctuate with equity markets and are never guaranteed.*",
                    sim1000.getTotalPrincipalInvested(), sim1000.getProjectedMaturityValue(), sim1000.getEstimatedWealthGain(),
                    sim2000.getTotalPrincipalInvested(), sim2000.getProjectedMaturityValue(), sim2000.getEstimatedWealthGain(),
                    sim2000.getEstimatedWealthGain());
        }

        // 15. Data Source & NAV Date ("Show the data date and sources", "data source", "amfi source")
        if (p.contains("data date") || p.contains("data source") || p.contains("sources") ||
                p.contains("where does this data come from") || p.contains("amfi data")) {

            LocalDateTime refresh = amfiNavService.getLastSuccessfulRefresh();
            boolean stale = amfiNavService.isStale();
            int count = amfiNavService.getAllSchemes().size();

            return String.format(
                    "### 🏛️ Verified Mutual Fund Data Sources & Governance\n\n" +
                    "• **Primary Official Data Provider**: **Association of Mutual Funds in India (AMFI)**\n" +
                    "• **Official NAV Portal**: [AMFI NAV Download](https://www.amfiindia.com/net-asset-value/nav-download)\n" +
                    "• **Pricing Model**: **End-of-Day (EOD) Official Net Asset Value** (Updated daily on business trading days; delayed EOD, not tick-by-tick live pricing).\n" +
                    "• **Historical Return & Volatility Source**: AMFI Historical Registry via verified NAV mirrors.\n" +
                    "• **Last Successful Refresh**: `%s` (Status: %s)\n" +
                    "• **Curated Active Schemes Tracked**: **%d Benchmark Schemes** (Large Cap, Flexi Cap, Mid Cap, Small Cap, Hybrid, Liquid Debt).\n\n" +
                    "💡 *Transparency Commitment: When live connection is unavailable, cached snapshots are clearly marked with staleness disclosures. No NAVs or return figures are ever fabricated.*",
                    refresh != null ? refresh.toString() : "Initialized Snapshot",
                    stale ? "⚠️ Cached Snapshot" : "✅ Fresh Live Feed",
                    count);
        }

        // 16. Actionable Investment Guidance ("In which should I invest?", "Where to invest?", "Which fund should I choose?")
        if (p.contains("which should i invest") || p.contains("where should i invest") ||
                p.contains("in which should i invest") || p.contains("how should i invest") ||
                p.contains("what should i invest") || p.contains("kahan invest") ||
                p.contains("invest kisme kare") || p.contains("recommend fund") ||
                p.contains("which fund") || p.contains("best investment") ||
                p.contains("where to invest") || p.contains("how to invest") ||
                (p.contains("invest") && (p.contains("which") || p.contains("where") || p.contains("how") || p.contains("suggest") || p.contains("recommend") || p.contains("should")))) {

            StringBuilder sb = new StringBuilder();
            sb.append("### 🎯 Personalized Investment Strategy & Scheme Selection\n\n");

            // 1. Check Affordability & Cash-Flow Capacity
            FinancialAssessmentDto assess = affordabilityService.calculateAffordability(currentUser, 6);
            BigDecimal capacity = assess.getRecommendedSip();
            sb.append(String.format("Based on your verified historical ledger (Avg Income: **₹%s**, Avg Expenses: **₹%s**):\n\n",
                    assess.getAverageMonthlyIncome(), assess.getAverageMonthlyExpense()));
            sb.append(String.format("• **Estimated Monthly Investment Capacity**: **₹%s/month** (%s Status)\n",
                    capacity, assess.getAffordabilityStatus()));

            // 2. Emergency Reserve Priority Check
            if (assess.getCurrentEmergencyReserve().compareTo(assess.getRequiredEmergencyReserve()) < 0) {
                sb.append(String.format("• ⚠️ **Priority Pre-requisite**: Your liquid emergency fund is currently **₹%s** (Target: **₹%s**). Never invest money needed for near-term necessities.\n\n",
                        assess.getCurrentEmergencyReserve(), assess.getRequiredEmergencyReserve()));
            } else {
                sb.append(String.format("• ✅ **Emergency Reserve Secured**: You have **₹%s** in emergency savings covering basic obligations.\n\n",
                        assess.getCurrentEmergencyReserve()));
            }

            // 3. Recommended AMFI Schemes
            List<MutualFundSchemeDto> recs = suitabilityService.getRecommendedSchemes(currentUser);
            if (recs != null && !recs.isEmpty()) {
                sb.append("#### 🏆 Recommended Mutual Fund Schemes (AMFI Verified Daily NAV):\n\n");
                int rank = 1;
                for (MutualFundSchemeDto scheme : recs.stream().limit(3).toList()) {
                    sb.append(String.format("%d. **%s** (%s)\n", rank++, scheme.getSchemeName(), scheme.getCategory()));
                    sb.append(String.format("   - **Latest NAV**: ₹%s (Date: %s)\n", scheme.getNav(), scheme.getNavDate()));
                    sb.append(String.format("   - **Performance**: 1Y Return: **%s%%** | 3Y CAGR: **%s%%** | Volatility: **%s%%**\n",
                            scheme.getReturn1Y(), scheme.getReturn3Y(), scheme.getVolatility3Y()));
                    sb.append(String.format("   - **Why Suitable**: %s\n", scheme.getSuitabilityMatchReason()));
                    sb.append(String.format("   - **Risk Level**: %s | Est. Expense Ratio: %s%%\n\n",
                            scheme.getRiskometer(), scheme.getExpenseRatioEstimated()));
                }
            }

            // 4. Actionable Next Step
            sb.append("💡 **Recommended Action**:\n");
            sb.append("• Start with an automated SIP of **₹500 to ₹1,000/month** on the **SIP Tracker** tab.\n");
            sb.append("• If you have uninvested emergency capital, prioritize building your liquid buffer before allocating to volatile equity.\n\n");
            sb.append("*(Regulatory Notice: Mutual fund investments are subject to market risks. Read all scheme related documents carefully. Historical returns do not guarantee future performance.)*");

            return sb.toString();
        }

        // 17. Existing SIP & Investment commitments
        if (p.contains("sip") || p.contains("investment") || p.contains("mutual fund")) {
            List<SipRecord> sips = sipRecordRepository.findByStudentOrderByCreatedAtDesc(ctx.student);
            if (sips.isEmpty()) {
                return "You currently have no registered SIP plans. Starting an automated SIP of just ₹500–₹1,000/month in an index fund is a great discipline for students!";
            }
            BigDecimal totalSip = sips.stream().map(SipRecord::getMonthlyAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("### 📈 SIP Commitments (%d Active Plans)\n\n", sips.size()));
            sb.append(String.format("Total monthly systematic investment commitment: **₹%s**\n\n", totalSip));
            for (SipRecord s : sips) {
                sb.append(String.format("• **%s**: ₹%s/month | Return Rate: %s%% | Status: %s\n",
                        s.getFundName(), s.getMonthlyAmount(), s.getAssumedReturnRate(), s.getPaymentStatus()));
            }
            return sb.toString();
        }

        // 12. General Educational Financial Concepts (Inflation, Emergency Fund, 50/30/20)
        if (p.contains("inflation") || p.contains("emergency fund") || p.contains("50/30/20") || p.contains("budget rule")) {
            if (p.contains("inflation")) {
                return "### 📉 What is Inflation?\n\n" +
                        "**Inflation** is the rate at which the general prices for goods and services rise over time, causing each rupee to buy fewer goods.\n\n" +
                        "• **The Threat**: If inflation is 6% per year, ₹1,000 today will only have the purchasing power of ~₹940 next year.\n" +
                        "• **The Solution**: Keeping money in low-yield savings accounts loses purchasing power. Investing in assets that beat inflation (like equity SIPs or index funds) preserves your wealth over time.";
            } else if (p.contains("emergency fund")) {
                return "### 🛡️ Building an Emergency Fund\n\n" +
                        "An **Emergency Fund** is money set aside exclusively for unexpected financial shocks (e.g. medical expenses, urgent device repairs, sudden travel).\n\n" +
                        "• **Recommended Size for Students**: 2–3 months of basic living expenses (typically ₹10,000–₹25,000).\n" +
                        "• **Where to Keep It**: In a high-liquidity instrument like a separate bank account or liquid mutual fund, not in volatile stocks.";
            } else {
                return "### 📊 The 50/30/20 Budgeting Rule\n\n" +
                        "The 50/30/20 rule divides your post-income cash flow into three clear buckets:\n\n" +
                        "• **50% Needs**: Essential expenses (Hostel/Rent, Mess, College Fees, Commute).\n" +
                        "• **30% Wants**: Discretionary lifestyle spending (Dining out, Entertainment, Shopping).\n" +
                        "• **20% Savings & Debt**: Building an emergency fund, savings goals, and SIP investments.";
            }
        }

        // 13. Truthful response for open-ended queries not covered by local finance rules
        return String.format(
                "### ℹ️ AI Provider Notice\n\n" +
                "I am currently running in **Local Financial Knowledge Mode** because `GEMINI_API_KEY` is not configured on the server.\n\n" +
                "I could not find a verified financial ledger match or built-in finance concept for: *\"%s\"*.\n\n" +
                "**What you can do:**\n" +
                "1. **Ask Financial Questions**: Try *'Explain compound interest'*, *'Three ways to reduce spending'*, *'Where is my money going?'*, or *'How does this affect my budget?'*.\n" +
                "2. **Enable Shyam's Finance AI**: Set the `GEMINI_API_KEY` environment variable on the server to unlock dynamic generative reasoning for any question.",
                prompt);
    }

    // Conversation History Management
    public List<ChatMessageDto> getConversationHistory(User currentUser) {
        return aiConversationMessageRepository.findByUserOrderByCreatedAtAsc(currentUser)
                .stream()
                .map(m -> new ChatMessageDto(m.getId(), m.getRole(), m.getContent(), m.getCreatedAt()))
                .collect(Collectors.toList());
    }

    public void clearConversationHistory(User currentUser) {
        aiConversationMessageRepository.deleteByUser(currentUser);
        auditService.log(currentUser, "AI_CONVERSATION_CLEARED", "AI_CHAT", null, "User reset conversation context");
    }

    public Map<String, Object> getAiStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("isRealLlmConfigured", geminiClientService.isConfigured());
        status.put("activeModel", "Shyam's Finance AI");
        status.put("provider", geminiClientService.isConfigured() ? "Shyam's Finance AI" : "Local Financial Knowledge Engine (No Key Configured)");
        return status;
    }

    public void updateGeminiConfig(String apiKey, String model) {
        if (apiKey != null) geminiClientService.setApiKey(apiKey);
        if (model != null) geminiClientService.setModel(model);
    }

    // 4. Action-Taking AI Agent Proposals
    public List<AiActionDto> getProposedActions(User currentUser) {
        if (currentUser.getRole() == Role.PARENT) {
            return Collections.emptyList();
        }

        List<AiActionDto> proposals = new ArrayList<>();
        YearMonth currentYm = YearMonth.now();

        List<Budget> budgets = budgetRepository.findByStudentAndPeriodMonth(currentUser, currentYm.toString());
        Set<String> budgetedCats = budgets.stream().map(Budget::getCategory).collect(Collectors.toSet());

        List<Transaction> expenses = transactionRepository.findFiltered(
                currentUser, currentYm.atDay(1), currentYm.atEndOfMonth(), null, TransactionType.EXPENSE);

        Map<String, BigDecimal> catSpend = new HashMap<>();
        for (Transaction tx : expenses) {
            catSpend.merge(tx.getCategory(), tx.getAmount(), BigDecimal::add);
        }

        for (Map.Entry<String, BigDecimal> entry : catSpend.entrySet()) {
            if (!budgetedCats.contains(entry.getKey()) && entry.getValue().compareTo(new BigDecimal("1000")) > 0) {
                BigDecimal proposedLimit = entry.getValue().multiply(new BigDecimal("1.20")).setScale(0, RoundingMode.UP);
                Map<String, Object> payload = new HashMap<>();
                payload.put("category", entry.getKey());
                payload.put("periodMonth", currentYm.toString());
                payload.put("limitAmount", proposedLimit);
                payload.put("warningThreshold", 80.00);

                proposals.add(new AiActionDto(
                        "PROPOSAL_BUDGET_" + entry.getKey().replaceAll("\\s+", "_"),
                        "CREATE_BUDGET",
                        "Set Budget Limit for " + entry.getKey(),
                        String.format("You spent ₹%s in %s this month without a set limit. Proposed monthly cap: ₹%s (with 80%% warning).",
                                entry.getValue(), entry.getKey(), proposedLimit),
                        payload
                ));
            }
        }

        return proposals;
    }

    // 4. Action Confirmation & Execution
    public Map<String, Object> executeProposedAction(User currentUser, AiActionDto action) {
        if (currentUser.getRole() == Role.PARENT) {
            throw new ForbiddenException("Parents have strictly read-only access and cannot execute actions.");
        }

        Map<String, Object> result = new HashMap<>();
        if ("CREATE_BUDGET".equalsIgnoreCase(action.getActionType())) {
            Map<String, Object> payload = action.getPayload();
            BudgetDto budgetDto = new BudgetDto();
            budgetDto.setCategory((String) payload.get("category"));
            budgetDto.setPeriodMonth((String) payload.get("periodMonth"));
            budgetDto.setLimitAmount(new BigDecimal(payload.get("limitAmount").toString()));
            budgetDto.setWarningThreshold(new BigDecimal("80.00"));

            BudgetDto created = budgetService.createOrUpdateBudget(currentUser, budgetDto);
            auditService.log(currentUser, "AI_ACTION_EXECUTED", "BUDGET", created.getId().toString(),
                    "Created budget via AI proposal confirmation");

            result.put("status", "SUCCESS");
            result.put("message", "Confirmed and created monthly budget for " + created.getCategory() + " of ₹" + created.getLimitAmount());
            result.put("budget", created);
            return result;
        }

        throw new BadRequestException("Unsupported action type: " + action.getActionType());
    }
}
