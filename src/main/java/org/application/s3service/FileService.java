package org.application.s3service;

import io.micrometer.core.instrument.MeterRegistry;
import org.application.exception.BadRequestException;
import org.application.dto.*;
import org.application.entity.FileChunk;
import org.application.entity.Files;
import org.application.entity.UploadStatus;
import org.application.exception.FileNotFoundException;
import org.application.exception.InvalidUploadStateException;
import org.application.repository.FileChunkRepository;
import org.application.repository.FileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class FileService {
    private final FileRepository fileRepository;
    private final FileChunkRepository fileChunkRepository;
    private final S3StorageService s3StorageService;
    private static final long MIN_PART_SIZE = 5 * 1024 * 1024;
    private final Counter uploadsInitiatedCounter;
    private final Counter uploadsCompletedCounter;
    private final Counter uploadsFailedCounter;
    private final Timer uploadCompletionTimer;
    private final TransactionTemplate transactionTemplate;
    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    public FileService(FileRepository fileRepository, FileChunkRepository fileChunkRepository,
                       S3StorageService s3StorageService, MeterRegistry meterRegistry,
                       TransactionTemplate transactionTemplate){
        this.fileRepository = fileRepository;
        this.fileChunkRepository = fileChunkRepository;
        this.s3StorageService = s3StorageService;
        this.uploadsInitiatedCounter = meterRegistry.counter("uploads.initiated");
        this.uploadsCompletedCounter = meterRegistry.counter("uploads.completed");
        this.uploadsFailedCounter = meterRegistry.counter("uploads.failed");
        this.uploadCompletionTimer = meterRegistry.timer("uploads.completion.duration");
        this.transactionTemplate = transactionTemplate;
    }

    public FileResponse initiateUpload(String userId,String idempotencyKey,InitiateUploadRequest request){
        if(idempotencyKey != null && !idempotencyKey.isBlank()){
            Optional<Files> existing = fileRepository.findByUserIdAndIdempotencyKey(userId,idempotencyKey);
            if(existing.isPresent()){
                return toResponse(existing.get());
            }
        }
        Files file = new Files();
        file.setUserId(userId);
        file.setFileName(request.fileName());
        file.setMimeType(request.mimeType());
        file.setTotalSizeBytes(request.totalSizeBytes());
        file.setStatus(UploadStatus.INITIATED);
        file.setIdempotencyKey(idempotencyKey);
        //file = fileRepository.save(file);

        try{
            file = fileRepository.saveAndFlush(file);
        }catch(DataIntegrityViolationException e){
            if(idempotencyKey==null || idempotencyKey.isBlank()) throw e;
            return fileRepository.findByUserIdAndIdempotencyKey(userId,idempotencyKey)
                    .map(this::toResponse).orElseThrow(() -> e);
        }

        String key = R2KeyGenerator.generatedKey(userId,file.getId(),request.fileName());
        String uploadId;
        try{
            uploadId = s3StorageService.startMultipartUpload(key);
        }catch(RuntimeException e){
            file.setStatus(UploadStatus.FAILED);
            fileRepository.save(file);
            uploadsFailedCounter.increment();
            throw e;
        }

        file.setS3Key(key);
        file.setS3MultipartUploadId(uploadId);
        file.setStatus(UploadStatus.UPLOADING);
        file = fileRepository.save(file);

        uploadsInitiatedCounter.increment();
        return toResponse(file);
    }

    public PreSignedUrlResponse presignPart(String userId, UUID fileId, int partNumber){
        if(partNumber < 1 || partNumber > 10_000){
            throw new BadRequestException("partNumber must be between 1 and 10000");
        }
        Files file = getOwnedFileOrThrow(userId,fileId);
        if(file.getStatus() != UploadStatus.UPLOADING){
            throw new InvalidUploadStateException("File is not uploading : " + file.getStatus());
        }
        String url = s3StorageService.generatePreSignedUrlForPart
                (file.getS3Key(),file.getS3MultipartUploadId(),partNumber);
        return new PreSignedUrlResponse(url);
    }
    public List<PartSummary> listUploadParts(String userId, UUID fileId){
        Files file = getOwnedFileOrThrow(userId,fileId);
        return s3StorageService.listParts(file.getS3Key(), file.getS3MultipartUploadId());
    }


    public FileResponse completeUpload(String userId, UUID fileId, CompleteUploadRequest request){
        return uploadCompletionTimer.record(()-> {
            Files file = getOwnedFileOrThrow(userId,fileId);
            int claimed = fileRepository.transitionStatus(fileId,UploadStatus.UPLOADING,UploadStatus.COMPLETING);
            if(claimed == 0){
                throw new InvalidUploadStateException("File " + fileId + " is not in UPLOADING state");
            }
            List<PartSummary> authoritativeParts;
            try {
                authoritativeParts = s3StorageService.listParts(file.getS3Key(), file.getS3MultipartUploadId());
                if (authoritativeParts.isEmpty()) {
                    throw new InvalidUploadStateException("No parts uploaded for file " + fileId);
                }
                validateParts(authoritativeParts);
                s3StorageService.completeMultipartUpload(
                        file.getS3Key(), file.getS3MultipartUploadId(), authoritativeParts);
            } catch (RuntimeException e) {
                fileRepository.transitionStatus(fileId, UploadStatus.COMPLETING, UploadStatus.UPLOADING);
                throw e;
            }

            Files completed = transactionTemplate.execute(status -> {
                for (PartSummary part : authoritativeParts) {
                    FileChunk chunk = new FileChunk();
                    chunk.setFile(file);
                    chunk.setChunkNumber(part.partNumber());
                    chunk.setEtag(part.eTag());
                    chunk.setSizeBytes(part.sizeBytes());
                    fileChunkRepository.save(chunk);
                }
                file.setStatus(UploadStatus.COMPLETED);
                return fileRepository.save(file);
            });

            uploadsCompletedCounter.increment();
            return toResponse(completed);

        });
    }

    public void abortUpload(String userId,UUID fileId){
        Files file = getOwnedFileOrThrow(userId,fileId);
        s3StorageService.abortMultipartUpload(file.getS3Key(),file.getS3MultipartUploadId());
        file.setStatus(UploadStatus.FAILED);
        fileRepository.save(file);
        uploadsFailedCounter.increment();
    }

    public PreSignedUrlResponse preSignDownload(String userId,UUID fileId){

        Files file = getOwnedFileOrThrow(userId,fileId);
        if(file.getStatus() != UploadStatus.COMPLETED){
            throw new InvalidUploadStateException("File is not ready for download:" + file.getStatus());
        }
        return new PreSignedUrlResponse(s3StorageService.generatePresignedGetUrl(file.getS3Key()));
    }

    public Page<FileResponse> listFiles(String userId, Pageable pageable){
        return fileRepository.findByUserIdAndDeletedAtIsNull(userId,pageable)
                .map(this::toResponse);
    }

    public void deleteFile(String userId,UUID fileId){
        Files file = getOwnedFileOrThrow(userId,fileId);
        file.setDeletedAt(OffsetDateTime.now());
        fileRepository.save(file);

        try{
            if(file.getStatus() == UploadStatus.COMPLETED){
                s3StorageService.deleteObject(file.getS3Key());
            }else if(file.getStatus() == UploadStatus.UPLOADING &&
                    file.getS3Key() != null && file.getS3MultipartUploadId() != null){
                  s3StorageService.abortMultipartUpload(file.getS3Key(),file.getS3MultipartUploadId());
            }

        }catch(Exception e){
            log.error("Failed to delete R2 object for file {} (key={}), object may be orphaned",
                    fileId,file.getS3Key(),e);
        }
    }

    private Files getOwnedFileOrThrow(String userId,UUID fileId){
        Files file = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File not Found : " + fileId));

        if(!file.getUserId().equals(userId)){
            throw new FileNotFoundException("File not found: " + fileId);
        }
        if(file.getDeletedAt() != null){
            throw new FileNotFoundException("File not found: " + fileId);
        }
        return file;
    }

    private FileResponse toResponse(Files file){
        return new FileResponse(file.getId(),file.getFileName(),file.getMimeType(),
                file.getTotalSizeBytes(),file.getStatus().name(),file.getCreatedAt());
    }

    private void validateParts(List<PartSummary> parts) {
        if (parts.size() > 10_000) {
            throw new InvalidUploadStateException("Upload has more than 10,000 parts");
        }
        int maxPartNumber = parts.stream().mapToInt(PartSummary::partNumber).max().orElseThrow();
        for (PartSummary part : parts) {
            boolean isLastPart = part.partNumber() == maxPartNumber;
            if (!isLastPart && part.sizeBytes() < MIN_PART_SIZE) {
                throw new InvalidUploadStateException(
                        "Part " + part.partNumber() + " is smaller than the 5MB minimum (all parts except the last must meet it)");
            }
        }
    }

}
