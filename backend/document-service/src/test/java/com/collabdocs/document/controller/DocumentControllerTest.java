package com.collabdocs.document.controller;

import com.collabdocs.document.dto.DocumentRequest;
import com.collabdocs.document.dto.DocumentResponse;
import com.collabdocs.document.exception.ForbiddenException;
import com.collabdocs.document.exception.NotFoundException;
import com.collabdocs.document.service.DocumentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DocumentController.class)
class DocumentControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean DocumentService documentService;

    private DocumentResponse sampleDoc(UUID id, UUID ownerId) {
        return new DocumentResponse(id, ownerId, "Test Doc", Instant.now(), Instant.now(), null);
    }

    @Test
    void listDocuments_returns200WithList() throws Exception {
        UUID ownerId = UUID.randomUUID();
        when(documentService.listDocuments())
                .thenReturn(List.of(sampleDoc(UUID.randomUUID(), ownerId)));

        mockMvc.perform(get("/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].title").value("Test Doc"));
    }

    @Test
    void createDocument_withValidBody_returns201() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        when(documentService.createDocument(eq(ownerId), any(DocumentRequest.class)))
                .thenReturn(sampleDoc(docId, ownerId));

        mockMvc.perform(post("/documents")
                        .header("X-User-Id", ownerId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "Test Doc"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(docId.toString()))
                .andExpect(jsonPath("$.title").value("Test Doc"));
    }

    @Test
    void createDocument_withBlankTitle_returns400() throws Exception {
        UUID ownerId = UUID.randomUUID();

        mockMvc.perform(post("/documents")
                        .header("X-User-Id", ownerId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", ""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getDocument_whenExists_returns200() throws Exception {
        UUID docId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        when(documentService.getDocument(docId)).thenReturn(sampleDoc(docId, ownerId));

        mockMvc.perform(get("/documents/{id}", docId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(docId.toString()));
    }

    @Test
    void getDocument_whenNotFound_returns404() throws Exception {
        UUID docId = UUID.randomUUID();
        when(documentService.getDocument(docId))
                .thenThrow(new NotFoundException("Document not found: " + docId));

        mockMvc.perform(get("/documents/{id}", docId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Document not found: " + docId));
    }

    @Test
    void updateDocument_asOwner_returns200() throws Exception {
        UUID docId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        DocumentResponse updated = new DocumentResponse(docId, ownerId, "Updated Title", Instant.now(), Instant.now(), null);
        when(documentService.updateDocument(eq(docId), eq(ownerId), any(DocumentRequest.class)))
                .thenReturn(updated);

        mockMvc.perform(patch("/documents/{id}", docId)
                        .header("X-User-Id", ownerId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "Updated Title"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"));
    }

    @Test
    void updateDocument_asNonOwner_returns403() throws Exception {
        UUID docId = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();
        when(documentService.updateDocument(eq(docId), eq(otherUser), any(DocumentRequest.class)))
                .thenThrow(new ForbiddenException("Access denied"));

        mockMvc.perform(patch("/documents/{id}", docId)
                        .header("X-User-Id", otherUser.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "Hacked"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteDocument_asOwner_returns204() throws Exception {
        UUID docId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        doNothing().when(documentService).deleteDocument(docId, ownerId);

        mockMvc.perform(delete("/documents/{id}", docId)
                        .header("X-User-Id", ownerId.toString()))
                .andExpect(status().isNoContent());

        verify(documentService).deleteDocument(docId, ownerId);
    }

    @Test
    void deleteDocument_asNonOwner_returns403() throws Exception {
        UUID docId = UUID.randomUUID();
        UUID otherUser = UUID.randomUUID();
        doThrow(new ForbiddenException("Access denied"))
                .when(documentService).deleteDocument(docId, otherUser);

        mockMvc.perform(delete("/documents/{id}", docId)
                        .header("X-User-Id", otherUser.toString()))
                .andExpect(status().isForbidden());
    }
}
