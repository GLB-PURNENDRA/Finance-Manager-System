package com.financemanager.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MutualFundSchemeDto {

    private String schemeCode;
    private String isin;
    private String schemeName;
    private String fundHouse;
    private String category; // e.g. "Equity - Large Cap", "Equity - Flexi Cap", "Debt - Liquid Fund"
    private String broadCategory; // EQUITY, DEBT, HYBRID, SOLUTION_ORIENTED
    private String plan; // Direct Plan / Regular Plan
    private String option; // Growth Option / IDCW Option
    private BigDecimal nav;
    private LocalDate navDate;
    private BigDecimal return1Y; // Annualized CAGR %
    private BigDecimal return3Y; // 3-Year CAGR %
    private BigDecimal return5Y; // 5-Year CAGR %
    private BigDecimal volatility3Y; // Annualized std dev %
    private BigDecimal expenseRatioEstimated;
    private String benchmark;
    private String riskometer; // Low, Moderately Low, Moderate, Moderately High, High, Very High
    private String officialAmcUrl;
    private String investmentObjective;
    private String suitabilityMatchReason;
    private int matchScore; // 0 - 100 suitability rating
    private List<String> riskFactors = new ArrayList<>();
    private String dataProvider = "Association of Mutual Funds in India (AMFI) Official Daily NAV Feed";
    private boolean isStale = false;
    private String lastUpdatedTimestamp;

    public MutualFundSchemeDto() {}

    public String getSchemeCode() { return schemeCode; }
    public void setSchemeCode(String schemeCode) { this.schemeCode = schemeCode; }

    public String getIsin() { return isin; }
    public void setIsin(String isin) { this.isin = isin; }

    public String getSchemeName() { return schemeName; }
    public void setSchemeName(String schemeName) { this.schemeName = schemeName; }

    public String getFundHouse() { return fundHouse; }
    public void setFundHouse(String fundHouse) { this.fundHouse = fundHouse; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getBroadCategory() { return broadCategory; }
    public void setBroadCategory(String broadCategory) { this.broadCategory = broadCategory; }

    public String getPlan() { return plan; }
    public void setPlan(String plan) { this.plan = plan; }

    public String getOption() { return option; }
    public void setOption(String option) { this.option = option; }

    public BigDecimal getNav() { return nav; }
    public void setNav(BigDecimal nav) { this.nav = nav; }

    public LocalDate getNavDate() { return navDate; }
    public void setNavDate(LocalDate navDate) { this.navDate = navDate; }

    public BigDecimal getReturn1Y() { return return1Y; }
    public void setReturn1Y(BigDecimal return1Y) { this.return1Y = return1Y; }

    public BigDecimal getReturn3Y() { return return3Y; }
    public void setReturn3Y(BigDecimal return3Y) { this.return3Y = return3Y; }

    public BigDecimal getReturn5Y() { return return5Y; }
    public void setReturn5Y(BigDecimal return5Y) { this.return5Y = return5Y; }

    public BigDecimal getVolatility3Y() { return volatility3Y; }
    public void setVolatility3Y(BigDecimal volatility3Y) { this.volatility3Y = volatility3Y; }

    public BigDecimal getExpenseRatioEstimated() { return expenseRatioEstimated; }
    public void setExpenseRatioEstimated(BigDecimal expenseRatioEstimated) { this.expenseRatioEstimated = expenseRatioEstimated; }

    public String getBenchmark() { return benchmark; }
    public void setBenchmark(String benchmark) { this.benchmark = benchmark; }

    public String getRiskometer() { return riskometer; }
    public void setRiskometer(String riskometer) { this.riskometer = riskometer; }

    public String getOfficialAmcUrl() { return officialAmcUrl; }
    public void setOfficialAmcUrl(String officialAmcUrl) { this.officialAmcUrl = officialAmcUrl; }

    public String getInvestmentObjective() { return investmentObjective; }
    public void setInvestmentObjective(String investmentObjective) { this.investmentObjective = investmentObjective; }

    public String getSuitabilityMatchReason() { return suitabilityMatchReason; }
    public void setSuitabilityMatchReason(String suitabilityMatchReason) { this.suitabilityMatchReason = suitabilityMatchReason; }

    public int getMatchScore() { return matchScore; }
    public void setMatchScore(int matchScore) { this.matchScore = matchScore; }

    public List<String> getRiskFactors() { return riskFactors; }
    public void setRiskFactors(List<String> riskFactors) { this.riskFactors = riskFactors; }

    public String getDataProvider() { return dataProvider; }
    public void setDataProvider(String dataProvider) { this.dataProvider = dataProvider; }

    public boolean isStale() { return isStale; }
    public void setStale(boolean stale) { isStale = stale; }

    public String getLastUpdatedTimestamp() { return lastUpdatedTimestamp; }
    public void setLastUpdatedTimestamp(String lastUpdatedTimestamp) { this.lastUpdatedTimestamp = lastUpdatedTimestamp; }
}
