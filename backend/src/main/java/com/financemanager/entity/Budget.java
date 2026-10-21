package com.financemanager.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "budgets", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"student_user_id", "category", "period_month"})
})
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_user_id", nullable = false)
    private User student;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(name = "period_month", nullable = false, length = 7)
    private String periodMonth; // e.g. "2026-10"

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal limitAmount;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal warningThreshold = new BigDecimal("80.00");

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Budget() {}

    public Budget(User student, String category, String periodMonth, BigDecimal limitAmount, BigDecimal warningThreshold) {
        this.student = student;
        this.category = category;
        this.periodMonth = periodMonth;
        this.limitAmount = limitAmount;
        this.warningThreshold = warningThreshold != null ? warningThreshold : new BigDecimal("80.00");
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getPeriodMonth() { return periodMonth; }
    public void setPeriodMonth(String periodMonth) { this.periodMonth = periodMonth; }

    public BigDecimal getLimitAmount() { return limitAmount; }
    public void setLimitAmount(BigDecimal limitAmount) { this.limitAmount = limitAmount; }

    public BigDecimal getWarningThreshold() { return warningThreshold; }
    public void setWarningThreshold(BigDecimal warningThreshold) { this.warningThreshold = warningThreshold; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
