package org.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;


public record InitiateUploadRequest(
        @NotBlank(message="fileName is required") @Size(max=255) String fileName,
        @NotBlank(message="mimeType is required") @Size(max=100) String mimeType,
        @Positive(message="totalSizeBytes must be positive") long totalSizeBytes
) {};
