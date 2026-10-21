package com.financemanager.dto;

import com.financemanager.entity.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;

public class StatementRowDto {

    private int rowIndex;
    private LocalDate transactionDate;
    private TransactionType type;
    private BigDecimal amount;
    private BigDecimal debit;
    private BigDecimal credit;
    private BigDecimal balance;
    private String category;
    private String description;
    private String paymentMethod;
    private boolean duplicate;
    private String duplicateReason;
    private boolean selected = true;

    public StatementRowDto() {}

    public int getRowIndex() { return rowIndex; }
    public void setRowIndex(int rowIndex) { this.rowIndex = rowIndex; }

    public LocalDate getTransactionDate() { return transactionDate; }
    public void setTransactionDate(LocalDate transactionDate) { this.transactionDate = transactionDate; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getDebit() { return debit; }
    public void setDebit(BigDecimal debit) { this.debit = debit; }

    public BigDecimal getCredit() { return credit; }
    public void setCredit(BigDecimal credit) { this.credit = credit; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public boolean isDuplicate() { return duplicate; }
    public void setDuplicate(boolean duplicate) { this.duplicate = duplicate; }

    public String getDuplicateReason() { return duplicateReason; }
    public void setDuplicateReason(String duplicateReason) { this.duplicateReason = duplicateReason; }

    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }
}
