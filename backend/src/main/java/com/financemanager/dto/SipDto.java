package com.financemanager.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public class SipDto {

    private Long id;

    @NotBlank(message = "Fund name is required")
    private String fundName;

    @NotNull(message = "Monthly contribution is required")
    @DecimalMin(value = "100.00", message = "Monthly contribution must be at least 100")
    private BigDecimal monthlyAmount;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @Min(value = 1, message = "Due day must be between 1 and 31")
    @Max(value = 31, message = "Due day must be between 1 and 31")
    private Integer dueDayOfMonth = 5;

    private String paymentStatus;

    @DecimalMin(value = "0.00", message = "Assumed return rate cannot be negative")
    private BigDecimal assumedReturnRate = new BigDecimal("12.00");

    @Min(value = 1, message = "Duration must be at least 1 month")
    private Integer durationMonths = 36;

    private BigDecimal totalInvested = BigDecimal.ZERO;
    private BigDecimal estimatedFutureValue = BigDecimal.ZERO;
    private BigDecimal estimatedWealthGain = BigDecimal.ZERO;

    public SipDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFundName() { return fundName; }
    public void setFundName(String fundName) { this.fundName = fundName; }

    public BigDecimal getMonthlyAmount() { return monthlyAmount; }
    public void setMonthlyAmount(BigDecimal monthlyAmount) { this.monthlyAmount = monthlyAmount; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public Integer getDueDayOfMonth() { return dueDayOfMonth; }
    public void setDueDayOfMonth(Integer dueDayOfMonth) { this.dueDayOfMonth = dueDayOfMonth; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public BigDecimal getAssumedReturnRate() { return assumedReturnRate; }
    public void setAssumedReturnRate(BigDecimal assumedReturnRate) { this.assumedReturnRate = assumedReturnRate; }

    public Integer getDurationMonths() { return durationMonths; }
    public void setDurationMonths(Integer durationMonths) { this.durationMonths = durationMonths; }

    public BigDecimal getTotalInvested() { return totalInvested; }
    public void setTotalInvested(BigDecimal totalInvested) { this.totalInvested = totalInvested; }

    public BigDecimal getEstimatedFutureValue() { return estimatedFutureValue; }
    public void setEstimatedFutureValue(BigDecimal estimatedFutureValue) { this.estimatedFutureValue = estimatedFutureValue; }

    public BigDecimal getEstimatedWealthGain() { return estimatedWealthGain; }
    public void setEstimatedWealthGain(BigDecimal estimatedWealthGain) { this.estimatedWealthGain = estimatedWealthGain; }
}
