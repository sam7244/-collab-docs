package com.collabdocs.document.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID docId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private Instant timestamp;

    public AuditLog() {}

    public AuditLog(UUID docId, UUID userId, String action, Instant timestamp) {
        this.docId = docId;
        this.userId = userId;
        this.action = action;
        this.timestamp = timestamp;
    }

    public UUID getId() { return id; }
    public UUID getDocId() { return docId; }
    public UUID getUserId() { return userId; }
    public String getAction() { return action; }
    public Instant getTimestamp() { return timestamp; }
}
