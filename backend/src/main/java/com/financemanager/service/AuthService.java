package com.financemanager.service;

import com.financemanager.config.JwtService;
import com.financemanager.dto.AuthRequest;
import com.financemanager.dto.AuthResponse;
import com.financemanager.dto.RegisterRequest;
import com.financemanager.entity.Role;
import com.financemanager.entity.User;
import com.financemanager.exception.BadRequestException;
import com.financemanager.exception.UnauthorizedException;
import com.financemanager.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuditService auditService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
            throw new BadRequestException("An account with email " + request.getEmail() + " already exists.");
        }

        String invitationCode = null;
        if (request.getRole() == Role.STUDENT) {
            invitationCode = "STU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        }

        User user = new User(
                request.getEmail().toLowerCase().trim(),
                passwordEncoder.encode(request.getPassword()),
                request.getDisplayName().trim(),
                request.getRole(),
                invitationCode
        );

        user = userRepository.save(user);
        auditService.log(user, "USER_REGISTERED", "USER", user.getId().toString(), "Role: " + user.getRole());

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getDisplayName(), user.getRole(), user.getInvitationCode());
    }

    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password.");
        }

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new UnauthorizedException("Account is inactive or disabled.");
        }

        auditService.log(user, "USER_LOGIN", "USER", user.getId().toString(), "Login successful");

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getDisplayName(), user.getRole(), user.getInvitationCode());
    }
}
