package com.financemanager.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InvestmentDto {

    private Long id;

    @NotBlank(message = "Asset type is required (STOCKS, MUTUAL_FUNDS, GOLD, BONDS, FIXED_DEPOSIT)")
    private String assetType;

    @NotBlank(message = "Asset name is required")
    private String assetName;

    @NotNull(message = "Invested amount is required")
    @DecimalMin(value = "0.01", message = "Invested amount must be greater than 0")
    private BigDecimal investedAmount;

    @NotNull(message = "Current value is required")
    @DecimalMin(value = "0.00", message = "Current value cannot be negative")
    private BigDecimal currentValue;

    private BigDecimal profitLoss = BigDecimal.ZERO;
    private BigDecimal profitLossPercentage = BigDecimal.ZERO;
    private LocalDateTime lastUpdated;

    public InvestmentDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAssetType() { return assetType; }
    public void setAssetType(String assetType) { this.assetType = assetType; }

    public String getAssetName() { return assetName; }
    public void setAssetName(String assetName) { this.assetName = assetName; }

    public BigDecimal getInvestedAmount() { return investedAmount; }
    public void setInvestedAmount(BigDecimal investedAmount) { this.investedAmount = investedAmount; }

    public BigDecimal getCurrentValue() { return currentValue; }
    public void setCurrentValue(BigDecimal currentValue) { this.currentValue = currentValue; }

    public BigDecimal getProfitLoss() { return profitLoss; }
    public void setProfitLoss(BigDecimal profitLoss) { this.profitLoss = profitLoss; }

    public BigDecimal getProfitLossPercentage() { return profitLossPercentage; }
    public void setProfitLossPercentage(BigDecimal profitLossPercentage) { this.profitLossPercentage = profitLossPercentage; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}
