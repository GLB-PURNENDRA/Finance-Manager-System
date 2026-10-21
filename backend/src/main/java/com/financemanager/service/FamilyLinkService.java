package com.financemanager.service;

import com.financemanager.dto.FamilyLinkDto;
import com.financemanager.dto.LinkRequestDto;
import com.financemanager.entity.FamilyLink;
import com.financemanager.entity.LinkStatus;
import com.financemanager.entity.Role;
import com.financemanager.entity.User;
import com.financemanager.exception.BadRequestException;
import com.financemanager.exception.ForbiddenException;
import com.financemanager.exception.ResourceNotFoundException;
import com.financemanager.repository.FamilyLinkRepository;
import com.financemanager.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FamilyLinkService {

    @Autowired
    private FamilyLinkRepository familyLinkRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditService auditService;

    public FamilyLinkDto requestLink(User parent, LinkRequestDto request) {
        if (parent.getRole() != Role.PARENT) {
            throw new ForbiddenException("Only Parent accounts can send a link request.");
        }

        String identifier = request.getIdentifier().trim();
        User student = userRepository.findByEmail(identifier.toLowerCase())
                .or(() -> userRepository.findByInvitationCode(identifier))
                .orElseThrow(() -> new ResourceNotFoundException("No Student account found matching: " + identifier));

        if (student.getRole() != Role.STUDENT) {
            throw new BadRequestException("The target account is not a Student account.");
        }

        // Check if a link already exists
        Optional<FamilyLink> existing = familyLinkRepository.findByParentAndStudent(parent, student);
        FamilyLink link;
        if (existing.isPresent()) {
            link = existing.get();
            if (link.getStatus() == LinkStatus.ACTIVE) {
                throw new BadRequestException("You are already actively linked to this Student.");
            }
            // Re-request link
            link.setStatus(LinkStatus.PENDING);
            link.setRequestedBy("PARENT");
            link.setApprovedAt(null);
        } else {
            link = new FamilyLink(parent, student, LinkStatus.PENDING, "PARENT");
        }

        link = familyLinkRepository.save(link);
        auditService.log(parent, "LINK_REQUESTED", "STUDENT", student.getId().toString(), "Parent sent link request");
        return toDto(link);
    }

    public FamilyLinkDto approveLink(User student, Long linkId) {
        if (student.getRole() != Role.STUDENT) {
            throw new ForbiddenException("Only Students can approve link requests.");
        }

        FamilyLink link = familyLinkRepository.findById(linkId)
                .orElseThrow(() -> new ResourceNotFoundException("Link request not found."));

        if (!link.getStudent().getId().equals(student.getId())) {
            throw new ForbiddenException("You are not authorized to approve this link.");
        }

        link.setStatus(LinkStatus.ACTIVE);
        link.setApprovedAt(LocalDateTime.now());
        link = familyLinkRepository.save(link);

        auditService.log(student, "LINK_APPROVED", "PARENT", link.getParent().getId().toString(), "Student approved link");
        return toDto(link);
    }

    public FamilyLinkDto revokeLink(User currentUser, Long linkId) {
        FamilyLink link = familyLinkRepository.findById(linkId)
                .orElseThrow(() -> new ResourceNotFoundException("Link not found."));

        boolean isStudent = link.getStudent().getId().equals(currentUser.getId());
        boolean isParent = link.getParent().getId().equals(currentUser.getId());

        if (!isStudent && !isParent) {
            throw new ForbiddenException("You are not authorized to modify this link.");
        }

        link.setStatus(LinkStatus.REVOKED);
        link = familyLinkRepository.save(link);

        auditService.log(currentUser, "LINK_REVOKED", "FAMILY_LINK", link.getId().toString(), "Revoked by " + currentUser.getRole());
        return toDto(link);
    }

    public List<FamilyLinkDto> getMyLinks(User user) {
        if (user.getRole() == Role.STUDENT) {
            return familyLinkRepository.findByStudentOrderByCreatedAtDesc(user)
                    .stream().map(this::toDto).collect(Collectors.toList());
        } else {
            return familyLinkRepository.findByParentOrderByCreatedAtDesc(user)
                    .stream().map(this::toDto).collect(Collectors.toList());
        }
    }

    public User getActiveStudentForParent(User parent) {
        return getActiveStudentForParent(parent, null);
    }

    public User getActiveStudentForParent(User parent, Long studentId) {
        if (parent.getRole() != Role.PARENT) {
            throw new ForbiddenException("Operation requires Parent role.");
        }

        return getActiveLinkForParent(parent, studentId)
                .map(FamilyLink::getStudent)
                .orElseThrow(() -> new ForbiddenException(
                        "No active approved link to a Student account. Access to student financial data is denied."));
    }

    public Optional<FamilyLink> getActiveLinkForParent(User parent) {
        return getActiveLinkForParent(parent, null);
    }

    public Optional<FamilyLink> getActiveLinkForParent(User parent, Long studentId) {
        if (studentId != null) {
            return familyLinkRepository.findByParentAndStudent_IdAndStatus(parent, studentId, LinkStatus.ACTIVE);
        }
        List<FamilyLink> activeLinks = familyLinkRepository.findByParentAndStatusOrderByApprovedAtDescCreatedAtDesc(parent, LinkStatus.ACTIVE);
        if (activeLinks.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(activeLinks.get(0));
    }

    public List<FamilyLinkDto> getActiveLinksForParent(User parent) {
        if (parent.getRole() != Role.PARENT) {
            return List.of();
        }
        return familyLinkRepository.findByParentAndStatusOrderByApprovedAtDescCreatedAtDesc(parent, LinkStatus.ACTIVE)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public FamilyLinkDto toDto(FamilyLink link) {
        FamilyLinkDto dto = new FamilyLinkDto();
        dto.setId(link.getId());
        dto.setParentId(link.getParent().getId());
        dto.setParentEmail(link.getParent().getEmail());
        dto.setParentName(link.getParent().getDisplayName());
        dto.setStudentId(link.getStudent().getId());
        dto.setStudentEmail(link.getStudent().getEmail());
        dto.setStudentName(link.getStudent().getDisplayName());
        dto.setStatus(link.getStatus());
        dto.setRequestedBy(link.getRequestedBy());
        dto.setApprovedAt(link.getApprovedAt());
        dto.setCreatedAt(link.getCreatedAt());
        return dto;
    }
}
