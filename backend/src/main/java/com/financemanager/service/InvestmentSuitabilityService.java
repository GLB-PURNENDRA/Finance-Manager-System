package com.financemanager.service;

import com.financemanager.dto.*;
import com.financemanager.entity.InvestorProfile;
import com.financemanager.entity.User;
import com.financemanager.repository.InvestorProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InvestmentSuitabilityService {

    @Autowired
    private AmfiNavService amfiNavService;

    @Autowired
    private InvestorProfileRepository investorProfileRepository;

    @Autowired
    private InvestmentAffordabilityService affordabilityService;

    public List<MutualFundSchemeDto> getRecommendedSchemes(User currentUser) {
        User student = affordabilityService.resolveEffectiveStudent(currentUser);
        Optional<InvestorProfile> profileOpt = investorProfileRepository.findByStudent(student);

        String riskTolerance = profileOpt.map(InvestorProfile::getRiskTolerance).orElse("MODERATE");
        String horizon = profileOpt.map(InvestorProfile::getInvestmentHorizon).orElse("LONG_TERM_3_5Y");
        String goal = profileOpt.map(InvestorProfile::getPrimaryGoal).orElse("WEALTH_CREATION");

        List<MutualFundSchemeDto> allSchemes = amfiNavService.getAllSchemes();
        List<MutualFundSchemeDto> shortlisted = new ArrayList<>();

        for (MutualFundSchemeDto scheme : allSchemes) {
            SuitabilityEvaluation eval = evaluateSuitability(scheme, riskTolerance, horizon, goal);
            if (eval.isEligible) {
                MutualFundSchemeDto copy = copyDto(scheme);
                copy.setMatchScore(eval.score);
                copy.setSuitabilityMatchReason(eval.reason);
                shortlisted.add(copy);
            }
        }

        // Sort by match score descending (Never by recent returns alone)
        shortlisted.sort((a, b) -> Integer.compare(b.getMatchScore(), a.getMatchScore()));

        // Return top 4 well-diversified recommendations
        return shortlisted.stream().limit(4).collect(Collectors.toList());
    }

    public SchemeComparisonResponseDto compareSchemes(List<String> schemeCodes) {
        SchemeComparisonResponseDto response = new SchemeComparisonResponseDto();
        response.setComparisonDate(LocalDate.now().toString());

        if (schemeCodes == null || schemeCodes.isEmpty()) {
            response.setCategoryVerdict("No schemes selected for comparison.");
            return response;
        }

        List<MutualFundSchemeDto> matched = new ArrayList<>();
        for (String code : schemeCodes) {
            amfiNavService.getScheme(code.trim()).ifPresent(matched::add);
        }
        response.setSchemes(matched);

        if (matched.size() < 2) {
            response.setCategoryVerdict("Select at least two schemes to view side-by-side risk, return, and expense comparisons.");
            return response;
        }

        List<String> diffs = new ArrayList<>();
        // Compare Expense Ratios
        MutualFundSchemeDto lowestExpense = matched.stream()
                .filter(s -> s.getExpenseRatioEstimated() != null)
                .min(Comparator.comparing(MutualFundSchemeDto::getExpenseRatioEstimated))
                .orElse(null);
        if (lowestExpense != null) {
            diffs.add(String.format("Lowest Expense Ratio: %s (%s%% p.a.) offers minimal cost drag compared to peers.",
                    lowestExpense.getSchemeName(), lowestExpense.getExpenseRatioEstimated()));
        }

        // Compare Volatility
        MutualFundSchemeDto lowestVol = matched.stream()
                .filter(s -> s.getVolatility3Y() != null)
                .min(Comparator.comparing(MutualFundSchemeDto::getVolatility3Y))
                .orElse(null);
        if (lowestVol != null) {
            diffs.add(String.format("Lowest 3-Year Volatility: %s (std dev: %s%%) indicates smoother historical ride during market corrections.",
                    lowestVol.getSchemeName(), lowestVol.getVolatility3Y()));
        }

        // Riskometer distribution
        Set<String> riskLevels = matched.stream().map(MutualFundSchemeDto::getRiskometer).collect(Collectors.toSet());
        if (riskLevels.size() > 1) {
            diffs.add(String.format("Riskometer Divergence: Selected funds span across different risk bands: %s.",
                    String.join(", ", riskLevels)));
        }

        response.setKeyDifferences(diffs);
        response.setCategoryVerdict(String.format(
                "Compared %d schemes across verified AMFI NAV data. Higher historical CAGR must be evaluated alongside 3-year volatility and expense ratios. Never pick solely on past 1-year returns.",
                matched.size()
        ));

        return response;
    }

    public SipSimulationResponseDto simulateSip(SipSimulationRequestDto request) {
        BigDecimal monthlyP = (request.getMonthlyInvestment() != null && request.getMonthlyInvestment().compareTo(BigDecimal.ZERO) > 0)
                ? request.getMonthlyInvestment()
                : new BigDecimal("1000.00");
        int years = (request.getDurationYears() != null && request.getDurationYears() > 0)
                ? request.getDurationYears()
                : 5;
        BigDecimal annualRate = (request.getExpectedAnnualReturnRate() != null && request.getExpectedAnnualReturnRate().compareTo(BigDecimal.ZERO) >= 0)
                ? request.getExpectedAnnualReturnRate()
                : new BigDecimal("12.00");

        SipSimulationResponseDto response = new SipSimulationResponseDto();
        response.setMonthlyInvestment(monthlyP);
        response.setDurationYears(years);
        response.setAssumedAnnualReturnRate(annualRate);

        int totalMonths = years * 12;
        response.setTotalMonths(totalMonths);

        BigDecimal totalPrincipal = monthlyP.multiply(BigDecimal.valueOf(totalMonths));
        response.setTotalPrincipalInvested(totalPrincipal);

        if (request.getSchemeCode() != null) {
            amfiNavService.getScheme(request.getSchemeCode()).ifPresent(s -> response.setSchemeName(s.getSchemeName()));
        }

        // Monthly rate i = (r / 100) / 12
        double r = annualRate.doubleValue() / 100.0;
        double i = r / 12.0;

        double fvDouble;
        if (i > 0) {
            // Future value of an annuity due: FV = P * [((1+i)^n - 1) / i] * (1+i)
            fvDouble = monthlyP.doubleValue() * ((Math.pow(1.0 + i, totalMonths) - 1.0) / i) * (1.0 + i);
        } else {
            fvDouble = totalPrincipal.doubleValue();
        }

        BigDecimal fv = BigDecimal.valueOf(fvDouble).setScale(2, RoundingMode.HALF_UP);
        response.setProjectedMaturityValue(fv);
        response.setEstimatedWealthGain(fv.subtract(totalPrincipal));

        // Generate yearly milestones
        List<SipSimulationResponseDto.YearlyMilestone> milestones = new ArrayList<>();
        for (int y = 1; y <= years; y++) {
            int m = y * 12;
            BigDecimal invested = monthlyP.multiply(BigDecimal.valueOf(m));
            double val;
            if (i > 0) {
                val = monthlyP.doubleValue() * ((Math.pow(1.0 + i, m) - 1.0) / i) * (1.0 + i);
            } else {
                val = invested.doubleValue();
            }
            BigDecimal proj = BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP);
            BigDecimal gain = proj.subtract(invested);
            milestones.add(new SipSimulationResponseDto.YearlyMilestone(y, invested, proj, gain));
        }
        response.setTrajectory(milestones);

        return response;
    }

    private SuitabilityEvaluation evaluateSuitability(MutualFundSchemeDto scheme, String riskTolerance, String horizon, String goal) {
        String cat = scheme.getCategory() != null ? scheme.getCategory().toUpperCase() : "";
        String broad = scheme.getBroadCategory() != null ? scheme.getBroadCategory() : "";

        int score = 70; // baseline
        StringBuilder reason = new StringBuilder();

        // 1. Goal Alignment
        if ("EMERGENCY_RESERVE".equalsIgnoreCase(goal)) {
            if ("DEBT".equalsIgnoreCase(broad)) {
                score += 25;
                reason.append("Directly matches your Emergency Reserve goal by focusing on capital safety and high liquidity. ");
            } else {
                return new SuitabilityEvaluation(false, 0, "Emergency reserve capital should never be put into market-linked equities.");
            }
        } else if ("TAX_SAVING".equalsIgnoreCase(goal)) {
            if (cat.contains("ELSS")) {
                score += 25;
                reason.append("Provides Section 80C tax deduction benefits with mandatory 3-year disciplined lock-in. ");
            }
        }

        // 2. Horizon Alignment
        if ("SHORT_TERM_UNDER_1Y".equalsIgnoreCase(horizon)) {
            if ("DEBT".equalsIgnoreCase(broad)) {
                score += 20;
                reason.append("Suitable for horizons under 1 year with negligible stock market exposure. ");
            } else {
                return new SuitabilityEvaluation(false, 0, "Equity funds require at least 3-5 years to weather cyclical market dips.");
            }
        } else if ("MEDIUM_TERM_1_3Y".equalsIgnoreCase(horizon)) {
            if ("HYBRID".equalsIgnoreCase(broad) || "DEBT".equalsIgnoreCase(broad)) {
                score += 15;
                reason.append("Balanced allocation manages downside risk for 1-3 year medium horizons. ");
            } else if (cat.contains("LARGE CAP")) {
                score += 5;
                reason.append("Bluechip stability provides lower volatility than mid/small cap segments. ");
            }
        } else { // 3-5Y or 5Y+
            if ("EQUITY".equalsIgnoreCase(broad)) {
                score += 15;
                reason.append("Long horizon allows compounding to smooth out short-term market volatility. ");
            }
        }

        // 3. Risk Tolerance Alignment
        if ("CONSERVATIVE".equalsIgnoreCase(riskTolerance)) {
            if ("DEBT".equalsIgnoreCase(broad) || "HYBRID".equalsIgnoreCase(broad)) {
                score += 15;
                reason.append("Low/Moderate risk profile matches your conservative preservation preference. ");
            } else if (cat.contains("INDEX") || cat.contains("LARGE CAP")) {
                score += 5;
                reason.append("Bluechip large-cap core presents disciplined risk profile. ");
            } else {
                return new SuitabilityEvaluation(false, 0, "Mid and Small-Cap volatility exceeds conservative risk tolerance.");
            }
        } else if ("AGGRESSIVE".equalsIgnoreCase(riskTolerance)) {
            if (cat.contains("MID CAP") || cat.contains("SMALL CAP") || cat.contains("FLEXI CAP")) {
                score += 15;
                reason.append("High growth potential matches your aggressive wealth creation tolerance. ");
            }
        } else { // MODERATE
            if (cat.contains("LARGE CAP") || cat.contains("INDEX") || cat.contains("FLEXI CAP") || "HYBRID".equalsIgnoreCase(broad)) {
                score += 15;
                reason.append("Diversified blend balances wealth generation with risk management. ");
            }
        }

        // Low expense bonus
        if (scheme.getExpenseRatioEstimated() != null && scheme.getExpenseRatioEstimated().compareTo(new BigDecimal("0.50")) <= 0) {
            score += 5;
            reason.append("Low direct expense ratio minimizes long-term compounding cost drag.");
        }

        return new SuitabilityEvaluation(true, Math.min(98, score), reason.toString().trim());
    }

    private MutualFundSchemeDto copyDto(MutualFundSchemeDto src) {
        MutualFundSchemeDto d = new MutualFundSchemeDto();
        d.setSchemeCode(src.getSchemeCode());
        d.setIsin(src.getIsin());
        d.setSchemeName(src.getSchemeName());
        d.setFundHouse(src.getFundHouse());
        d.setCategory(src.getCategory());
        d.setBroadCategory(src.getBroadCategory());
        d.setPlan(src.getPlan());
        d.setOption(src.getOption());
        d.setNav(src.getNav());
        d.setNavDate(src.getNavDate());
        d.setReturn1Y(src.getReturn1Y());
        d.setReturn3Y(src.getReturn3Y());
        d.setReturn5Y(src.getReturn5Y());
        d.setVolatility3Y(src.getVolatility3Y());
        d.setExpenseRatioEstimated(src.getExpenseRatioEstimated());
        d.setBenchmark(src.getBenchmark());
        d.setRiskometer(src.getRiskometer());
        d.setOfficialAmcUrl(src.getOfficialAmcUrl());
        d.setInvestmentObjective(src.getInvestmentObjective());
        d.setRiskFactors(new ArrayList<>(src.getRiskFactors()));
        d.setDataProvider(src.getDataProvider());
        d.setStale(src.isStale());
        d.setLastUpdatedTimestamp(src.getLastUpdatedTimestamp());
        return d;
    }

    private static class SuitabilityEvaluation {
        final boolean isEligible;
        final int score;
        final String reason;

        SuitabilityEvaluation(boolean isEligible, int score, String reason) {
            this.isEligible = isEligible;
            this.score = score;
            this.reason = reason;
        }
    }
}
