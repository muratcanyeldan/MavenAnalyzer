package com.muratcan.yeldan.mavenanalyzer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisCacheConfig {

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Value("${spring.redis.password:}")
    private String redisPassword;

    @Value("${vulnerability.cache.ttl.hours:24}")
    private int vulnerabilityCacheTtlHours;

    @Value("${chart.cache.ttl.minutes:30}")
    private int chartCacheTtlMinutes;

    @Value("${license.cache.ttl.days:365}")
    private int licenseCacheTtlDays;

    @Value("${version.estimate.cache.ttl.days:30}")
    private int versionEstimateCacheTtlDays;


    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.setPassword(redisPassword);
        }
        return new LettuceConnectionFactory(config);
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .entryTtl(Duration.ofHours(1))
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer())
                );

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        cacheConfigurations.put("licenseCache", defaultCacheConfig
                .entryTtl(Duration.ofDays(licenseCacheTtlDays)));

        cacheConfigurations.put("versionEstimateCache", defaultCacheConfig
                .entryTtl(Duration.ofDays(versionEstimateCacheTtlDays)));

        cacheConfigurations.put("vulnerabilities", defaultCacheConfig
                .entryTtl(Duration.ofHours(vulnerabilityCacheTtlHours)));

        cacheConfigurations.put("vulnerabilityCounts", defaultCacheConfig
                .entryTtl(Duration.ofHours(vulnerabilityCacheTtlHours)));

        cacheConfigurations.put("chartCache", defaultCacheConfig
                .entryTtl(Duration.ofMinutes(chartCacheTtlMinutes)));

        cacheConfigurations.put("chartDataCache", defaultCacheConfig
                .entryTtl(Duration.ofMinutes(chartCacheTtlMinutes)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
} 