package com.financemanager.repository;

import com.financemanager.entity.Transaction;
import com.financemanager.entity.TransactionType;
import com.financemanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByOwnerOrderByTransactionDateDescCreatedAtDesc(User owner);

    List<Transaction> findByOwnerAndTypeOrderByTransactionDateDesc(User owner, TransactionType type);

    @Query("SELECT t FROM Transaction t WHERE t.owner = :owner " +
           "AND (:startDate IS NULL OR t.transactionDate >= :startDate) " +
           "AND (:endDate IS NULL OR t.transactionDate <= :endDate) " +
           "AND (:category IS NULL OR t.category = :category) " +
           "AND (:type IS NULL OR t.type = :type) " +
           "ORDER BY t.transactionDate DESC, t.createdAt DESC")
    List<Transaction> findFiltered(@Param("owner") User owner,
                                  @Param("startDate") LocalDate startDate,
                                  @Param("endDate") LocalDate endDate,
                                  @Param("category") String category,
                                  @Param("type") TransactionType type);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.owner = :owner AND t.type = :type")
    BigDecimal sumAmountByOwnerAndType(@Param("owner") User owner, @Param("type") TransactionType type);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.owner = :owner AND t.type = :type " +
           "AND t.transactionDate >= :startDate AND t.transactionDate <= :endDate")
    BigDecimal sumAmountByOwnerAndTypeAndDateRange(@Param("owner") User owner,
                                                  @Param("type") TransactionType type,
                                                  @Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.owner = :owner AND t.type = 'EXPENSE' " +
           "AND t.category = :category AND t.transactionDate >= :startDate AND t.transactionDate <= :endDate")
    BigDecimal sumExpenseByOwnerAndCategoryAndDateRange(@Param("owner") User owner,
                                                        @Param("category") String category,
                                                        @Param("startDate") LocalDate startDate,
                                                        @Param("endDate") LocalDate endDate);

    Optional<Transaction> findByOwnerAndReferenceHash(User owner, String referenceHash);

    List<Transaction> findByOwnerAndTypeAndTransactionDate(
            User owner, TransactionType type, LocalDate transactionDate);

    List<Transaction> findByOwnerAndDescriptionIgnoreCaseAndAmountAndTransactionDate(
            User owner, String description, BigDecimal amount, LocalDate transactionDate);
}
