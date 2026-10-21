package com.financemanager.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class BudgetDto {

    private Long id;

    @NotBlank(message = "Category is required")
    private String category;

    @NotBlank(message = "Period month is required (YYYY-MM)")
    private String periodMonth;

    @NotNull(message = "Limit amount is required")
    @DecimalMin(value = "0.01", message = "Limit must be greater than 0")
    private BigDecimal limitAmount;

    private BigDecimal warningThreshold = new BigDecimal("80.00");
    private BigDecimal actualSpent = BigDecimal.ZERO;
    private BigDecimal remainingAmount = BigDecimal.ZERO;
    private BigDecimal percentageSpent = BigDecimal.ZERO;
    private boolean overBudget = false;
    private boolean warningTriggered = false;

    public BudgetDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getPeriodMonth() { return periodMonth; }
    public void setPeriodMonth(String periodMonth) { this.periodMonth = periodMonth; }

    public BigDecimal getLimitAmount() { return limitAmount; }
    public void setLimitAmount(BigDecimal limitAmount) { this.limitAmount = limitAmount; }

    public BigDecimal getWarningThreshold() { return warningThreshold; }
    public void setWarningThreshold(BigDecimal warningThreshold) { this.warningThreshold = warningThreshold; }

    public BigDecimal getActualSpent() { return actualSpent; }
    public void setActualSpent(BigDecimal actualSpent) { this.actualSpent = actualSpent; }

    public BigDecimal getRemainingAmount() { return remainingAmount; }
    public void setRemainingAmount(BigDecimal remainingAmount) { this.remainingAmount = remainingAmount; }

    public BigDecimal getPercentageSpent() { return percentageSpent; }
    public void setPercentageSpent(BigDecimal percentageSpent) { this.percentageSpent = percentageSpent; }

    public boolean isOverBudget() { return overBudget; }
    public void setOverBudget(boolean overBudget) { this.overBudget = overBudget; }

    public boolean isWarningTriggered() { return warningTriggered; }
    public void setWarningTriggered(boolean warningTriggered) { this.warningTriggered = warningTriggered; }
}
