package com.financemanager;

import com.financemanager.dto.*;
import com.financemanager.entity.*;
import com.financemanager.exception.ForbiddenException;
import com.financemanager.repository.*;
import com.financemanager.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class InvestmentResearchTests {

    @Autowired
    private AmfiNavService amfiNavService;

    @Autowired
    private InvestmentAffordabilityService affordabilityService;

    @Autowired
    private InvestmentSuitabilityService suitabilityService;

    @Autowired
    private IntelligenceService intelligenceService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private SipRecordRepository sipRecordRepository;

    @Autowired
    private FamilyLinkRepository familyLinkRepository;

    @Autowired
    private InvestorProfileRepository investorProfileRepository;

    private User student;
    private User parent;
    private FamilyLink link;

    @BeforeEach
    public void setup() {
        student = userRepository.findByEmail("student@university.edu").orElseThrow();
        parent = userRepository.findByEmail("parent@family.com").orElseThrow();

        investorProfileRepository.findByStudent(student).ifPresent(investorProfileRepository::delete);

        link = familyLinkRepository.findByParentAndStudent(parent, student)
                .orElseGet(() -> familyLinkRepository.save(new FamilyLink(parent, student, LinkStatus.ACTIVE, "PARENT")));
        link.setStatus(LinkStatus.ACTIVE);
        familyLinkRepository.save(link);
    }

    @Test
    public void testAmfiServiceInitializationAndCatalog() {
        List<MutualFundSchemeDto> schemes = amfiNavService.getAllSchemes();
        assertNotNull(schemes, "Scheme catalog should not be null");
        assertTrue(schemes.size() >= 10, "Should have at least 10 curated schemes");

        // Verify Parag Parikh Flexi Cap exists
        MutualFundSchemeDto ppfas = amfiNavService.getScheme("122639").orElse(null);
        assertNotNull(ppfas, "Parag Parikh Flexi Cap Fund (122639) must exist");
        assertNotNull(ppfas.getNav(), "NAV should be populated");
        assertNotNull(ppfas.getNavDate(), "NAV Date should be populated");
        assertNotNull(ppfas.getExpenseRatioEstimated(), "Expense ratio must be defined");
        assertEquals("EQUITY", ppfas.getBroadCategory());

        // Verify Liquid Fund exists for capital safety
        MutualFundSchemeDto liquid = amfiNavService.getScheme("119797").orElse(null);
        assertNotNull(liquid, "HDFC Liquid Fund (119797) must exist");
        assertEquals("DEBT", liquid.getBroadCategory());
        assertEquals("Low to Moderate", liquid.getRiskometer());
    }

    @Test
    public void testAffordabilityAssessmentCalculation() {
        FinancialAssessmentDto assess = affordabilityService.calculateAffordability(student, 6);
        assertNotNull(assess);

        // Student has seeded income (15000) and expenses
        assertTrue(assess.getAverageMonthlyIncome().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(assess.getAverageMonthlyEssentialExpense().compareTo(BigDecimal.ZERO) > 0);

        // Before completing questionnaire, missing details are flagged
        assertFalse(assess.isQuestionnaireCompleted());
        assertFalse(assess.getMissingDetails().isEmpty(), "Should prompt user for missing details");

        // Recommended SIP capacity should be calculated and non-negative
        assertNotNull(assess.getConservativeRecommendedSip());
        assertNotNull(assess.getRecommendedSip());
        assertNotNull(assess.getAffordabilityStatus());
    }

    @Test
    public void testEmergencyFundPriorityWarning() {
        // Create an investor profile with 0 emergency savings and 5000 essential expenses (leaving surplus)
        InvestorProfileDto prof = new InvestorProfileDto();
        prof.setRiskTolerance("AGGRESSIVE");
        prof.setInvestmentHorizon("VERY_LONG_TERM_5Y_PLUS");
        prof.setPrimaryGoal("WEALTH_CREATION");
        prof.setExistingEmergencyFund(BigDecimal.ZERO); // Zero emergency fund
        prof.setEmergencyFundTargetMonths(3);
        prof.setManualEssentialExpenseOverride(new BigDecimal("5000.00"));
        affordabilityService.saveProfile(student, prof);

        FinancialAssessmentDto assess = affordabilityService.calculateAffordability(student, 6);
        assertTrue(assess.isQuestionnaireCompleted());
        assertFalse(assess.isEmergencyFundAdequate());
        assertEquals("EMERGENCY_PRIORITY", assess.getAffordabilityStatus());

        // Observations must include safety rule to build emergency reserve
        boolean hasEmergencyWarning = assess.getAssessmentObservations().stream()
                .anyMatch(o -> o.contains("emergency reserve") || o.contains("Liquid Fund"));
        assertTrue(hasEmergencyWarning, "Must advise prioritizing emergency reserve");
    }

    @Test
    public void testSuitabilityMatchingForDifferentRiskProfiles() {
        // 1. Conservative Investor Profile
        InvestorProfileDto consProf = new InvestorProfileDto();
        consProf.setRiskTolerance("CONSERVATIVE");
        consProf.setInvestmentHorizon("SHORT_TERM_UNDER_1Y");
        consProf.setPrimaryGoal("EMERGENCY_RESERVE");
        affordabilityService.saveProfile(student, consProf);

        List<MutualFundSchemeDto> consRecs = suitabilityService.getRecommendedSchemes(student);
        assertFalse(consRecs.isEmpty());
        // Must recommend Debt/Liquid funds for emergency/conservative profile
        for (MutualFundSchemeDto s : consRecs) {
            assertEquals("DEBT", s.getBroadCategory(), "Conservative short-term profile must recommend capital-safe DEBT/Liquid funds");
        }

        // 2. Aggressive Investor Profile
        InvestorProfileDto aggProf = new InvestorProfileDto();
        aggProf.setRiskTolerance("AGGRESSIVE");
        aggProf.setInvestmentHorizon("VERY_LONG_TERM_5Y_PLUS");
        aggProf.setPrimaryGoal("WEALTH_CREATION");
        affordabilityService.saveProfile(student, aggProf);

        List<MutualFundSchemeDto> aggRecs = suitabilityService.getRecommendedSchemes(student);
        assertFalse(aggRecs.isEmpty());
        boolean hasEquity = aggRecs.stream().anyMatch(s -> "EQUITY".equals(s.getBroadCategory()));
        assertTrue(hasEquity, "Aggressive long-term profile must recommend wealth-creation equity funds");
    }

    @Test
    public void testDeterministicSipSimulation() {
        SipSimulationRequestDto req = new SipSimulationRequestDto(new BigDecimal("2000.00"), 5, new BigDecimal("12.00"));
        SipSimulationResponseDto sim = suitabilityService.simulateSip(req);

        assertEquals(new BigDecimal("2000.00"), sim.getMonthlyInvestment());
        assertEquals(5, sim.getDurationYears());
        assertEquals(60, sim.getTotalMonths());

        // Principal: 2000 * 60 = 120,000
        assertEquals(new BigDecimal("120000.00"), sim.getTotalPrincipalInvested());

        // Future Value for 2000/mo at 12% over 5 years is ~164,972.74
        assertTrue(sim.getProjectedMaturityValue().compareTo(new BigDecimal("160000.00")) > 0);
        assertTrue(sim.getEstimatedWealthGain().compareTo(new BigDecimal("40000.00")) > 0);
        assertEquals(5, sim.getTrajectory().size(), "Should produce 5 yearly milestones");
        assertNotNull(sim.getDisclaimer(), "Must include hypothetical disclaimer");
    }

    @Test
    public void testSchemeComparisonEngine() {
        SchemeComparisonResponseDto comp = suitabilityService.compareSchemes(List.of("120716", "122639", "120828"));
        assertEquals(3, comp.getSchemes().size());
        assertFalse(comp.getKeyDifferences().isEmpty());
        assertNotNull(comp.getCategoryVerdict());
        assertNotNull(comp.getDisclaimer());
    }

    @Test
    public void testConversationalAiIntegrationInvestmentQueries() {
        FinancialContextService.FinancialContext ctx = new FinancialContextService.FinancialContext();
        ctx.student = student;
        ctx.viewerRole = Role.STUDENT;
        ctx.currentYm = java.time.YearMonth.now();
        ctx.previousYm = ctx.currentYm.minusMonths(1);
        ctx.currentMonthIncome = new BigDecimal("15000.00");
        ctx.currentMonthExpense = new BigDecimal("10000.00");
        ctx.currentMonthNet = new BigDecimal("5000.00");
        ctx.previousMonthIncome = new BigDecimal("15000.00");
        ctx.previousMonthExpense = new BigDecimal("9500.00");
        ctx.previousMonthNet = new BigDecimal("5500.00");
        ctx.topCategory = "Hostel & Rent";
        ctx.topCategoryAmount = new BigDecimal("6500.00");

        // 1. Hindi Affordability Question
        String q1 = "Meri income aur expenses ke hisaab se kitni SIP affordable hai?";
        String r1 = intelligenceService.generateDeterministicConversationalResponse(ctx, List.of(), q1, student);
        assertTrue(r1.contains("Aapki Monthly SIP Affordability"), "Should trigger Hindi affordability assessment");
        assertTrue(r1.contains("Recommended Affordable SIP Range") || r1.contains("Sifarish"), "Should provide SIP range");

        // 2. Fund Comparison Question
        String q2 = "Compare these funds using current available data.";
        String r2 = intelligenceService.generateDeterministicConversationalResponse(ctx, List.of(), q2, student);
        assertTrue(r2.contains("Verified Scheme Comparison"), "Should return verified comparison table");
        assertTrue(r2.contains("Latest NAV"), "Should include NAV columns");

        // 3. Suitability Question
        String q3 = "Why does this option match my risk profile?";
        String r3 = intelligenceService.generateDeterministicConversationalResponse(ctx, List.of(), q3, student);
        assertTrue(r3.contains("Suitability Matching Analysis"), "Should explain suitability match");

        // 4. Compounding Simulation Question
        String q4 = "What if I invest ₹1,000 or ₹2,000 monthly?";
        String r4 = intelligenceService.generateDeterministicConversationalResponse(ctx, List.of(), q4, student);
        assertTrue(r4.contains("SIP Compounding Scenario Comparison"), "Should simulate both 1000 and 2000");
        assertTrue(r4.contains("₹1,000/month") && r4.contains("₹2,000/month"));

        // 5. Data Source Question
        String q5 = "Show the data date and sources.";
        String r5 = intelligenceService.generateDeterministicConversationalResponse(ctx, List.of(), q5, student);
        assertTrue(r5.contains("Association of Mutual Funds in India (AMFI)"), "Must cite AMFI official provider");
        assertTrue(r5.contains("End-of-Day"), "Must clarify delayed EOD pricing model");

        // Distinctness assertion across all 5
        long distinct = List.of(r1, r2, r3, r4, r5).stream().distinct().count();
        assertEquals(5, distinct, "All 5 conversational answers must be distinct and non-generic");
    }

    @Test
    public void testParentPermissionsAndSecurity() {
        // Parent viewing student's affordability assessment is permitted (Read-Only)
        FinancialAssessmentDto parentView = affordabilityService.calculateAffordability(parent, 6);
        assertNotNull(parentView);
        assertTrue(parentView.getAverageMonthlyIncome().compareTo(BigDecimal.ZERO) > 0);

        // Parent attempting to mutate student's profile must throw 403 Forbidden
        InvestorProfileDto mutateDto = new InvestorProfileDto();
        mutateDto.setRiskTolerance("AGGRESSIVE");
        assertThrows(ForbiddenException.class, () -> affordabilityService.saveProfile(parent, mutateDto),
                "Parents must be blocked with 403 Forbidden from modifying student profile");

        // When link is revoked, parent cannot view student assessment
        link.setStatus(LinkStatus.REVOKED);
        familyLinkRepository.save(link);

        assertThrows(ForbiddenException.class, () -> affordabilityService.calculateAffordability(parent, 6),
                "Revoked link must block parent with 403 Forbidden");
    }
}
