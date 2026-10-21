package com.financemanager.repository;

import com.financemanager.entity.FamilyLink;
import com.financemanager.entity.LinkStatus;
import com.financemanager.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FamilyLinkRepository extends JpaRepository<FamilyLink, Long> {
    List<FamilyLink> findByStudentOrderByCreatedAtDesc(User student);
    List<FamilyLink> findByParentOrderByCreatedAtDesc(User parent);
    Optional<FamilyLink> findByParentAndStudent(User parent, User student);
    List<FamilyLink> findByParentAndStatus(User parent, LinkStatus status);
    List<FamilyLink> findByParentAndStatusOrderByApprovedAtDescCreatedAtDesc(User parent, LinkStatus status);
    Optional<FamilyLink> findByParentAndStudentAndStatus(User parent, User student, LinkStatus status);
    Optional<FamilyLink> findByParentAndStudent_IdAndStatus(User parent, Long studentId, LinkStatus status);
}
