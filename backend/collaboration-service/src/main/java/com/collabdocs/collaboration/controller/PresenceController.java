package com.collabdocs.collaboration.controller;

import com.collabdocs.collaboration.presence.PresenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/presence")
public class PresenceController {

    private final PresenceService presenceService;

    public PresenceController(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @GetMapping("/{docId}")
    public ResponseEntity<Set<String>> getPresence(@PathVariable("docId") String docId) {
        return ResponseEntity.ok(presenceService.getPresence(docId));
    }
}
