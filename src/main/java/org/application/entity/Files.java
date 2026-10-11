package org.application.entity;
import lombok.Getter;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.UUID;

@Table(name="files")
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Files
{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name="user_id" , nullable = false)
    private String userId;

    @Column(name="file_name",nullable = false)
    private String fileName;

    @Column(name="mime_type",nullable = false)
    private String mimeType;

    @Column(name="total_size_bytes",nullable = false)
    private Long totalSizeBytes;

    @Column(name="s3_key")
    private String s3Key;

    @Column(name="s3_multipart_upload_id")
    private String s3MultipartUploadId;

    @Column(name="checksum")
    private String checksum;

    @Enumerated(EnumType.STRING)
    @Column(name="status",nullable = false)
    private UploadStatus status = UploadStatus.INITIATED;

    @Column(name="created_at",nullable = false,updatable = false)
    private OffsetDateTime createdAt;

    @Column(name="updated_at",nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name="deleted_at")
    private OffsetDateTime deletedAt;

    @Column(name="idempotency_key")
    private String idempotencyKey;

    @PrePersist
    protected void onCreate(){
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate(){
        this.updatedAt = OffsetDateTime.now();
    }
}
