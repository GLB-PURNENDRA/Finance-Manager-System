package com.financemanager.repository;

import com.financemanager.entity.AuditEvent;
import com.financemanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    List<AuditEvent> findByActorOrderByTimestampDesc(User actor);
}
