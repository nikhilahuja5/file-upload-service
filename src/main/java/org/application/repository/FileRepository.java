package org.application.repository;

import org.application.entity.Files;
import org.application.entity.UploadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileRepository extends JpaRepository<Files, UUID> {
    Page<Files> findByUserIdAndDeletedAtIsNull(String userId, Pageable pageable);
    Optional<Files> findByUserIdAndIdempotencyKey(String userId,String idempotencyKey);

    @Modifying
    @Query(value = """
          UPDATE files SET status='RECONCILING', updated_at = now()
          WHERE id IN (
          SELECT id FROM files
          WHERE (status in ('UPLOADING','INITIATED')
          AND created_at < :cutoff) OR (status = 'RECONCILING' AND updated_at < :reclaimCutoff) 
          or (status = 'COMPLETING' AND updated_at < :reclaimCutoff)
          LIMIT :batchSize
          FOR UPDATE SKIP LOCKED
          )
          RETURNING id
          """,nativeQuery = true)
    List<UUID> claimStaleUploads(@Param("cutoff") OffsetDateTime cutoff,
                                 @Param("reclaimCutoff") OffsetDateTime reclaimCutoff,
                                 @Param("batchSize") int batchSize);


    @Modifying
    @Transactional
    @Query("""
            UPDATE Files f SET f.status = :to, f.updatedAt = CURRENT_TIMESTAMP
            WHERE f.id = :id AND f.status = :from 
            """)
    int transitionStatus(@Param("id")UUID id,
                         @Param("from")UploadStatus from,
                         @Param("to")UploadStatus to);

}


