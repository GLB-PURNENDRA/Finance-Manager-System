package com.financemanager.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class DashboardSummaryDto {

    private BigDecimal totalIncome = BigDecimal.ZERO;
    private BigDecimal totalExpenses = BigDecimal.ZERO;
    private BigDecimal netCashFlow = BigDecimal.ZERO;
    private BigDecimal trackedBalance = BigDecimal.ZERO;

    private BigDecimal totalInvested = BigDecimal.ZERO;
    private BigDecimal totalCurrentPortfolioValue = BigDecimal.ZERO;
    private BigDecimal totalPortfolioProfitLoss = BigDecimal.ZERO;

    private int activeBudgetsCount = 0;
    private int activeGoalsCount = 0;

    private Map<String, BigDecimal> categoryExpenses;
    private List<Map<String, Object>> monthlyTrends;
    private List<TransactionDto> recentTransactions;

    private String userRole;
    private boolean isReadOnly = false;
    private Long studentId;
    private String studentName;
    private String linkStatus;
    private List<FamilyLinkDto> availableStudents;

    public DashboardSummaryDto() {}

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public List<FamilyLinkDto> getAvailableStudents() { return availableStudents; }
    public void setAvailableStudents(List<FamilyLinkDto> availableStudents) { this.availableStudents = availableStudents; }

    public BigDecimal getTotalIncome() { return totalIncome; }
    public void setTotalIncome(BigDecimal totalIncome) { this.totalIncome = totalIncome; }

    public BigDecimal getTotalExpenses() { return totalExpenses; }
    public void setTotalExpenses(BigDecimal totalExpenses) { this.totalExpenses = totalExpenses; }

    public BigDecimal getNetCashFlow() { return netCashFlow; }
    public void setNetCashFlow(BigDecimal netCashFlow) { this.netCashFlow = netCashFlow; }

    public BigDecimal getTrackedBalance() { return trackedBalance; }
    public void setTrackedBalance(BigDecimal trackedBalance) { this.trackedBalance = trackedBalance; }

    public BigDecimal getTotalInvested() { return totalInvested; }
    public void setTotalInvested(BigDecimal totalInvested) { this.totalInvested = totalInvested; }

    public BigDecimal getTotalCurrentPortfolioValue() { return totalCurrentPortfolioValue; }
    public void setTotalCurrentPortfolioValue(BigDecimal totalCurrentPortfolioValue) { this.totalCurrentPortfolioValue = totalCurrentPortfolioValue; }

    public BigDecimal getTotalPortfolioProfitLoss() { return totalPortfolioProfitLoss; }
    public void setTotalPortfolioProfitLoss(BigDecimal totalPortfolioProfitLoss) { this.totalPortfolioProfitLoss = totalPortfolioProfitLoss; }

    public int getActiveBudgetsCount() { return activeBudgetsCount; }
    public void setActiveBudgetsCount(int activeBudgetsCount) { this.activeBudgetsCount = activeBudgetsCount; }

    public int getActiveGoalsCount() { return activeGoalsCount; }
    public void setActiveGoalsCount(int activeGoalsCount) { this.activeGoalsCount = activeGoalsCount; }

    public Map<String, BigDecimal> getCategoryExpenses() { return categoryExpenses; }
    public void setCategoryExpenses(Map<String, BigDecimal> categoryExpenses) { this.categoryExpenses = categoryExpenses; }

    public List<Map<String, Object>> getMonthlyTrends() { return monthlyTrends; }
    public void setMonthlyTrends(List<Map<String, Object>> monthlyTrends) { this.monthlyTrends = monthlyTrends; }

    public List<TransactionDto> getRecentTransactions() { return recentTransactions; }
    public void setRecentTransactions(List<TransactionDto> recentTransactions) { this.recentTransactions = recentTransactions; }

    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }

    public boolean isReadOnly() { return isReadOnly; }
    public void setReadOnly(boolean readOnly) { isReadOnly = readOnly; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getLinkStatus() { return linkStatus; }
    public void setLinkStatus(String linkStatus) { this.linkStatus = linkStatus; }
}
