package com.financemanager.dto;

import java.math.BigDecimal;
import java.util.Map;

public class WhatIfResponseDto {

    private BigDecimal baselineIncome;
    private BigDecimal baselineExpense;
    private BigDecimal baselineNetCashFlow;

    private BigDecimal simulatedIncome;
    private BigDecimal simulatedExpense;
    private BigDecimal simulatedNetCashFlow;
    private BigDecimal projectedAnnualSavingsImpact;

    private Map<String, BigDecimal> categoryImpacts;
    private String explanation;
    private String disclaimer = "SIMULATION ONLY: No actual database records were modified.";

    public WhatIfResponseDto() {}

    public BigDecimal getBaselineIncome() { return baselineIncome; }
    public void setBaselineIncome(BigDecimal baselineIncome) { this.baselineIncome = baselineIncome; }

    public BigDecimal getBaselineExpense() { return baselineExpense; }
    public void setBaselineExpense(BigDecimal baselineExpense) { this.baselineExpense = baselineExpense; }

    public BigDecimal getBaselineNetCashFlow() { return baselineNetCashFlow; }
    public void setBaselineNetCashFlow(BigDecimal baselineNetCashFlow) { this.baselineNetCashFlow = baselineNetCashFlow; }

    public BigDecimal getSimulatedIncome() { return simulatedIncome; }
    public void setSimulatedIncome(BigDecimal simulatedIncome) { this.simulatedIncome = simulatedIncome; }

    public BigDecimal getSimulatedExpense() { return simulatedExpense; }
    public void setSimulatedExpense(BigDecimal simulatedExpense) { this.simulatedExpense = simulatedExpense; }

    public BigDecimal getSimulatedNetCashFlow() { return simulatedNetCashFlow; }
    public void setSimulatedNetCashFlow(BigDecimal simulatedNetCashFlow) { this.simulatedNetCashFlow = simulatedNetCashFlow; }

    public BigDecimal getProjectedAnnualSavingsImpact() { return projectedAnnualSavingsImpact; }
    public void setProjectedAnnualSavingsImpact(BigDecimal projectedAnnualSavingsImpact) { this.projectedAnnualSavingsImpact = projectedAnnualSavingsImpact; }

    public Map<String, BigDecimal> getCategoryImpacts() { return categoryImpacts; }
    public void setCategoryImpacts(Map<String, BigDecimal> categoryImpacts) { this.categoryImpacts = categoryImpacts; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public String getDisclaimer() { return disclaimer; }
    public void setDisclaimer(String disclaimer) { this.disclaimer = disclaimer; }
}
