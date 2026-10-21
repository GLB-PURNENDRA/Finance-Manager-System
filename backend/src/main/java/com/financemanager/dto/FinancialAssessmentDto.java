package com.financemanager.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class FinancialAssessmentDto {

    private BigDecimal averageMonthlyIncome = BigDecimal.ZERO;
    private BigDecimal averageMonthlyExpense = BigDecimal.ZERO;
    private BigDecimal averageMonthlyEssentialExpense = BigDecimal.ZERO;
    private BigDecimal averageMonthlyDiscretionaryExpense = BigDecimal.ZERO;
    private BigDecimal existingMonthlySipCommitments = BigDecimal.ZERO;
    private BigDecimal monthlySavingsGoalCommitments = BigDecimal.ZERO;
    private BigDecimal monthlyDebtObligations = BigDecimal.ZERO;
    private BigDecimal netMonthlyDiscretionarySurplus = BigDecimal.ZERO;

    // Emergency Fund Assessment
    private BigDecimal currentEmergencyReserve = BigDecimal.ZERO;
    private BigDecimal requiredEmergencyReserve = BigDecimal.ZERO;
    private double emergencyReserveMonthsCovered = 0.0;
    private boolean emergencyFundAdequate = false;

    // Affordable SIP Capacity Range
    private BigDecimal minRecommendedSip = BigDecimal.ZERO;
    private BigDecimal conservativeRecommendedSip = BigDecimal.ZERO;
    private BigDecimal recommendedSip = BigDecimal.ZERO;
    private BigDecimal maxPrudentSip = BigDecimal.ZERO;
    private String affordabilityStatus; // HEALTHY, TIGHT, DEFICIT, EMERGENCY_PRIORITY

    // Disclosures & Questions
    private List<String> assessmentObservations = new ArrayList<>();
    private List<String> missingDetails = new ArrayList<>();
    private boolean questionnaireCompleted = false;
    private String assessmentPeriod;
    private String regulatoryDisclaimer = "Educational research and financial planning computation. Mutual fund investments are subject to market risks; past performance does not guarantee future results.";

    public FinancialAssessmentDto() {}

    public BigDecimal getAverageMonthlyIncome() { return averageMonthlyIncome; }
    public void setAverageMonthlyIncome(BigDecimal averageMonthlyIncome) { this.averageMonthlyIncome = averageMonthlyIncome; }

    public BigDecimal getAverageMonthlyExpense() { return averageMonthlyExpense; }
    public void setAverageMonthlyExpense(BigDecimal averageMonthlyExpense) { this.averageMonthlyExpense = averageMonthlyExpense; }

    public BigDecimal getAverageMonthlyEssentialExpense() { return averageMonthlyEssentialExpense; }
    public void setAverageMonthlyEssentialExpense(BigDecimal averageMonthlyEssentialExpense) { this.averageMonthlyEssentialExpense = averageMonthlyEssentialExpense; }

    public BigDecimal getAverageMonthlyDiscretionaryExpense() { return averageMonthlyDiscretionaryExpense; }
    public void setAverageMonthlyDiscretionaryExpense(BigDecimal averageMonthlyDiscretionaryExpense) { this.averageMonthlyDiscretionaryExpense = averageMonthlyDiscretionaryExpense; }

    public BigDecimal getExistingMonthlySipCommitments() { return existingMonthlySipCommitments; }
    public void setExistingMonthlySipCommitments(BigDecimal existingMonthlySipCommitments) { this.existingMonthlySipCommitments = existingMonthlySipCommitments; }

    public BigDecimal getMonthlySavingsGoalCommitments() { return monthlySavingsGoalCommitments; }
    public void setMonthlySavingsGoalCommitments(BigDecimal monthlySavingsGoalCommitments) { this.monthlySavingsGoalCommitments = monthlySavingsGoalCommitments; }

    public BigDecimal getMonthlyDebtObligations() { return monthlyDebtObligations; }
    public void setMonthlyDebtObligations(BigDecimal monthlyDebtObligations) { this.monthlyDebtObligations = monthlyDebtObligations; }

    public BigDecimal getNetMonthlyDiscretionarySurplus() { return netMonthlyDiscretionarySurplus; }
    public void setNetMonthlyDiscretionarySurplus(BigDecimal netMonthlyDiscretionarySurplus) { this.netMonthlyDiscretionarySurplus = netMonthlyDiscretionarySurplus; }

    public BigDecimal getCurrentEmergencyReserve() { return currentEmergencyReserve; }
    public void setCurrentEmergencyReserve(BigDecimal currentEmergencyReserve) { this.currentEmergencyReserve = currentEmergencyReserve; }

    public BigDecimal getRequiredEmergencyReserve() { return requiredEmergencyReserve; }
    public void setRequiredEmergencyReserve(BigDecimal requiredEmergencyReserve) { this.requiredEmergencyReserve = requiredEmergencyReserve; }

    public double getEmergencyReserveMonthsCovered() { return emergencyReserveMonthsCovered; }
    public void setEmergencyReserveMonthsCovered(double emergencyReserveMonthsCovered) { this.emergencyReserveMonthsCovered = emergencyReserveMonthsCovered; }

    public boolean isEmergencyFundAdequate() { return emergencyFundAdequate; }
    public void setEmergencyFundAdequate(boolean emergencyFundAdequate) { this.emergencyFundAdequate = emergencyFundAdequate; }

    public BigDecimal getMinRecommendedSip() { return minRecommendedSip; }
    public void setMinRecommendedSip(BigDecimal minRecommendedSip) { this.minRecommendedSip = minRecommendedSip; }

    public BigDecimal getConservativeRecommendedSip() { return conservativeRecommendedSip; }
    public void setConservativeRecommendedSip(BigDecimal conservativeRecommendedSip) { this.conservativeRecommendedSip = conservativeRecommendedSip; }

    public BigDecimal getRecommendedSip() { return recommendedSip; }
    public void setRecommendedSip(BigDecimal recommendedSip) { this.recommendedSip = recommendedSip; }

    public BigDecimal getMaxPrudentSip() { return maxPrudentSip; }
    public void setMaxPrudentSip(BigDecimal maxPrudentSip) { this.maxPrudentSip = maxPrudentSip; }

    public String getAffordabilityStatus() { return affordabilityStatus; }
    public void setAffordabilityStatus(String affordabilityStatus) { this.affordabilityStatus = affordabilityStatus; }

    public List<String> getAssessmentObservations() { return assessmentObservations; }
    public void setAssessmentObservations(List<String> assessmentObservations) { this.assessmentObservations = assessmentObservations; }

    public List<String> getMissingDetails() { return missingDetails; }
    public void setMissingDetails(List<String> missingDetails) { this.missingDetails = missingDetails; }

    public boolean isQuestionnaireCompleted() { return questionnaireCompleted; }
    public void setQuestionnaireCompleted(boolean questionnaireCompleted) { this.questionnaireCompleted = questionnaireCompleted; }

    public String getAssessmentPeriod() { return assessmentPeriod; }
    public void setAssessmentPeriod(String assessmentPeriod) { this.assessmentPeriod = assessmentPeriod; }

    public String getRegulatoryDisclaimer() { return regulatoryDisclaimer; }
    public void setRegulatoryDisclaimer(String regulatoryDisclaimer) { this.regulatoryDisclaimer = regulatoryDisclaimer; }
}
