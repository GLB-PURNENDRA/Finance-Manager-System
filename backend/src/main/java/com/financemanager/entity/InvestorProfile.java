package com.financemanager.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "investor_profiles")
public class InvestorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false, unique = true)
    private User student;

    @Column(length = 30)
    private String riskTolerance = "MODERATE"; // CONSERVATIVE, MODERATE, AGGRESSIVE

    @Column(length = 30)
    private String investmentHorizon = "LONG_TERM_3_5Y"; // SHORT_TERM_UNDER_1Y, MEDIUM_TERM_1_3Y, LONG_TERM_3_5Y, VERY_LONG_TERM_5Y_PLUS

    @Column(length = 50)
    private String primaryGoal = "WEALTH_CREATION"; // EMERGENCY_RESERVE, EDUCATION, WEALTH_CREATION, TAX_SAVING, CAREER_CAPITAL

    @Column(precision = 12, scale = 2)
    private BigDecimal existingEmergencyFund = BigDecimal.ZERO;

    @Column(precision = 12, scale = 2)
    private BigDecimal monthlyDebtObligations = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer emergencyFundTargetMonths = 3;

    @Column(precision = 12, scale = 2)
    private BigDecimal manualEssentialExpenseOverride;

    @Column(nullable = false)
    private Boolean questionnaireCompleted = false;

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public InvestorProfile() {}

    public InvestorProfile(User student) {
        this.student = student;
        this.questionnaireCompleted = false;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }

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
