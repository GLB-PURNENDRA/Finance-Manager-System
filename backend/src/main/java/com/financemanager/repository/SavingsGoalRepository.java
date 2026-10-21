package com.financemanager.repository;

import com.financemanager.entity.SavingsGoal;
import com.financemanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {
    List<SavingsGoal> findByStudentOrderByCreatedAtDesc(User student);
}
