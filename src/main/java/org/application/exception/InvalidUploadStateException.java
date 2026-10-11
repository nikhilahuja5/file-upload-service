package org.application.exception;

public class InvalidUploadStateException extends RuntimeException {
    public InvalidUploadStateException(String message) {
        super(message);
    }
}
