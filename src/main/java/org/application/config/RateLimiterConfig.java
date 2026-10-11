package org.application.config;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ClientSideConfig;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimiterConfig {

    @Bean
    public RedisClient redisClient(@Value("${spring.data.redis.host}")String host,
        @Value("${spring.data.redis.port}")int port){
        return RedisClient.create("redis://"+host + ":" + port);
    }
    @Bean
    public StatefulRedisConnection<byte[],byte[]>redisConnection(RedisClient redisClient){
        return redisClient.connect(ByteArrayCodec.INSTANCE);
    }
    @Bean
    public ProxyManager<byte[]>proxyManager(StatefulRedisConnection<byte[],byte[]>connection){
        ClientSideConfig clientSideConfig = ClientSideConfig.getDefault()
                .withExpirationAfterWriteStrategy(
                        ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(10)));

        return LettuceBasedProxyManager.builderFor(connection)
                .withClientSideConfig(clientSideConfig).build();
    }
}
