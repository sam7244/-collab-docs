package com.collabdocs.document.dto;

import com.collabdocs.document.entity.Document;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        UUID ownerId,
        String title,
        Instant createdAt,
        Instant updatedAt,
        String latestSnapshot  // base64-encoded Yjs state, nullable
) {
    public static DocumentResponse from(Document doc) {
        String snapshot = null;
        if (doc.getLatestSnapshot() != null && doc.getLatestSnapshot().length > 0) {
            snapshot = Base64.getEncoder().encodeToString(doc.getLatestSnapshot());
        }
        return new DocumentResponse(
                doc.getId(),
                doc.getOwnerId(),
                doc.getTitle(),
                doc.getCreatedAt(),
                doc.getUpdatedAt(),
                snapshot
        );
    }
}
