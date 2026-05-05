package com.collabdocs.document.kafka;

import com.collabdocs.document.entity.AuditLog;
import com.collabdocs.document.repository.AuditLogRepository;
import com.collabdocs.document.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EditEventConsumerTest {

    @Mock DocumentService documentService;
    @Mock AuditLogRepository auditLogRepository;
    @InjectMocks EditEventConsumer consumer;

    @Test
    void onEditEvent_validJson_callsTouchDocument() {
        UUID docId = UUID.randomUUID();
        String message = """
            {"docId":"%s","userId":"%s","timestamp":"2024-01-01T00:00:00Z","updateSize":42}
            """.formatted(docId, UUID.randomUUID());

        consumer.onEditEvent(message);

        verify(documentService).touchDocument(docId);
    }

    @Test
    void onEditEvent_invalidJson_doesNotThrow() {
        consumer.onEditEvent("not-valid-json");
        verify(documentService, never()).touchDocument(any());
    }

    @Test
    void onEditEvent_missingDocId_doesNotThrow() {
        consumer.onEditEvent("{\"userId\":\"some-id\"}");
        verify(documentService, never()).touchDocument(any());
    }

    @Test
    void onAuditEvent_validJson_savesAuditLog() {
        UUID docId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        String message = """
            {"docId":"%s","userId":"%s","action":"EDIT","timestamp":"2024-01-01T10:00:00Z"}
            """.formatted(docId, userId);

        consumer.onAuditEvent(message);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertThat(saved.getDocId()).isEqualTo(docId);
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getAction()).isEqualTo("EDIT");
    }

    @Test
    void onAuditEvent_invalidJson_doesNotThrow() {
        consumer.onAuditEvent("{bad json}");
        verify(auditLogRepository, never()).save(any());
    }
}
