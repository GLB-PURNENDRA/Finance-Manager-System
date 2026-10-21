package com.financemanager.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InvestorProfileDto {

    private Long id;
    private Long studentId;
    private String riskTolerance; // CONSERVATIVE, MODERATE, AGGRESSIVE
    private String investmentHorizon; // SHORT_TERM_UNDER_1Y, MEDIUM_TERM_1_3Y, LONG_TERM_3_5Y, VERY_LONG_TERM_5Y_PLUS
    private String primaryGoal; // EMERGENCY_RESERVE, EDUCATION, WEALTH_CREATION, TAX_SAVING, CAREER_CAPITAL
    private BigDecimal existingEmergencyFund;
    private BigDecimal monthlyDebtObligations;
    private Integer emergencyFundTargetMonths;
    private BigDecimal manualEssentialExpenseOverride;
    private Boolean questionnaireCompleted;
    private LocalDateTime updatedAt;

    public InvestorProfileDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public String getRiskTolerance() { return riskTolerance; }
    public void setRiskTolerance(String riskTolerance) { this.riskTolerance = riskTolerance; }

    public String getInvestmentHorizon() { return investmentHorizon; }
    public void setInvestmentHorizon(String investmentHorizon) { this.investmentHorizon = investmentHorizon; }

    public String getPrimaryGoal() { return primaryGoal; }
    public void setPrimaryGoal(String primaryGoal) { this.primaryGoal = primaryGoal; }

    public BigDecimal getExistingEmergencyFund() { return existingEmergencyFund; }
    public void setExistingEmergencyFund(BigDecimal existingEmergencyFund) { this.existingEmergencyFund = existingEmergencyFund; }

    public BigDecimal getMonthlyDebtObligations() { return monthlyDebtObligations; }
    public void setMonthlyDebtObligations(BigDecimal monthlyDebtObligations) { this.monthlyDebtObligations = monthlyDebtObligations; }

    public Integer getEmergencyFundTargetMonths() { return emergencyFundTargetMonths; }
    public void setEmergencyFundTargetMonths(Integer emergencyFundTargetMonths) { this.emergencyFundTargetMonths = emergencyFundTargetMonths; }

    public BigDecimal getManualEssentialExpenseOverride() { return manualEssentialExpenseOverride; }
    public void setManualEssentialExpenseOverride(BigDecimal manualEssentialExpenseOverride) { this.manualEssentialExpenseOverride = manualEssentialExpenseOverride; }

    public Boolean getQuestionnaireCompleted() { return questionnaireCompleted; }
    public void setQuestionnaireCompleted(Boolean questionnaireCompleted) { this.questionnaireCompleted = questionnaireCompleted; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
