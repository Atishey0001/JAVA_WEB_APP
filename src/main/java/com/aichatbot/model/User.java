package com.aichatbot.model;

public class User extends BaseEntity {

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_USER = "USER";

    private String fullName;
    private String email;
    private String passwordHash;
    private String role = ROLE_USER;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    /** Used from JSP as ${user.admin}. */
    public boolean isAdmin() { return ROLE_ADMIN.equals(role); }
}
