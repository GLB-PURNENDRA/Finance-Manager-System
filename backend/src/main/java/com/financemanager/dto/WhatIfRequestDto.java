package com.financemanager.dto;

import java.math.BigDecimal;
import java.util.Map;

public class WhatIfRequestDto {

    private BigDecimal monthlyIncomeAdjustment = BigDecimal.ZERO;
    private Map<String, BigDecimal> categoryExpenseAdjustments;
    private BigDecimal monthlySipAdjustment = BigDecimal.ZERO;

    public WhatIfRequestDto() {}

    public BigDecimal getMonthlyIncomeAdjustment() { return monthlyIncomeAdjustment; }
    public void setMonthlyIncomeAdjustment(BigDecimal monthlyIncomeAdjustment) { this.monthlyIncomeAdjustment = monthlyIncomeAdjustment; }

    public Map<String, BigDecimal> getCategoryExpenseAdjustments() { return categoryExpenseAdjustments; }
    public void setCategoryExpenseAdjustments(Map<String, BigDecimal> categoryExpenseAdjustments) { this.categoryExpenseAdjustments = categoryExpenseAdjustments; }

    public BigDecimal getMonthlySipAdjustment() { return monthlySipAdjustment; }
    public void setMonthlySipAdjustment(BigDecimal monthlySipAdjustment) { this.monthlySipAdjustment = monthlySipAdjustment; }
}
