package com.finance.manager;

import jakarta.persistence.*;

/**
 * JPA entity representing an application user.
 * Passwords are never stored in plain text — only the BCrypt hash is persisted.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    /** BCrypt hash of the user's password — never the raw password. */
    @Column(nullable = false)
    private String passwordHash;

    /** Immutable OIDC subject. Null for offline-only Swing accounts. */
    @Column(unique = true, length = 128)
    private String externalSubject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    public enum Role { USER, ADMIN }

    protected User() {}

    public User(String username, String passwordHash) {
        this.username     = username;
        this.passwordHash = passwordHash;
    }

    public static User oauthUser(String username, String externalSubject) {
        if (externalSubject == null || externalSubject.isBlank()) {
            throw new IllegalArgumentException("OIDC subject is required.");
        }
        User user = new User(username, "{oauth2}external-account");
        user.externalSubject = externalSubject;
        return user;
    }

    public Long   getId()           { return id; }
    public String getUsername()     { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getExternalSubject() { return externalSubject; }
    public Role   getRole()         { return role; }
    public void   setRole(Role role){ this.role = role; }
}
