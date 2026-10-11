package org.application.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CompleteUploadRequest(@NotEmpty List<PartInput> parts) {};
