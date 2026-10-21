package com.financemanager.service;

import com.financemanager.dto.FinancialAssessmentDto;
import com.financemanager.dto.InvestorProfileDto;
import com.financemanager.entity.*;
import com.financemanager.exception.ForbiddenException;
import com.financemanager.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class InvestmentAffordabilityService {

    private static final Logger log = LoggerFactory.getLogger(InvestmentAffordabilityService.class);

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private SipRecordRepository sipRecordRepository;

    @Autowired
    private SavingsGoalRepository savingsGoalRepository;

    @Autowired
    private InvestorProfileRepository investorProfileRepository;

    @Autowired
    private FamilyLinkService familyLinkService;

    private static final Set<String> ESSENTIAL_CATEGORIES = Set.of(
            "HOSTEL & RENT", "RENT", "UTILITIES", "BILLS", "ELECTRICITY",
            "FOOD & MESS", "GROCERIES", "MESS", "EDUCATION", "TUITION",
            "BOOKS & SUPPLIES", "HEALTH & MEDICAL", "MEDICAL", "TRANSPORT"
    );

    public User resolveEffectiveStudent(User currentUser) {
        if (currentUser.getRole() == Role.PARENT) {
            return familyLinkService.getActiveStudentForParent(currentUser);
        }
        return currentUser;
    }

    public FinancialAssessmentDto calculateAffordability(User currentUser, Integer lookbackMonths) {
        User student = resolveEffectiveStudent(currentUser);
        int months = (lookbackMonths != null && lookbackMonths > 0) ? lookbackMonths : 6;

        FinancialAssessmentDto dto = new FinancialAssessmentDto();
        dto.setAssessmentPeriod("Past " + months + " Months Verified Ledger Analysis");

        // 1. Fetch User's Investor Profile
        Optional<InvestorProfile> profileOpt = investorProfileRepository.findByStudent(student);
        InvestorProfile profile = profileOpt.orElse(null);

        boolean profileComplete = profile != null && Boolean.TRUE.equals(profile.getQuestionnaireCompleted());
        dto.setQuestionnaireCompleted(profileComplete);

        BigDecimal emergencyReserve = (profile != null && profile.getExistingEmergencyFund() != null)
                ? profile.getExistingEmergencyFund()
                : BigDecimal.ZERO;
        BigDecimal debtObligations = (profile != null && profile.getMonthlyDebtObligations() != null)
                ? profile.getMonthlyDebtObligations()
                : BigDecimal.ZERO;
        int targetReserveMonths = (profile != null && profile.getEmergencyFundTargetMonths() != null)
                ? profile.getEmergencyFundTargetMonths()
                : 3;

        dto.setCurrentEmergencyReserve(emergencyReserve);
        dto.setMonthlyDebtObligations(debtObligations);

        // 2. Fetch Historical Ledger Transactions
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(months).withDayOfMonth(1);

        List<Transaction> transactions = transactionRepository.findFiltered(student, startDate, endDate, null, null);

        // Group by Month to calculate monthly averages
        Map<YearMonth, BigDecimal> monthlyIncome = new HashMap<>();
        Map<YearMonth, BigDecimal> monthlyExpense = new HashMap<>();
        Map<YearMonth, BigDecimal> monthlyEssentialExpense = new HashMap<>();

        for (Transaction t : transactions) {
            YearMonth ym = YearMonth.from(t.getTransactionDate());
            BigDecimal amt = t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO;

            if (t.getType() == TransactionType.INCOME) {
                monthlyIncome.merge(ym, amt, BigDecimal::add);
            } else if (t.getType() == TransactionType.EXPENSE) {
                monthlyExpense.merge(ym, amt, BigDecimal::add);

                String cat = t.getCategory() != null ? t.getCategory().trim().toUpperCase() : "";
                if (isEssentialCategory(cat)) {
                    monthlyEssentialExpense.merge(ym, amt, BigDecimal::add);
                }
            }
        }

        // Active months count (at least 1 to avoid division by zero)
        int distinctMonths = Math.max(1, Math.max(monthlyIncome.size(), monthlyExpense.size()));

        BigDecimal totalIncome = monthlyIncome.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpense = monthlyExpense.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalEssential = monthlyEssentialExpense.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgIncome = totalIncome.divide(BigDecimal.valueOf(distinctMonths), 2, RoundingMode.HALF_UP);
        BigDecimal avgExpense = totalExpense.divide(BigDecimal.valueOf(distinctMonths), 2, RoundingMode.HALF_UP);
        BigDecimal avgEssential = totalEssential.divide(BigDecimal.valueOf(distinctMonths), 2, RoundingMode.HALF_UP);

        // Allow manual override if student explicitly specified monthly essential expenses
        if (profile != null && profile.getManualEssentialExpenseOverride() != null &&
                profile.getManualEssentialExpenseOverride().compareTo(BigDecimal.ZERO) > 0) {
            avgEssential = profile.getManualEssentialExpenseOverride();
        }

        BigDecimal avgDiscretionary = avgExpense.subtract(avgEssential);
        if (avgDiscretionary.compareTo(BigDecimal.ZERO) < 0) {
            avgDiscretionary = BigDecimal.ZERO;
        }

        dto.setAverageMonthlyIncome(avgIncome);
        dto.setAverageMonthlyExpense(avgExpense);
        dto.setAverageMonthlyEssentialExpense(avgEssential);
        dto.setAverageMonthlyDiscretionaryExpense(avgDiscretionary);

        // 3. Existing Monthly SIP Commitments
        List<SipRecord> existingSips = sipRecordRepository.findByStudentOrderByCreatedAtDesc(student);
        BigDecimal totalExistingSip = existingSips.stream()
                .filter(s -> "ACTIVE".equalsIgnoreCase(s.getPaymentStatus()))
                .map(SipRecord::getMonthlyAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setExistingMonthlySipCommitments(totalExistingSip);

        // 4. Existing Savings Goals Monthly Commitments
        List<SavingsGoal> goals = savingsGoalRepository.findByStudentOrderByCreatedAtDesc(student);
        BigDecimal monthlyGoals = BigDecimal.ZERO;
        for (SavingsGoal g : goals) {
            if (g.getTargetDate() != null && g.getTargetDate().isAfter(LocalDate.now())) {
                long monthsLeft = Math.max(1, ChronoUnit.MONTHS.between(LocalDate.now(), g.getTargetDate()));
                BigDecimal remaining = g.getTargetAmount().subtract(g.getSavedAmount() != null ? g.getSavedAmount() : BigDecimal.ZERO);
                if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                    monthlyGoals = monthlyGoals.add(remaining.divide(BigDecimal.valueOf(monthsLeft), 2, RoundingMode.HALF_UP));
                }
            }
        }
        dto.setMonthlySavingsGoalCommitments(monthlyGoals);

        // 5. Emergency Reserve Adequacy
        BigDecimal monthlyEssentialBurnRate = avgEssential.add(debtObligations);
        BigDecimal requiredEmergencyReserve = monthlyEssentialBurnRate.multiply(BigDecimal.valueOf(targetReserveMonths));
        dto.setRequiredEmergencyReserve(requiredEmergencyReserve);

        double monthsCovered = 0.0;
        if (monthlyEssentialBurnRate.compareTo(BigDecimal.ZERO) > 0) {
            monthsCovered = emergencyReserve.divide(monthlyEssentialBurnRate, 1, RoundingMode.HALF_UP).doubleValue();
        } else if (emergencyReserve.compareTo(BigDecimal.ZERO) > 0) {
            monthsCovered = 12.0; // High coverage if zero burn rate
        }
        dto.setEmergencyReserveMonthsCovered(monthsCovered);

        boolean emergencyAdequate = monthsCovered >= targetReserveMonths;
        dto.setEmergencyFundAdequate(emergencyAdequate);

        // 6. Net Monthly Discretionary Surplus Calculation
        // Surplus = Income - EssentialExpenses - ExistingSIPs - MonthlyGoalCommitments - DebtObligations - DiscretionaryBuffer(15%)
        BigDecimal discretionaryBuffer = avgIncome.multiply(new BigDecimal("0.15")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal committedOutflows = avgEssential
                .add(totalExistingSip)
                .add(monthlyGoals)
                .add(debtObligations)
                .add(discretionaryBuffer);

        BigDecimal surplus = avgIncome.subtract(committedOutflows);
        if (surplus.compareTo(BigDecimal.ZERO) < 0) {
            surplus = BigDecimal.ZERO;
        }
        dto.setNetMonthlyDiscretionarySurplus(surplus);

        // 7. Affordable SIP Range Determination
        List<String> observations = new ArrayList<>();
        List<String> missing = new ArrayList<>();

        if (profile == null || !profileComplete) {
            missing.add("Investor risk-tolerance questionnaire is incomplete.");
            missing.add("Emergency liquid cash reserve has not been formally verified.");
            missing.add("Debt / EMI obligations have not been confirmed.");
        }

        BigDecimal minSip = new BigDecimal("500.00");
        dto.setMinRecommendedSip(minSip);

        if (avgIncome.compareTo(BigDecimal.ZERO) <= 0 || surplus.compareTo(BigDecimal.ZERO) <= 0) {
            dto.setAffordabilityStatus("DEFICIT");
            dto.setConservativeRecommendedSip(BigDecimal.ZERO);
            dto.setRecommendedSip(BigDecimal.ZERO);
            dto.setMaxPrudentSip(BigDecimal.ZERO);
            observations.add("Your committed expenses and essential obligations match or exceed recorded monthly income.");
            observations.add("Never commit to market SIPs using money required for rent, food, or essential bills. Prioritize spending optimization first.");
        } else if (!emergencyAdequate && monthsCovered < 2.0) {
            dto.setAffordabilityStatus("EMERGENCY_PRIORITY");
            // If emergency fund is severely low, recommend starting with capital-safe liquid fund or bank deposit first
            BigDecimal conservative = roundToHundreds(surplus.multiply(new BigDecimal("0.25")));
            dto.setConservativeRecommendedSip(conservative.max(minSip));
            dto.setRecommendedSip(conservative.max(minSip));
            dto.setMaxPrudentSip(roundToHundreds(surplus.multiply(new BigDecimal("0.40"))).max(minSip));

            observations.add(String.format("Priority Notice: Your verified emergency reserve covers only %.1f months of essential burn rate (target: %d months).", monthsCovered, targetReserveMonths));
            observations.add("Financial Safety Rule: We recommend directing surplus into an emergency Liquid Fund or savings deposit before committing to high-volatility equity SIPs.");
        } else {
            dto.setAffordabilityStatus("HEALTHY");
            BigDecimal conservative = roundToHundreds(surplus.multiply(new BigDecimal("0.40"))).max(minSip);
            BigDecimal recommended = roundToHundreds(surplus.multiply(new BigDecimal("0.65"))).max(minSip);
            BigDecimal maxPrudent = roundToHundreds(surplus.multiply(new BigDecimal("0.85"))).max(minSip);

            dto.setConservativeRecommendedSip(conservative);
            dto.setRecommendedSip(recommended);
            dto.setMaxPrudentSip(maxPrudent);

            observations.add(String.format("Verified monthly surplus after essential needs, existing SIPs, and safety buffer is ₹%s.", surplus.toPlainString()));
            observations.add(String.format("Recommended monthly SIP contribution range: ₹%s to ₹%s.", conservative.toPlainString(), recommended.toPlainString()));
            if (totalExistingSip.compareTo(BigDecimal.ZERO) > 0) {
                observations.add(String.format("Existing active monthly SIP commitments: ₹%s already accounted for.", totalExistingSip.toPlainString()));
            }
        }

        dto.setAssessmentObservations(observations);
        dto.setMissingDetails(missing);
        return dto;
    }

    public InvestorProfileDto getProfile(User currentUser) {
        User student = resolveEffectiveStudent(currentUser);
        InvestorProfile profile = investorProfileRepository.findByStudent(student)
                .orElseGet(() -> new InvestorProfile(student));
        return toDto(profile);
    }

    public InvestorProfileDto saveProfile(User currentUser, InvestorProfileDto dto) {
        if (currentUser.getRole() == Role.PARENT) {
            throw new ForbiddenException("Parents have strictly read-only access and cannot alter investor risk profiles.");
        }

        InvestorProfile profile = investorProfileRepository.findByStudent(currentUser)
                .orElseGet(() -> new InvestorProfile(currentUser));

        if (dto.getRiskTolerance() != null) profile.setRiskTolerance(dto.getRiskTolerance().trim().toUpperCase());
        if (dto.getInvestmentHorizon() != null) profile.setInvestmentHorizon(dto.getInvestmentHorizon().trim().toUpperCase());
        if (dto.getPrimaryGoal() != null) profile.setPrimaryGoal(dto.getPrimaryGoal().trim().toUpperCase());
        if (dto.getExistingEmergencyFund() != null) profile.setExistingEmergencyFund(dto.getExistingEmergencyFund());
        if (dto.getMonthlyDebtObligations() != null) profile.setMonthlyDebtObligations(dto.getMonthlyDebtObligations());
        if (dto.getEmergencyFundTargetMonths() != null) profile.setEmergencyFundTargetMonths(dto.getEmergencyFundTargetMonths());
        if (dto.getManualEssentialExpenseOverride() != null) profile.setManualEssentialExpenseOverride(dto.getManualEssentialExpenseOverride());

        profile.setQuestionnaireCompleted(true);
        profile.setUpdatedAt(java.time.LocalDateTime.now());

        profile = investorProfileRepository.save(profile);
        return toDto(profile);
    }

    private boolean isEssentialCategory(String cat) {
        if (cat == null) return false;
        String upper = cat.trim().toUpperCase();
        for (String ess : ESSENTIAL_CATEGORIES) {
            if (upper.contains(ess)) return true;
        }
        return false;
    }

    private BigDecimal roundToHundreds(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;
        long val = amount.longValue();
        long rounded = Math.round((double) val / 100.0) * 100;
        return BigDecimal.valueOf(Math.max(500, rounded));
    }

    private InvestorProfileDto toDto(InvestorProfile p) {
        InvestorProfileDto dto = new InvestorProfileDto();
        dto.setId(p.getId());
        dto.setStudentId(p.getStudent() != null ? p.getStudent().getId() : null);
        dto.setRiskTolerance(p.getRiskTolerance());
        dto.setInvestmentHorizon(p.getInvestmentHorizon());
        dto.setPrimaryGoal(p.getPrimaryGoal());
        dto.setExistingEmergencyFund(p.getExistingEmergencyFund());
        dto.setMonthlyDebtObligations(p.getMonthlyDebtObligations());
        dto.setEmergencyFundTargetMonths(p.getEmergencyFundTargetMonths());
        dto.setManualEssentialExpenseOverride(p.getManualEssentialExpenseOverride());
        dto.setQuestionnaireCompleted(p.getQuestionnaireCompleted());
        dto.setUpdatedAt(p.getUpdatedAt());
        return dto;
    }
}
