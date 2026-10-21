package com.financemanager.repository;

import com.financemanager.entity.InvestorProfile;
import com.financemanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvestorProfileRepository extends JpaRepository<InvestorProfile, Long> {
    Optional<InvestorProfile> findByStudent(User student);
}
