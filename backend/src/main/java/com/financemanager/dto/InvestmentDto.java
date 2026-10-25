package com.financemanager.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class InvestmentDto {

    private Long id;

    @NotBlank(message = "Asset type is required")
    private String assetType; // "INDIAN_STOCKS", "US_STOCKS", "ETFS", "MUTUAL_FUNDS", "GOLD", "CRYPTO", "BONDS", "FIXED_DEPOSIT"

    @NotBlank(message = "Asset name is required")
    private String assetName;

    private String symbol; // Ticker or market identifier

    private LocalDate investmentDate;

    private BigDecimal quantity;

    private BigDecimal purchasePrice;

    private BigDecimal brokerageCharges = BigDecimal.ZERO;

    private BigDecimal investedAmount;

    private BigDecimal currentPrice;

    private BigDecimal currentValue;

    private BigDecimal profitLoss = BigDecimal.ZERO;
    private BigDecimal profitLossPercentage = BigDecimal.ZERO;

    private BigDecimal dayChange = BigDecimal.ZERO;
    private BigDecimal dayChangePercentage = BigDecimal.ZERO;
    private BigDecimal totalDayChange = BigDecimal.ZERO;

    private String currency = "INR";
    private String notes;
    private LocalDateTime priceLastUpdated;
    private LocalDateTime lastUpdated;

    public InvestmentDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAssetType() { return assetType; }
    public void setAssetType(String assetType) { this.assetType = assetType; }

    public String getAssetName() { return assetName; }
    public void setAssetName(String assetName) { this.assetName = assetName; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public LocalDate getInvestmentDate() { return investmentDate; }
    public void setInvestmentDate(LocalDate investmentDate) { this.investmentDate = investmentDate; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }

    public BigDecimal getBrokerageCharges() { return brokerageCharges; }
    public void setBrokerageCharges(BigDecimal brokerageCharges) { this.brokerageCharges = brokerageCharges; }

    public BigDecimal getInvestedAmount() { return investedAmount; }
    public void setInvestedAmount(BigDecimal investedAmount) { this.investedAmount = investedAmount; }

    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }

    public BigDecimal getCurrentValue() { return currentValue; }
    public void setCurrentValue(BigDecimal currentValue) { this.currentValue = currentValue; }

    public BigDecimal getProfitLoss() { return profitLoss; }
    public void setProfitLoss(BigDecimal profitLoss) { this.profitLoss = profitLoss; }

    public BigDecimal getProfitLossPercentage() { return profitLossPercentage; }
    public void setProfitLossPercentage(BigDecimal profitLossPercentage) { this.profitLossPercentage = profitLossPercentage; }

    public BigDecimal getDayChange() { return dayChange; }
    public void setDayChange(BigDecimal dayChange) { this.dayChange = dayChange; }

    public BigDecimal getDayChangePercentage() { return dayChangePercentage; }
    public void setDayChangePercentage(BigDecimal dayChangePercentage) { this.dayChangePercentage = dayChangePercentage; }

    public BigDecimal getTotalDayChange() { return totalDayChange; }
    public void setTotalDayChange(BigDecimal totalDayChange) { this.totalDayChange = totalDayChange; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getPriceLastUpdated() { return priceLastUpdated; }
    public void setPriceLastUpdated(LocalDateTime priceLastUpdated) { this.priceLastUpdated = priceLastUpdated; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }
}
