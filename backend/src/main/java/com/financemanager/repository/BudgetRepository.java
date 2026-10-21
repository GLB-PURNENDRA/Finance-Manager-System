package com.financemanager.repository;

import com.financemanager.entity.Budget;
import com.financemanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByStudentOrderByPeriodMonthDesc(User student);
    List<Budget> findByStudentAndPeriodMonth(User student, String periodMonth);
    Optional<Budget> findByStudentAndCategoryAndPeriodMonth(User student, String category, String periodMonth);
}
