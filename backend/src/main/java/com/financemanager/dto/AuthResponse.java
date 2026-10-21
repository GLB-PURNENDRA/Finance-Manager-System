package com.financemanager.dto;

import com.financemanager.entity.Role;

public class AuthResponse {

    private String token;
    private Long userId;
    private String email;
    private String displayName;
    private Role role;
    private String invitationCode;

    public AuthResponse() {}

    public AuthResponse(String token, Long userId, String email, String displayName, Role role, String invitationCode) {
        this.token = token;
        this.userId = userId;
        this.email = email;
        this.displayName = displayName;
        this.role = role;
        this.invitationCode = invitationCode;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getInvitationCode() { return invitationCode; }
    public void setInvitationCode(String invitationCode) { this.invitationCode = invitationCode; }
}
