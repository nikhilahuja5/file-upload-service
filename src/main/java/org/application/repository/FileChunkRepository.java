package org.application.repository;

import org.application.entity.FileChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FileChunkRepository extends JpaRepository<FileChunk, UUID> {
    List<FileChunk> findByFileIdOrderByChunkNumber(UUID fileId);
}
