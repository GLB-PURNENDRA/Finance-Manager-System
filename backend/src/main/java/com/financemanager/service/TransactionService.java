package com.financemanager.service;

import com.financemanager.dto.TransactionDto;
import com.financemanager.entity.Role;
import com.financemanager.entity.Transaction;
import com.financemanager.entity.TransactionType;
import com.financemanager.entity.User;
import com.financemanager.exception.ForbiddenException;
import com.financemanager.exception.ResourceNotFoundException;
import com.financemanager.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private FamilyLinkService familyLinkService;

    @Autowired
    private AuditService auditService;

    public List<TransactionDto> getTransactions(User currentUser, LocalDate startDate, LocalDate endDate,
                                                String category, TransactionType type) {
        return getTransactions(currentUser, startDate, endDate, category, type, null);
    }

    public List<TransactionDto> getTransactions(User currentUser, LocalDate startDate, LocalDate endDate,
                                                String category, TransactionType type, Long studentId) {
        User targetStudent;
        TransactionType queryType = type;

        if (currentUser.getRole() == Role.PARENT) {
            // Parent can only view actively linked student's permitted EXPENSES
            targetStudent = familyLinkService.getActiveStudentForParent(currentUser, studentId);
            queryType = TransactionType.EXPENSE; // Strictly expenses only for parent
        } else {
            targetStudent = currentUser;
        }

        return transactionRepository.findFiltered(targetStudent, startDate, endDate, category, queryType)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public TransactionDto getTransactionById(User currentUser, Long id) {
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found."));

        if (currentUser.getRole() == Role.PARENT) {
            List<com.financemanager.dto.FamilyLinkDto> activeLinks = familyLinkService.getActiveLinksForParent(currentUser);
            boolean owns = activeLinks.stream().anyMatch(l -> l.getStudentId().equals(tx.getOwner().getId()));
            if (!owns || tx.getType() != TransactionType.EXPENSE) {
                throw new ForbiddenException("Unauthorized to view this transaction.");
            }
        } else {
            if (!tx.getOwner().getId().equals(currentUser.getId())) {
                throw new ForbiddenException("Unauthorized to view this transaction.");
            }
        }

        return toDto(tx);
    }

    public TransactionDto createTransaction(User currentUser, TransactionDto dto) {
        if (currentUser.getRole() == Role.PARENT) {
            throw new ForbiddenException("Parents have strictly read-only access and cannot alter student records.");
        }

        java.math.BigDecimal normAmount = dto.getAmount() != null
                ? dto.getAmount().setScale(2, java.math.RoundingMode.HALF_UP)
                : java.math.BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP);
        String normCategory = dto.getCategory() != null ? dto.getCategory().trim() : "Other";
        String normDescription = dto.getDescription() != null ? dto.getDescription().trim() : "Transaction";

        Transaction tx = new Transaction(
                currentUser,
                dto.getType(),
                normAmount,
                normCategory,
                normDescription,
                dto.getTransactionDate(),
                dto.getPaymentMethod(),
                dto.getSource() != null ? dto.getSource() : "MANUAL"
        );

        tx = transactionRepository.save(tx);
        auditService.log(currentUser, "TRANSACTION_CREATED", "TRANSACTION", tx.getId().toString(),
                "Type: " + tx.getType() + ", Amount: " + tx.getAmount());

        return toDto(tx);
    }

    public TransactionDto updateTransaction(User currentUser, Long id, TransactionDto dto) {
        if (currentUser.getRole() == Role.PARENT) {
            throw new ForbiddenException("Parents have strictly read-only access and cannot alter student records.");
        }

        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found."));

        if (!tx.getOwner().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You can only modify your own transactions.");
        }

        java.math.BigDecimal normAmount = dto.getAmount() != null
                ? dto.getAmount().setScale(2, java.math.RoundingMode.HALF_UP)
                : java.math.BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP);
        String normCategory = dto.getCategory() != null ? dto.getCategory().trim() : "Other";
        String normDescription = dto.getDescription() != null ? dto.getDescription().trim() : "Transaction";

        tx.setType(dto.getType());
        tx.setAmount(normAmount);
        tx.setCategory(normCategory);
        tx.setDescription(normDescription);
        tx.setTransactionDate(dto.getTransactionDate());
        tx.setPaymentMethod(dto.getPaymentMethod());

        tx = transactionRepository.save(tx);
        auditService.log(currentUser, "TRANSACTION_UPDATED", "TRANSACTION", tx.getId().toString(),
                "Amount: " + tx.getAmount());

        return toDto(tx);
    }

    public void deleteTransaction(User currentUser, Long id) {
        if (currentUser.getRole() == Role.PARENT) {
            throw new ForbiddenException("Parents have strictly read-only access and cannot alter student records.");
        }

        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found."));

        if (!tx.getOwner().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You can only delete your own transactions.");
        }

        transactionRepository.delete(tx);
        auditService.log(currentUser, "TRANSACTION_DELETED", "TRANSACTION", id.toString(), "Deleted by student");
    }

    public TransactionDto toDto(Transaction tx) {
        TransactionDto dto = new TransactionDto();
        dto.setId(tx.getId());
        dto.setType(tx.getType());
        dto.setAmount(tx.getAmount());
        dto.setCategory(tx.getCategory());
        dto.setDescription(tx.getDescription());
        dto.setTransactionDate(tx.getTransactionDate());
        dto.setPaymentMethod(tx.getPaymentMethod());
        dto.setSource(tx.getSource());
        dto.setCreatedAt(tx.getCreatedAt());
        return dto;
    }
}
