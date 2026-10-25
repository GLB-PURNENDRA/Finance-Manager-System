package com.financemanager;

import com.financemanager.dto.AuthRequest;
import com.financemanager.dto.AuthResponse;
import com.financemanager.dto.ForgotPasswordRequest;
import com.financemanager.dto.RegisterRequest;
import com.financemanager.dto.ResetPasswordRequest;
import com.financemanager.entity.Role;
import com.financemanager.exception.ForbiddenException;
import com.financemanager.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class AuthAndSecurityTests {

    @Autowired
    private AuthService authService;

    @Test
    @DisplayName("Verify user registration and JWT token issuance")
    void testRegistration() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("newstudent@school.edu");
        req.setPassword("SecurePass123!");
        req.setDisplayName("Jordan Lee");
        req.setRole(Role.STUDENT);

        AuthResponse res = authService.register(req);
        assertNotNull(res.getToken());
        assertEquals("newstudent@school.edu", res.getEmail());
        assertEquals(Role.STUDENT, res.getRole());
        assertTrue(res.getInvitationCode().startsWith("STU-"));
    }

    @Test
    @DisplayName("Verify successful login with valid credentials")
    void testLoginSuccess() {
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail("auth_student@school.edu");
        reg.setPassword("Password123!");
        reg.setDisplayName("Auth Student");
        reg.setRole(Role.STUDENT);
        authService.register(reg);

        AuthRequest req = new AuthRequest("auth_student@school.edu", "Password123!", Role.STUDENT, false);
        AuthResponse res = authService.login(req);

        assertNotNull(res.getToken());
        assertEquals("auth_student@school.edu", res.getEmail());
        assertEquals(Role.STUDENT, res.getRole());
    }

    @Test
    @DisplayName("Verify login rejection on invalid password")
    void testLoginInvalidPassword() {
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail("wrongpass@school.edu");
        reg.setPassword("Password123!");
        reg.setDisplayName("Wrong Pass User");
        reg.setRole(Role.STUDENT);
        authService.register(reg);

        AuthRequest req = new AuthRequest("wrongpass@school.edu", "WrongPassword!");
        assertThrows(RuntimeException.class, () -> authService.login(req));
    }

    @Test
    @DisplayName("Verify Student cannot log in via Parent portal (Role Mismatch Rejection)")
    void testStudentRejectedFromParentPortal() {
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail("student_portal_check@school.edu");
        reg.setPassword("Password123!");
        reg.setDisplayName("Student Portal User");
        reg.setRole(Role.STUDENT);
        authService.register(reg);

        // Attempting to log into PARENT portal with STUDENT credentials
        AuthRequest req = new AuthRequest("student_portal_check@school.edu", "Password123!", Role.PARENT, false);
        ForbiddenException ex = assertThrows(ForbiddenException.class, () -> authService.login(req));
        assertTrue(ex.getMessage().contains("Access denied"));
        assertTrue(ex.getMessage().contains("STUDENT"));
    }

    @Test
    @DisplayName("Verify Parent cannot log in via Student portal (Role Mismatch Rejection)")
    void testParentRejectedFromStudentPortal() {
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail("parent_portal_check@family.com");
        reg.setPassword("Password123!");
        reg.setDisplayName("Parent Portal User");
        reg.setRole(Role.PARENT);
        authService.register(reg);

        // Attempting to log into STUDENT portal with PARENT credentials
        AuthRequest req = new AuthRequest("parent_portal_check@family.com", "Password123!", Role.STUDENT, false);
        ForbiddenException ex = assertThrows(ForbiddenException.class, () -> authService.login(req));
        assertTrue(ex.getMessage().contains("Access denied"));
        assertTrue(ex.getMessage().contains("PARENT"));
    }

    @Test
    @DisplayName("Verify Remember-Me session login generates valid token")
    void testRememberMeLogin() {
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail("rememberme@school.edu");
        reg.setPassword("Password123!");
        reg.setDisplayName("Remember Me User");
        reg.setRole(Role.STUDENT);
        authService.register(reg);

        AuthRequest req = new AuthRequest("rememberme@school.edu", "Password123!", Role.STUDENT, true);
        AuthResponse res = authService.login(req);
        assertNotNull(res.getToken());
    }

    @Test
    @DisplayName("Verify Forgot Password code generation and successful password reset")
    void testForgotPasswordAndReset() {
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail("recovery_user@school.edu");
        reg.setPassword("OldPassword123!");
        reg.setDisplayName("Recovery User");
        reg.setRole(Role.STUDENT);
        authService.register(reg);

        // Step 1: Request reset code
        ForgotPasswordRequest forgotReq = new ForgotPasswordRequest("recovery_user@school.edu", Role.STUDENT);
        Map<String, Object> forgotRes = authService.forgotPassword(forgotReq);
        assertTrue((Boolean) forgotRes.get("success"));
        String resetCode = (String) forgotRes.get("resetCode");
        assertNotNull(resetCode);
        assertEquals(6, resetCode.length());

        // Step 2: Reset password with code
        ResetPasswordRequest resetReq = new ResetPasswordRequest("recovery_user@school.edu", resetCode, "NewPassword456!");
        Map<String, Object> resetRes = authService.resetPassword(resetReq);
        assertTrue((Boolean) resetRes.get("success"));

        // Step 3: Verify old password fails and new password succeeds
        AuthRequest oldLogin = new AuthRequest("recovery_user@school.edu", "OldPassword123!", Role.STUDENT, false);
        assertThrows(RuntimeException.class, () -> authService.login(oldLogin));

        AuthRequest newLogin = new AuthRequest("recovery_user@school.edu", "NewPassword456!", Role.STUDENT, false);
        AuthResponse loginRes = authService.login(newLogin);
        assertNotNull(loginRes.getToken());
    }
}
