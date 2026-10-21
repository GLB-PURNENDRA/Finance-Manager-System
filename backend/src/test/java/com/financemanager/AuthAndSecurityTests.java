package com.financemanager;

import com.financemanager.dto.AuthRequest;
import com.financemanager.dto.AuthResponse;
import com.financemanager.dto.RegisterRequest;
import com.financemanager.entity.Role;
import com.financemanager.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

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
        AuthRequest req = new AuthRequest("student@university.edu", "Password123!");
        AuthResponse res = authService.login(req);

        assertNotNull(res.getToken());
        assertEquals("student@university.edu", res.getEmail());
        assertEquals(Role.STUDENT, res.getRole());
    }

    @Test
    @DisplayName("Verify login rejection on invalid password")
    void testLoginInvalidPassword() {
        AuthRequest req = new AuthRequest("student@university.edu", "WrongPassword!");
        assertThrows(RuntimeException.class, () -> authService.login(req));
    }
}
