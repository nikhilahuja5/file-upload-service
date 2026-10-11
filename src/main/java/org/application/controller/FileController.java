package org.application.controller;

import jakarta.validation.Valid;
import org.application.s3service.RateLimitService;
import org.application.dto.*;
import org.application.exception.RateLimitExceededException;
import org.application.s3service.FileService;
import org.application.security.UserContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;
    private final UserContext userContext;
    private final RateLimitService rateLimitService;

    public FileController(FileService fileService, UserContext userContext,RateLimitService rateLimitService){
        this.fileService = fileService;
        this.userContext = userContext;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping
    public ResponseEntity<FileResponse> initiateUpload(
            @RequestHeader(value= "Idempotency-Key",required=false)String idempotencyKey,
            @Valid @RequestBody InitiateUploadRequest request){
        if(!rateLimitService.tryConsume(userContext.getUserId(),"initiate-upload",10, Duration.ofMinutes(1))){
            throw new RateLimitExceededException("Too many upload initiation,slow down");
        }
        FileResponse response = fileService.initiateUpload(userContext.getUserId(),idempotencyKey,request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{FileId}/parts/{partNumber}/presign")
    public PreSignedUrlResponse presignPart(
            @PathVariable UUID FileId,
            @PathVariable int partNumber){
        if(!rateLimitService.tryConsume(userContext.getUserId(),"presign-part",100, Duration.ofMinutes(1))){
            throw new RateLimitExceededException("Too many upload initiation,slow down");
        }
        return fileService.presignPart(userContext.getUserId(),FileId,partNumber);
    }

    @GetMapping("/{fileId}/parts")
    public List<PartSummary> listParts(
            @PathVariable UUID fileId){
        return fileService.listUploadParts(userContext.getUserId(),fileId);
    }

    @PostMapping("/{fileId}/complete")
    public FileResponse completeUpload(
            @PathVariable UUID fileId,
            @Valid @RequestBody CompleteUploadRequest request){
        return fileService.completeUpload(userContext.getUserId(),fileId,request);
    }

    @PostMapping("/{fileId}/abort")
    public ResponseEntity<Void>abortUpload(
            @PathVariable UUID fileId){
        fileService.abortUpload(userContext.getUserId(),fileId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{FileId}/download")
    public PreSignedUrlResponse downloadFile(
            @PathVariable UUID FileId){
        return fileService.preSignDownload(userContext.getUserId(),FileId);
    }

    @GetMapping
    public Page<FileResponse> listFiles(
            @PageableDefault(size=20,sort="createdAt",direction = Sort.Direction.DESC)Pageable pageable){
           return fileService.listFiles(userContext.getUserId(),pageable);
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void>deleteFile(
            @PathVariable UUID fileId){
        fileService.deleteFile(userContext.getUserId(),fileId);
        return ResponseEntity.noContent().build();
    }
}
