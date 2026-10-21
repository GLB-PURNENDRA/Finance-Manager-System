package com.financemanager.dto;

import java.math.BigDecimal;

public class SipSimulationRequestDto {

    private BigDecimal monthlyInvestment = new BigDecimal("1000.00");
    private Integer durationYears = 5;
    private BigDecimal expectedAnnualReturnRate = new BigDecimal("12.00");
    private String schemeCode; // Optional scheme reference

    public SipSimulationRequestDto() {}

    public SipSimulationRequestDto(BigDecimal monthlyInvestment, Integer durationYears, BigDecimal expectedAnnualReturnRate) {
        this.monthlyInvestment = monthlyInvestment;
        this.durationYears = durationYears;
        this.expectedAnnualReturnRate = expectedAnnualReturnRate;
    }

    public BigDecimal getMonthlyInvestment() { return monthlyInvestment; }
    public void setMonthlyInvestment(BigDecimal monthlyInvestment) { this.monthlyInvestment = monthlyInvestment; }

    public Integer getDurationYears() { return durationYears; }
    public void setDurationYears(Integer durationYears) { this.durationYears = durationYears; }

    public BigDecimal getExpectedAnnualReturnRate() { return expectedAnnualReturnRate; }
    public void setExpectedAnnualReturnRate(BigDecimal expectedAnnualReturnRate) { this.expectedAnnualReturnRate = expectedAnnualReturnRate; }

    public String getSchemeCode() { return schemeCode; }
    public void setSchemeCode(String schemeCode) { this.schemeCode = schemeCode; }
}
