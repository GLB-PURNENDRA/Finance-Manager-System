package com.financemanager.service;

import com.financemanager.config.JwtService;
import com.financemanager.dto.AuthRequest;
import com.financemanager.dto.AuthResponse;
import com.financemanager.dto.ForgotPasswordRequest;
import com.financemanager.dto.RegisterRequest;
import com.financemanager.dto.ResetPasswordRequest;
import com.financemanager.entity.Role;
import com.financemanager.entity.User;
import com.financemanager.exception.BadRequestException;
import com.financemanager.exception.ForbiddenException;
import com.financemanager.exception.UnauthorizedException;
import com.financemanager.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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

        String token = jwtService.generateToken(user, false);
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getDisplayName(), user.getRole(), user.getInvitationCode());
    }

    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password.");
        }

        if (request.getExpectedRole() != null && user.getRole() != request.getExpectedRole()) {
            throw new ForbiddenException("Access denied: This account is registered as " + user.getRole()
                    + ". Please use the " + (user.getRole() == Role.STUDENT ? "Student" : "Parent") + " Login portal.");
        }

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new UnauthorizedException("Account is inactive or disabled.");
        }

        auditService.log(user, "USER_LOGIN", "USER", user.getId().toString(), "Login successful (" + user.getRole() + ")");

        String token = jwtService.generateToken(user, request.isRememberMe());
        return new AuthResponse(token, user.getId(), user.getEmail(), user.getDisplayName(), user.getRole(), user.getInvitationCode());
    }

    public Map<String, Object> forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            throw new BadRequestException("No registered account found with email: " + request.getEmail());
        }

        User user = userOpt.get();
        if (request.getRole() != null && user.getRole() != request.getRole()) {
            throw new ForbiddenException("Account exists, but it is registered as " + user.getRole() + ", not " + request.getRole() + ".");
        }

        // Generate a cryptographically secure 6-digit reset code
        String resetToken = String.format("%06d", new SecureRandom().nextInt(1000000));
        user.setResetToken(resetToken);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        auditService.log(user, "FORGOT_PASSWORD_REQUEST", "USER", user.getId().toString(), "Password reset code generated");

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Password reset verification code generated. Valid for 15 minutes.");
        response.put("resetCode", resetToken);
        response.put("email", user.getEmail());
        return response;
    }

    public Map<String, Object> resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("No registered account found with email: " + request.getEmail()));

        if (user.getResetToken() == null || !user.getResetToken().equals(request.getToken().trim())) {
            throw new BadRequestException("Invalid or incorrect reset code.");
        }

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Reset code has expired. Please request a new one.");
        }

        if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
            throw new BadRequestException("New password must be at least 6 characters long.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);

        auditService.log(user, "PASSWORD_RESET_SUCCESS", "USER", user.getId().toString(), "Password reset completed");

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Password has been successfully updated. You may now log in with your new password.");
        return response;
    }
}
