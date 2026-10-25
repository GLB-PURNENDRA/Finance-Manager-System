package com.financemanager.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
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
    private String assetType; // "INDIAN_STOCKS", "US_STOCKS", "ETFS", "MUTUAL_FUNDS", "GOLD", "CRYPTO", "BONDS", "FIXED_DEPOSIT"

    @Column(nullable = false, length = 150)
    private String assetName;

    @Column(length = 50)
    private String symbol; // e.g. "RELIANCE.NS", "AAPL", "120716", "BTC-INR", "GOLDBEES.NS"

    @Column
    private LocalDate investmentDate = LocalDate.now();

    @Column(precision = 18, scale = 6)
    private BigDecimal quantity = BigDecimal.ONE; // Units / Shares / Grams / Coins

    @Column(precision = 18, scale = 4)
    private BigDecimal purchasePrice = BigDecimal.ZERO; // Cost per unit

    @Column(precision = 12, scale = 2)
    private BigDecimal brokerageCharges = BigDecimal.ZERO; // Fees / STT / Brokerage

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal investedAmount; // Total capital invested

    @Column(precision = 18, scale = 4)
    private BigDecimal currentPrice; // Live market price per unit

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal currentValue; // Total current valuation (quantity * currentPrice)

    @Column(precision = 18, scale = 4)
    private BigDecimal dayChange = BigDecimal.ZERO; // Today's absolute price change

    @Column(precision = 8, scale = 2)
    private BigDecimal dayChangePercentage = BigDecimal.ZERO; // Today's % change

    @Column(length = 10)
    private String currency = "INR"; // "INR" or "USD"

    @Column(length = 500)
    private String notes;

    @Column
    private LocalDateTime priceLastUpdated;

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
        this.quantity = BigDecimal.ONE;
        this.purchasePrice = investedAmount;
        this.currentPrice = currentValue;
        this.currency = "INR";
        this.investmentDate = LocalDate.now();
        this.lastUpdated = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
    }

    public Investment(User student, String assetType, String assetName, String symbol, LocalDate investmentDate,
                      BigDecimal quantity, BigDecimal purchasePrice, BigDecimal brokerageCharges,
                      BigDecimal investedAmount, BigDecimal currentPrice, BigDecimal currentValue,
                      String currency, String notes) {
        this.student = student;
        this.assetType = assetType;
        this.assetName = assetName;
        this.symbol = symbol;
        this.investmentDate = investmentDate != null ? investmentDate : LocalDate.now();
        this.quantity = quantity != null ? quantity : BigDecimal.ONE;
        this.purchasePrice = purchasePrice != null ? purchasePrice : BigDecimal.ZERO;
        this.brokerageCharges = brokerageCharges != null ? brokerageCharges : BigDecimal.ZERO;
        this.investedAmount = investedAmount;
        this.currentPrice = currentPrice != null ? currentPrice : purchasePrice;
        this.currentValue = currentValue != null ? currentValue : investedAmount;
        this.currency = currency != null ? currency : "INR";
        this.notes = notes;
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

    public BigDecimal getDayChange() { return dayChange; }
    public void setDayChange(BigDecimal dayChange) { this.dayChange = dayChange; }

    public BigDecimal getDayChangePercentage() { return dayChangePercentage; }
    public void setDayChangePercentage(BigDecimal dayChangePercentage) { this.dayChangePercentage = dayChangePercentage; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getPriceLastUpdated() { return priceLastUpdated; }
    public void setPriceLastUpdated(LocalDateTime priceLastUpdated) { this.priceLastUpdated = priceLastUpdated; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
