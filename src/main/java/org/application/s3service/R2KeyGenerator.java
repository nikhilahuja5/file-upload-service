package org.application.s3service;

import java.util.UUID;

public class R2KeyGenerator {
    private static final int MAX_FILENAME_LENGTH = 255;

    private R2KeyGenerator(){};

    public static String generatedKey(String userId, UUID fileId,String originalFileName){
        String sanitized = sanitize(originalFileName);
        return userId + "/" + fileId + "/" + sanitized;
    }

    private static String sanitize(String fileName){
        String cleaned = fileName.replaceAll("[^A-Za-z0-9._-]","_");
        if (cleaned.length() > MAX_FILENAME_LENGTH){
            cleaned = cleaned.substring(cleaned.length()-MAX_FILENAME_LENGTH);
        }
        return cleaned;
    }
}
