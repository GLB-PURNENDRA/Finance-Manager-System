package com.financemanager.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "family_links", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"parent_user_id", "student_user_id"})
})
public class FamilyLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_user_id", nullable = false)
    private User parent;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_user_id", nullable = false)
    private User student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LinkStatus status;

    @Column(nullable = false, length = 20)
    private String requestedBy; // "PARENT" or "STUDENT"

    private LocalDateTime approvedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public FamilyLink() {}

    public FamilyLink(User parent, User student, LinkStatus status, String requestedBy) {
        this.parent = parent;
        this.student = student;
        this.status = status;
        this.requestedBy = requestedBy;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getParent() { return parent; }
    public void setParent(User parent) { this.parent = parent; }

    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }

    public LinkStatus getStatus() { return status; }
    public void setStatus(LinkStatus status) { this.status = status; }

    public String getRequestedBy() { return requestedBy; }
    public void setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
