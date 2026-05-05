package com.collabdocs.collaboration.controller;

import com.collabdocs.collaboration.presence.PresenceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PresenceController.class)
class PresenceControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean PresenceService presenceService;

    @Test
    void getPresence_returnsSetOfUsers() throws Exception {
        when(presenceService.getPresence("doc-abc"))
                .thenReturn(Set.of("user-1", "user-2"));

        mockMvc.perform(get("/presence/doc-abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getPresence_whenNoUsers_returnsEmptyArray() throws Exception {
        when(presenceService.getPresence("doc-xyz"))
                .thenReturn(Set.of());

        mockMvc.perform(get("/presence/doc-xyz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
