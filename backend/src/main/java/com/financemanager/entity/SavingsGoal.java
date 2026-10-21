package com.financemanager.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "savings_goals")
public class SavingsGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_user_id", nullable = false)
    private User student;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal targetAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal savedAmount = BigDecimal.ZERO;

    private LocalDate targetDate;

    @Column(nullable = false, length = 20)
    private String status = "IN_PROGRESS"; // "IN_PROGRESS", "COMPLETED"

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public SavingsGoal() {}

    public SavingsGoal(User student, String name, BigDecimal targetAmount, BigDecimal savedAmount, LocalDate targetDate) {
        this.student = student;
        this.name = name;
        this.targetAmount = targetAmount;
        this.savedAmount = savedAmount != null ? savedAmount : BigDecimal.ZERO;
        this.targetDate = targetDate;
        this.status = "IN_PROGRESS";
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getTargetAmount() { return targetAmount; }
    public void setTargetAmount(BigDecimal targetAmount) { this.targetAmount = targetAmount; }

    public BigDecimal getSavedAmount() { return savedAmount; }
    public void setSavedAmount(BigDecimal savedAmount) { this.savedAmount = savedAmount; }

    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
