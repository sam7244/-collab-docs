package com.collabdocs.document.dto;

import jakarta.validation.constraints.NotBlank;

public record DocumentRequest(@NotBlank String title) {}
