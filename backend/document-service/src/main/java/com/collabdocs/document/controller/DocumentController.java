package com.collabdocs.document.controller;

import com.collabdocs.document.dto.DocumentRequest;
import com.collabdocs.document.dto.DocumentResponse;
import com.collabdocs.document.dto.SnapshotRequest;
import com.collabdocs.document.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Documents", description = "CRUD operations for collaborative documents")
@RestController
@RequestMapping("/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @Operation(summary = "List all documents ordered by last modified")
    @GetMapping
    public ResponseEntity<List<DocumentResponse>> list() {
        return ResponseEntity.ok(documentService.listDocuments());
    }

    @Operation(summary = "Create a new document")
    @PostMapping
    public ResponseEntity<DocumentResponse> create(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody DocumentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(documentService.createDocument(UUID.fromString(userId), request));
    }

    @Operation(summary = "Get a document by ID")
    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> get(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(documentService.getDocument(id));
    }

    @Operation(summary = "Update a document title (owner only)")
    @PatchMapping("/{id}")
    public ResponseEntity<DocumentResponse> update(
            @PathVariable("id") UUID id,
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody DocumentRequest request) {
        return ResponseEntity.ok(documentService.updateDocument(id, UUID.fromString(userId), request));
    }

    @Operation(summary = "Delete a document (owner only)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable("id") UUID id,
            @RequestHeader("X-User-Id") String userId) {
        documentService.deleteDocument(id, UUID.fromString(userId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/snapshot")
    public ResponseEntity<Void> saveSnapshot(
            @PathVariable("id") UUID id,
            @Valid @RequestBody SnapshotRequest request) {
        documentService.saveSnapshot(id, request.snapshot());
        return ResponseEntity.noContent().build();
    }
}
