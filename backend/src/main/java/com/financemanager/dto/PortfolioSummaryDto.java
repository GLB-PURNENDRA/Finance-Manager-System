package com.financemanager.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class PortfolioSummaryDto {

    private BigDecimal totalInvested = BigDecimal.ZERO;
    private BigDecimal totalCurrentValue = BigDecimal.ZERO;
    private BigDecimal totalProfitLoss = BigDecimal.ZERO;
    private BigDecimal totalProfitLossPercentage = BigDecimal.ZERO;
    private BigDecimal todayGainLoss = BigDecimal.ZERO;
    private int assetCount = 0;
    private Map<String, BigDecimal> allocationByType; // e.g. "INDIAN_STOCKS" -> 45.2%
    private Map<String, BigDecimal> valueByType; // e.g. "INDIAN_STOCKS" -> ₹150,000
    private LocalDateTime lastRefreshedAt = LocalDateTime.now();
    private List<InvestmentDto> investments;

    public PortfolioSummaryDto() {}

    public BigDecimal getTotalInvested() { return totalInvested; }
    public void setTotalInvested(BigDecimal totalInvested) { this.totalInvested = totalInvested; }

    public BigDecimal getTotalCurrentValue() { return totalCurrentValue; }
    public void setTotalCurrentValue(BigDecimal totalCurrentValue) { this.totalCurrentValue = totalCurrentValue; }

    public BigDecimal getTotalProfitLoss() { return totalProfitLoss; }
    public void setTotalProfitLoss(BigDecimal totalProfitLoss) { this.totalProfitLoss = totalProfitLoss; }

    public BigDecimal getTotalProfitLossPercentage() { return totalProfitLossPercentage; }
    public void setTotalProfitLossPercentage(BigDecimal totalProfitLossPercentage) { this.totalProfitLossPercentage = totalProfitLossPercentage; }

    public BigDecimal getTodayGainLoss() { return todayGainLoss; }
    public void setTodayGainLoss(BigDecimal todayGainLoss) { this.todayGainLoss = todayGainLoss; }

    public int getAssetCount() { return assetCount; }
    public void setAssetCount(int assetCount) { this.assetCount = assetCount; }

    public Map<String, BigDecimal> getAllocationByType() { return allocationByType; }
    public void setAllocationByType(Map<String, BigDecimal> allocationByType) { this.allocationByType = allocationByType; }

    public Map<String, BigDecimal> getValueByType() { return valueByType; }
    public void setValueByType(Map<String, BigDecimal> valueByType) { this.valueByType = valueByType; }

    public LocalDateTime getLastRefreshedAt() { return lastRefreshedAt; }
    public void setLastRefreshedAt(LocalDateTime lastRefreshedAt) { this.lastRefreshedAt = lastRefreshedAt; }

    public List<InvestmentDto> getInvestments() { return investments; }
    public void setInvestments(List<InvestmentDto> investments) { this.investments = investments; }
}
