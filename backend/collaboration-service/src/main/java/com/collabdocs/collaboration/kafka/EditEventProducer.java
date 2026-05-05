package com.collabdocs.collaboration.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class EditEventProducer {

    private static final Logger log = LoggerFactory.getLogger(EditEventProducer.class);
    private static final String EDIT_EVENTS_TOPIC = "edit-events";
    private static final String AUDIT_LOG_TOPIC = "audit-log";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EditEventProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishEditEvent(String docId, String userId, int updateSize) {
        try {
            Map<String, Object> event = Map.of(
                    "eventId", UUID.randomUUID().toString(),
                    "docId", docId,
                    "userId", userId,
                    "timestamp", Instant.now().toString(),
                    "updateSize", updateSize
            );
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(EDIT_EVENTS_TOPIC, docId, payload);

            Map<String, Object> auditEvent = Map.of(
                    "eventId", UUID.randomUUID().toString(),
                    "docId", docId,
                    "userId", userId,
                    "action", "EDIT",
                    "timestamp", Instant.now().toString()
            );
            kafkaTemplate.send(AUDIT_LOG_TOPIC, docId, objectMapper.writeValueAsString(auditEvent));
        } catch (Exception e) {
            log.error("Failed to publish edit event: {}", e.getMessage());
        }
    }
}
