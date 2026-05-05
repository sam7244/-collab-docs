package com.collabdocs.document.dto;

import jakarta.validation.constraints.NotBlank;

public record SnapshotRequest(@NotBlank String snapshot) {}  // base64-encoded Yjs state
