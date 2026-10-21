package com.financemanager.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public class SavingsGoalDto {

    private Long id;

    @NotBlank(message = "Goal name is required")
    private String name;

    @NotNull(message = "Target amount is required")
    @DecimalMin(value = "0.01", message = "Target must be greater than 0")
    private BigDecimal targetAmount;

    private BigDecimal savedAmount = BigDecimal.ZERO;
    private BigDecimal remainingAmount = BigDecimal.ZERO;
    private BigDecimal progressPercentage = BigDecimal.ZERO;
    private LocalDate targetDate;
    private String status;

    public SavingsGoalDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getTargetAmount() { return targetAmount; }
    public void setTargetAmount(BigDecimal targetAmount) { this.targetAmount = targetAmount; }

    public BigDecimal getSavedAmount() { return savedAmount; }
    public void setSavedAmount(BigDecimal savedAmount) { this.savedAmount = savedAmount; }

    public BigDecimal getRemainingAmount() { return remainingAmount; }
    public void setRemainingAmount(BigDecimal remainingAmount) { this.remainingAmount = remainingAmount; }

    public BigDecimal getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(BigDecimal progressPercentage) { this.progressPercentage = progressPercentage; }

    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
