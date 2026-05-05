package com.collabdocs.document.kafka;

import com.collabdocs.document.entity.AuditLog;
import com.collabdocs.document.repository.AuditLogRepository;
import com.collabdocs.document.service.DocumentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class EditEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(EditEventConsumer.class);

    private final DocumentService documentService;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EditEventConsumer(DocumentService documentService, AuditLogRepository auditLogRepository) {
        this.documentService = documentService;
        this.auditLogRepository = auditLogRepository;
    }

    @KafkaListener(topics = "edit-events", groupId = "document-service")
    public void onEditEvent(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            UUID docId = UUID.fromString(event.get("docId").asText());
            documentService.touchDocument(docId);
            log.debug("Touched document {} from edit event", docId);
        } catch (Exception e) {
            log.error("Failed to process edit-event: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "audit-log", groupId = "document-service-audit")
    public void onAuditEvent(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            UUID docId = UUID.fromString(event.get("docId").asText());
            UUID userId = UUID.fromString(event.get("userId").asText());
            String action = event.get("action").asText();
            Instant timestamp = Instant.parse(event.get("timestamp").asText());
            auditLogRepository.save(new AuditLog(docId, userId, action, timestamp));
            log.debug("Saved audit log entry: {} by {} on {}", action, userId, docId);
        } catch (Exception e) {
            log.error("Failed to process audit-log event: {}", e.getMessage());
        }
    }
}
