package org.application.s3service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Supplier;

@Service
public class RateLimitService {
    private final ProxyManager<byte[]>proxyManager;

    public RateLimitService(ProxyManager<byte[]>proxyManager){
        this.proxyManager = proxyManager;
    }

    public boolean  tryConsume(String userId, String action, int capacity, Duration refillPeriod){
        byte[] key = (action + ":" + userId).getBytes(StandardCharsets.UTF_8);

        Supplier<BucketConfiguration> configSupplier = () -> BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(capacity)
                        .refillGreedy(capacity,refillPeriod)
                        .build())
                .build();
        Bucket bucket = proxyManager.builder().build(key, configSupplier);
        return bucket.tryConsumeAndReturnRemaining(1).isConsumed();
    }
}
