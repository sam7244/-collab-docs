package com.collabdocs.document.service;

import com.collabdocs.document.dto.DocumentRequest;
import com.collabdocs.document.dto.DocumentResponse;
import com.collabdocs.document.entity.Document;
import com.collabdocs.document.exception.ForbiddenException;
import com.collabdocs.document.exception.NotFoundException;
import com.collabdocs.document.repository.DocumentRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;

    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public List<DocumentResponse> listDocuments() {
        return documentRepository.findAllByOrderByUpdatedAtDesc()
                .stream()
                .map(DocumentResponse::from)
                .toList();
    }

    public DocumentResponse createDocument(UUID ownerId, DocumentRequest request) {
        Document doc = new Document(ownerId, request.title());
        return DocumentResponse.from(documentRepository.save(doc));
    }

    public DocumentResponse getDocument(UUID docId) {
        return DocumentResponse.from(findOrThrow(docId));
    }

    public DocumentResponse updateDocument(UUID docId, UUID requesterId, DocumentRequest request) {
        Document doc = findOrThrow(docId);
        assertOwner(doc, requesterId);
        doc.setTitle(request.title());
        doc.setUpdatedAt(Instant.now());
        return DocumentResponse.from(documentRepository.save(doc));
    }

    public void deleteDocument(UUID docId, UUID requesterId) {
        Document doc = findOrThrow(docId);
        assertOwner(doc, requesterId);
        documentRepository.delete(doc);
    }

    public void saveSnapshot(UUID docId, String base64Snapshot) {
        Document doc = findOrThrow(docId);
        doc.setLatestSnapshot(Base64.getDecoder().decode(base64Snapshot));
        doc.setUpdatedAt(Instant.now());
        documentRepository.save(doc);
    }

    public void touchDocument(UUID docId) {
        documentRepository.findById(docId).ifPresent(doc -> {
            doc.setUpdatedAt(Instant.now());
            documentRepository.save(doc);
        });
    }

    private Document findOrThrow(UUID docId) {
        return documentRepository.findById(docId)
                .orElseThrow(() -> new NotFoundException("Document not found: " + docId));
    }

    private void assertOwner(Document doc, UUID requesterId) {
        if (!doc.getOwnerId().equals(requesterId)) {
            throw new ForbiddenException("Access denied");
        }
    }
}
