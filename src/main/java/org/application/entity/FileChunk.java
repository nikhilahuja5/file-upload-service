package org.application.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Table(name="file_chunks")
@Entity
@Getter
@Setter
@NoArgsConstructor
public class FileChunk {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="file_id")
    private Files file;

    @Column(name="chunk_number",nullable = false)
    private int chunkNumber;

    @Column(name="etag")
    private String etag;

    @Column(name="size_bytes")
    private Long sizeBytes;

    @Column(name="created_at",nullable = false,updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate(){
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
    }
}
