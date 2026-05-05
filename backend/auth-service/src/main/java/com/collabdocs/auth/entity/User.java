package com.collabdocs.auth.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = true)
    private String passwordHash;

    @Column(nullable = false)
    private String displayName;

    @Column(name = "oauth2_provider", nullable = true)
    private String oauth2Provider;

    @Column(name = "oauth2_provider_id", nullable = true)
    private String oauth2ProviderId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public User() {}

    // Email/password signup
    public User(String email, String passwordHash, String displayName) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName; }
    public String getOauth2Provider() { return oauth2Provider; }
    public String getOauth2ProviderId() { return oauth2ProviderId; }
    public Instant getCreatedAt() { return createdAt; }

    public void setEmail(String email) { this.email = email; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setOauth2Provider(String oauth2Provider) { this.oauth2Provider = oauth2Provider; }
    public void setOauth2ProviderId(String oauth2ProviderId) { this.oauth2ProviderId = oauth2ProviderId; }
}
