package com.financemanager.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "sip_records")
public class SipRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_user_id", nullable = false)
    private User student;

    @Column(nullable = false, length = 100)
    private String fundName;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyAmount;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private Integer dueDayOfMonth = 5;

    @Column(nullable = false, length = 20)
    private String paymentStatus = "ACTIVE"; // "ACTIVE", "PAUSED", "COMPLETED"

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal assumedReturnRate = new BigDecimal("12.00");

    @Column(nullable = false)
    private Integer durationMonths = 36;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public SipRecord() {}

    public SipRecord(User student, String fundName, BigDecimal monthlyAmount, LocalDate startDate,
                     Integer dueDayOfMonth, BigDecimal assumedReturnRate, Integer durationMonths) {
        this.student = student;
        this.fundName = fundName;
        this.monthlyAmount = monthlyAmount;
        this.startDate = startDate;
        this.dueDayOfMonth = dueDayOfMonth != null ? dueDayOfMonth : 5;
        this.paymentStatus = "ACTIVE";
        this.assumedReturnRate = assumedReturnRate != null ? assumedReturnRate : new BigDecimal("12.00");
        this.durationMonths = durationMonths != null ? durationMonths : 36;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }

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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
