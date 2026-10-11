package org.application.s3service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.application.entity.Files;
import org.application.entity.UploadStatus;
import org.application.repository.FileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class ReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationService.class);
    private static final int STALE_HOURS = 24;
    private static final int BATCH_SIZE = 50;
    private static final int RECLAIM_AFTER_MINUTES = 60;

    private final FileRepository fileRepository;
    private final S3StorageService s3StorageService;
    private final Counter uploadsFailedCounter;


    public ReconciliationService(FileRepository fileRepository, S3StorageService s3StorageService, MeterRegistry meterRegistry){
        this.fileRepository = fileRepository;
        this.s3StorageService = s3StorageService;
        this.uploadsFailedCounter = meterRegistry.counter("uploads.failed");
    }

    @Scheduled(fixedDelay = 15,timeUnit = TimeUnit.MINUTES)
    //@Scheduled(fixedDelay = 1000)
    @Transactional
    public void reconcileStaleUploads(){
        OffsetDateTime cutoff = OffsetDateTime.now().minusHours(STALE_HOURS);
        OffsetDateTime reclaimCutoff = OffsetDateTime.now().minusMinutes(RECLAIM_AFTER_MINUTES);
        List<UUID> claimedIds = fileRepository.claimStaleUploads(cutoff,reclaimCutoff,BATCH_SIZE);


        if(claimedIds.isEmpty()){
            return;
        }
        log.info("Reconciliation claimed{} stale upload(s) ",claimedIds.size());

        for(UUID id : claimedIds){
            reconcileOne(id);
        }

    }

    private void reconcileOne(UUID id){
        Files file = fileRepository.findById(id).orElse(null);
        if(file == null){
            return;
        }

        try{
            if(file.getS3Key() != null && file.getS3MultipartUploadId() != null){
                s3StorageService.abortMultipartUpload(file.getS3Key(),file.getS3MultipartUploadId());
            }
            file.setStatus(UploadStatus.FAILED);
            uploadsFailedCounter.increment();
            fileRepository.save(file);
            log.info("Reconciled stale upload {} -> Failed",id);
        }catch (Exception e){
            log.error("Failed to reconcile upload {}, leaving in RECONCILING for next run", id, e);
        }
    }
}