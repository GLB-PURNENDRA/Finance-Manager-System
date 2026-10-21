package com.financemanager.repository;

import com.financemanager.entity.SipRecord;
import com.financemanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SipRecordRepository extends JpaRepository<SipRecord, Long> {
    List<SipRecord> findByStudentOrderByCreatedAtDesc(User student);
}
