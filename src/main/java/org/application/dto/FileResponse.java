package org.application.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record FileResponse(UUID id, String fileName, String mimeType, long totalSizeBytes, String status,
                           OffsetDateTime createdAt) {};
