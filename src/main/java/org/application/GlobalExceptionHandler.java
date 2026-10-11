package org.application;

import org.application.dto.ErrorResponse;
import org.application.exception.FileNotFoundException;
import org.application.exception.InvalidUploadStateException;
import org.application.exception.RateLimitExceededException;
import org.slf4j.LoggerFactory;

import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.stream.Collectors;
import org.slf4j.MDC;


import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import software.amazon.awssdk.core.exception.SdkException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleFileNotFound(FileNotFoundException ex){
        String requestId = MDC.get("requestId");
        log.warn("[{}] FILE_NOT_FOUND: {}", requestId, ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("FILE_NOT_FOUND",ex.getMessage(),requestId));
    }

    @ExceptionHandler(InvalidUploadStateException.class)
    public ResponseEntity<ErrorResponse>handleInvalidUploadState(InvalidUploadStateException ex){
        String requestId = MDC.get("requestId");
        log.warn("[{}] INVALID_UPLOAD_STATE: {}", requestId, ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("INVALID_UPLOAD_STATE", ex.getMessage(), requestId));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String requestId = MDC.get("requestId");
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("[{}] VALIDATION_ERROR: {}", requestId, message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_ERROR", message, requestId));
    }

    @ExceptionHandler(SdkException.class)
    public ResponseEntity<ErrorResponse> handleStorageError(SdkException ex) {
        String requestId = MDC.get("requestId");
        log.error("[{}] UPSTREAM_STORAGE_ERROR", requestId, ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ErrorResponse("UPSTREAM_STORAGE_ERROR", "The storage backend is currently unavailable", requestId));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        String requestId = MDC.get("requestId");
        log.error("[{}] INTERNAL_ERROR", requestId, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred", requestId));
    }
    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse>handleRateLimitExceeded(RateLimitExceededException ex){
        String requestId = MDC.get("requestId");
        log.warn("[{}] RATE_LIMIT_EXCEEDED: {}", requestId, ex.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new ErrorResponse("RATE_LIMIT_EXCEEDED",ex.getMessage(),requestId));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse>handleResourceNotFound(NoResourceFoundException ex){
        String requestId = MDC.get("requestId");
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("RESOURCE_NOT_FOUND", "Not found", requestId));
    }
}
