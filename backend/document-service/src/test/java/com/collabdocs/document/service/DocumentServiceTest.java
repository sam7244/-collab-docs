package com.collabdocs.document.service;

import com.collabdocs.document.dto.DocumentRequest;
import com.collabdocs.document.dto.DocumentResponse;
import com.collabdocs.document.entity.Document;
import com.collabdocs.document.exception.ForbiddenException;
import com.collabdocs.document.exception.NotFoundException;
import com.collabdocs.document.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock private DocumentRepository documentRepository;
    @InjectMocks private DocumentService documentService;

    @Test
    void listDocuments_returnsAllDocuments() {
        UUID ownerId = UUID.randomUUID();
        Document doc1 = new Document(ownerId, "Doc A");
        Document doc2 = new Document(ownerId, "Doc B");
        when(documentRepository.findAllByOrderByUpdatedAtDesc()).thenReturn(List.of(doc1, doc2));

        List<DocumentResponse> result = documentService.listDocuments();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).title()).isEqualTo("Doc A");
        assertThat(result.get(1).title()).isEqualTo("Doc B");
    }

    @Test
    void createDocument_savesAndReturnsDocument() {
        UUID ownerId = UUID.randomUUID();
        DocumentRequest req = new DocumentRequest("My Doc");
        Document saved = new Document(ownerId, "My Doc");
        when(documentRepository.save(any(Document.class))).thenReturn(saved);

        DocumentResponse response = documentService.createDocument(ownerId, req);

        assertThat(response.title()).isEqualTo("My Doc");
        assertThat(response.ownerId()).isEqualTo(ownerId);
        verify(documentRepository).save(any(Document.class));
    }

    @Test
    void getDocument_whenExists_returnsDocument() {
        UUID docId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Document doc = new Document(ownerId, "Test Doc");
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));

        DocumentResponse response = documentService.getDocument(docId);

        assertThat(response.title()).isEqualTo("Test Doc");
    }

    @Test
    void getDocument_whenNotFound_throwsNotFoundException() {
        UUID docId = UUID.randomUUID();
        when(documentRepository.findById(docId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.getDocument(docId))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Document not found");
    }

    @Test
    void updateDocument_asOwner_updatesTitle() {
        UUID ownerId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        Document doc = new Document(ownerId, "Old Title");
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        DocumentResponse response = documentService.updateDocument(docId, ownerId, new DocumentRequest("New Title"));

        assertThat(response.title()).isEqualTo("New Title");
    }

    @Test
    void updateDocument_asNonOwner_throwsForbiddenException() {
        UUID ownerId = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        Document doc = new Document(ownerId, "Doc");
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> documentService.updateDocument(docId, otherUser, new DocumentRequest("Hacked")))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Access denied");

        verify(documentRepository, never()).save(any());
    }

    @Test
    void deleteDocument_asOwner_deletesSuccessfully() {
        UUID ownerId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        Document doc = new Document(ownerId, "Doc");
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));

        documentService.deleteDocument(docId, ownerId);

        verify(documentRepository).delete(doc);
    }

    @Test
    void deleteDocument_asNonOwner_throwsForbiddenException() {
        UUID ownerId = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        Document doc = new Document(ownerId, "Doc");
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> documentService.deleteDocument(docId, otherUser))
                .isInstanceOf(ForbiddenException.class);

        verify(documentRepository, never()).delete(any());
    }

    @Test
    void touchDocument_whenExists_updatesTimestamp() {
        UUID docId = UUID.randomUUID();
        Document doc = new Document(UUID.randomUUID(), "Doc");
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(documentRepository.save(any())).thenReturn(doc);

        documentService.touchDocument(docId);

        verify(documentRepository).save(doc);
    }

    @Test
    void touchDocument_whenNotFound_doesNothing() {
        UUID docId = UUID.randomUUID();
        when(documentRepository.findById(docId)).thenReturn(Optional.empty());

        documentService.touchDocument(docId);

        verify(documentRepository, never()).save(any());
    }
}
