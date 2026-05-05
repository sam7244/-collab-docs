package com.collabdocs.document.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] latestSnapshot;

    @Column(name = "inactivity_email_sent_at", nullable = true)
    private Instant inactivityEmailSentAt;

    public Document() {}

    public Document(UUID ownerId, String title) {
        this.ownerId = ownerId;
        this.title = title;
    }

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public String getTitle() { return title; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public byte[] getLatestSnapshot() { return latestSnapshot; }

    public Instant getInactivityEmailSentAt() { return inactivityEmailSentAt; }

    public void setTitle(String title) { this.title = title; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public void setLatestSnapshot(byte[] latestSnapshot) { this.latestSnapshot = latestSnapshot; }
    public void setInactivityEmailSentAt(Instant inactivityEmailSentAt) { this.inactivityEmailSentAt = inactivityEmailSentAt; }
}
