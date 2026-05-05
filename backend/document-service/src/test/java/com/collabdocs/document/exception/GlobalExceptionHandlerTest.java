package com.collabdocs.document.exception;

import com.collabdocs.document.controller.DocumentController;
import com.collabdocs.document.service.DocumentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DocumentController.class)
class GlobalExceptionHandlerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean DocumentService documentService;

    @Test
    void notFoundException_returns404WithBody() throws Exception {
        UUID id = UUID.randomUUID();
        when(documentService.getDocument(id))
                .thenThrow(new NotFoundException("Document not found: " + id));

        mockMvc.perform(get("/documents/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Document not found: " + id))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void forbiddenException_returns403WithBody() throws Exception {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(documentService.updateDocument(any(), any(), any()))
                .thenThrow(new ForbiddenException("Access denied"));

        mockMvc.perform(patch("/documents/{id}", id)
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "X"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void validationException_returns400WithBody() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(post("/documents")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").exists());
    }
}
