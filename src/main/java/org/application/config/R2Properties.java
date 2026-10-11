package org.application.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "r2")
public record R2Properties(
        String accountId,
        String accessKey,
        String secretKey,
        String bucketName,
        String endpoint,
        @DefaultValue("false") boolean pathStyleAccess
) {}
