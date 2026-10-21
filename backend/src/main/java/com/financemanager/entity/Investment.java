package com.financemanager.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "investments")
public class Investment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_user_id", nullable = false)
    private User student;

    @Column(nullable = false, length = 50)
    private String assetType; // "STOCKS", "MUTUAL_FUNDS", "GOLD", "BONDS", "FIXED_DEPOSIT"

    @Column(nullable = false, length = 100)
    private String assetName;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal investedAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal currentValue;

    @Column(nullable = false)
    private LocalDateTime lastUpdated = LocalDateTime.now();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Investment() {}

    public Investment(User student, String assetType, String assetName, BigDecimal investedAmount, BigDecimal currentValue) {
        this.student = student;
        this.assetType = assetType;
        this.assetName = assetName;
        this.investedAmount = investedAmount;
        this.currentValue = currentValue;
        this.lastUpdated = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }

    public String getAssetType() { return assetType; }
    public void setAssetType(String assetType) { this.assetType = assetType; }

    public String getAssetName() { return assetName; }
    public void setAssetName(String assetName) { this.assetName = assetName; }

    public BigDecimal getInvestedAmount() { return investedAmount; }
    public void setInvestedAmount(BigDecimal investedAmount) { this.investedAmount = investedAmount; }

    public BigDecimal getCurrentValue() { return currentValue; }
    public void setCurrentValue(BigDecimal currentValue) { this.currentValue = currentValue; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
