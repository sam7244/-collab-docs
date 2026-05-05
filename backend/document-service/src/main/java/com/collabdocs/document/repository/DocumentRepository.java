package com.collabdocs.document.repository;

import com.collabdocs.document.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    List<Document> findByOwnerIdOrderByUpdatedAtDesc(UUID ownerId);
    List<Document> findAllByOrderByUpdatedAtDesc();

    // Find documents that:
    //  1. Have not been edited since `inactiveSince`
    //  2. Have never had an inactivity email sent OR last email was sent before `emailCooldown`
    @Query("""
        SELECT d FROM Document d
        WHERE d.updatedAt < :inactiveSince
        AND (d.inactivityEmailSentAt IS NULL OR d.inactivityEmailSentAt < :emailCooldown)
    """)
    List<Document> findInactiveDocuments(
            @Param("inactiveSince") Instant inactiveSince,
            @Param("emailCooldown") Instant emailCooldown
    );
}
