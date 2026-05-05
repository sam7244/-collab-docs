package com.collabdocs.document.scheduler;

import com.collabdocs.document.entity.Document;
import com.collabdocs.document.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InactiveDocumentSchedulerTest {

    @Mock DocumentRepository documentRepository;
    @Mock AuthServiceClient authServiceClient;
    @Mock EmailService emailService;
    @InjectMocks InactiveDocumentScheduler scheduler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduler, "inactivityThresholdMinutes", 5L);
        ReflectionTestUtils.setField(scheduler, "emailCooldownMinutes", 10L);
        ReflectionTestUtils.setField(scheduler, "emailNotificationsEnabled", true);
    }

    @Test
    void checkInactiveDocuments_whenNotificationsDisabled_skips() {
        ReflectionTestUtils.setField(scheduler, "emailNotificationsEnabled", false);

        scheduler.checkInactiveDocuments();

        verify(documentRepository, never()).findInactiveDocuments(any(), any());
    }

    @Test
    void checkInactiveDocuments_whenNoInactiveDocs_doesNotSendEmail() {
        when(documentRepository.findInactiveDocuments(any(), any()))
                .thenReturn(Collections.emptyList());

        scheduler.checkInactiveDocuments();

        verify(emailService, never()).sendInactivityEmail(any(), any(), any(), any(), any());
    }

    @Test
    void checkInactiveDocuments_withInactiveDoc_sendsEmailAndMarksDoc() {
        UUID ownerId = UUID.randomUUID();
        Document doc = new Document(ownerId, "Stale Doc");
        ReflectionTestUtils.setField(doc, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(doc, "updatedAt", Instant.now());
        when(documentRepository.findInactiveDocuments(any(), any())).thenReturn(List.of(doc));
        when(authServiceClient.getUserById(ownerId))
                .thenReturn(new AuthServiceClient.UserInfo(ownerId, "owner@test.com", "Owner"));

        scheduler.checkInactiveDocuments();

        verify(emailService).sendInactivityEmail(
                eq("owner@test.com"), eq("Owner"), eq("Stale Doc"), any(), any());
        verify(documentRepository).save(doc);
    }

    @Test
    void checkInactiveDocuments_whenOwnerNotFound_skipsEmail() {
        UUID ownerId = UUID.randomUUID();
        Document doc = new Document(ownerId, "Orphan Doc");
        when(documentRepository.findInactiveDocuments(any(), any())).thenReturn(List.of(doc));
        when(authServiceClient.getUserById(ownerId)).thenReturn(null);

        scheduler.checkInactiveDocuments();

        verify(emailService, never()).sendInactivityEmail(any(), any(), any(), any(), any());
        verify(documentRepository, never()).save(any());
    }

    @Test
    void checkInactiveDocuments_whenOwnerEmailNull_skipsEmail() {
        UUID ownerId = UUID.randomUUID();
        Document doc = new Document(ownerId, "Doc");
        when(documentRepository.findInactiveDocuments(any(), any())).thenReturn(List.of(doc));
        when(authServiceClient.getUserById(ownerId))
                .thenReturn(new AuthServiceClient.UserInfo(ownerId, null, "Owner"));

        scheduler.checkInactiveDocuments();

        verify(emailService, never()).sendInactivityEmail(any(), any(), any(), any(), any());
    }

    @Test
    void checkInactiveDocuments_multipleInactiveDocs_sendsEmailForEach() {
        UUID owner1 = UUID.randomUUID();
        UUID owner2 = UUID.randomUUID();
        Document doc1 = new Document(owner1, "Doc 1");
        Document doc2 = new Document(owner2, "Doc 2");
        ReflectionTestUtils.setField(doc1, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(doc2, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(doc1, "updatedAt", Instant.now());
        ReflectionTestUtils.setField(doc2, "updatedAt", Instant.now());
        when(documentRepository.findInactiveDocuments(any(), any())).thenReturn(List.of(doc1, doc2));
        when(authServiceClient.getUserById(owner1))
                .thenReturn(new AuthServiceClient.UserInfo(owner1, "a@test.com", "A"));
        when(authServiceClient.getUserById(owner2))
                .thenReturn(new AuthServiceClient.UserInfo(owner2, "b@test.com", "B"));

        scheduler.checkInactiveDocuments();

        verify(emailService, times(2)).sendInactivityEmail(any(), any(), any(), any(), any());
    }
}
