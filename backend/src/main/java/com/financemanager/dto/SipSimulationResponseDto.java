package com.financemanager.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class SipSimulationResponseDto {

    private BigDecimal monthlyInvestment;
    private Integer durationYears;
    private Integer totalMonths;
    private BigDecimal assumedAnnualReturnRate;
    private BigDecimal totalPrincipalInvested;
    private BigDecimal projectedMaturityValue;
    private BigDecimal estimatedWealthGain;
    private String schemeName;
    private List<YearlyMilestone> trajectory = new ArrayList<>();
    private String calculationType = "Deterministic Compound Monthly Annuity Model";
    private String disclaimer = "Hypothetical illustrative projection. Assumed returns are for educational visualization and not guaranteed predictions or promised returns. Equity mutual funds fluctuate with market cycles.";

    public static class YearlyMilestone {
        private int year;
        private BigDecimal principalInvested;
        private BigDecimal projectedValue;
        private BigDecimal wealthGain;

        public YearlyMilestone() {}

        public YearlyMilestone(int year, BigDecimal principalInvested, BigDecimal projectedValue, BigDecimal wealthGain) {
            this.year = year;
            this.principalInvested = principalInvested;
            this.projectedValue = projectedValue;
            this.wealthGain = wealthGain;
        }

        public int getYear() { return year; }
        public void setYear(int year) { this.year = year; }

        public BigDecimal getPrincipalInvested() { return principalInvested; }
        public void setPrincipalInvested(BigDecimal principalInvested) { this.principalInvested = principalInvested; }

        public BigDecimal getProjectedValue() { return projectedValue; }
        public void setProjectedValue(BigDecimal projectedValue) { this.projectedValue = projectedValue; }

        public BigDecimal getWealthGain() { return wealthGain; }
        public void setWealthGain(BigDecimal wealthGain) { this.wealthGain = wealthGain; }
    }

    public SipSimulationResponseDto() {}

    public BigDecimal getMonthlyInvestment() { return monthlyInvestment; }
    public void setMonthlyInvestment(BigDecimal monthlyInvestment) { this.monthlyInvestment = monthlyInvestment; }

    public Integer getDurationYears() { return durationYears; }
    public void setDurationYears(Integer durationYears) { this.durationYears = durationYears; }

    public Integer getTotalMonths() { return totalMonths; }
    public void setTotalMonths(Integer totalMonths) { this.totalMonths = totalMonths; }

    public BigDecimal getAssumedAnnualReturnRate() { return assumedAnnualReturnRate; }
    public void setAssumedAnnualReturnRate(BigDecimal assumedAnnualReturnRate) { this.assumedAnnualReturnRate = assumedAnnualReturnRate; }

    public BigDecimal getTotalPrincipalInvested() { return totalPrincipalInvested; }
    public void setTotalPrincipalInvested(BigDecimal totalPrincipalInvested) { this.totalPrincipalInvested = totalPrincipalInvested; }

    public BigDecimal getProjectedMaturityValue() { return projectedMaturityValue; }
    public void setProjectedMaturityValue(BigDecimal projectedMaturityValue) { this.projectedMaturityValue = projectedMaturityValue; }

    public BigDecimal getEstimatedWealthGain() { return estimatedWealthGain; }
    public void setEstimatedWealthGain(BigDecimal estimatedWealthGain) { this.estimatedWealthGain = estimatedWealthGain; }

    public String getSchemeName() { return schemeName; }
    public void setSchemeName(String schemeName) { this.schemeName = schemeName; }

    public List<YearlyMilestone> getTrajectory() { return trajectory; }
    public void setTrajectory(List<YearlyMilestone> trajectory) { this.trajectory = trajectory; }

    public String getCalculationType() { return calculationType; }
    public void setCalculationType(String calculationType) { this.calculationType = calculationType; }

    public String getDisclaimer() { return disclaimer; }
    public void setDisclaimer(String disclaimer) { this.disclaimer = disclaimer; }
}
