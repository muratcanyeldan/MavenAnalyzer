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

/**
 * Configuration for Redis caching with specific TTL values for different cache types
 */
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

    /**
     * Redis connection factory for connecting to Redis server
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.setPassword(redisPassword);
        }
        return new LettuceConnectionFactory(config);
    }

    /**
     * Cache manager configuration with different TTL values for different cache types
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Default cache configuration
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .entryTtl(Duration.ofHours(1))
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer())
                );

        // Cache configurations with different TTL values
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // License cache with very long TTL (365 days by default)
        // License information for a specific version of a dependency doesn't change
        cacheConfigurations.put("licenseCache", defaultCacheConfig
                .entryTtl(Duration.ofDays(licenseCacheTtlDays)));

        // Version estimate cache with moderately long TTL (30 days by default)
        // This is for estimated versions which may need updating less frequently
        cacheConfigurations.put("versionEstimateCache", defaultCacheConfig
                .entryTtl(Duration.ofDays(versionEstimateCacheTtlDays)));

        // Vulnerability cache with longer TTL (24 hours by default)
        // This avoids frequent external API calls for the same dependencies
        cacheConfigurations.put("vulnerabilities", defaultCacheConfig
                .entryTtl(Duration.ofHours(vulnerabilityCacheTtlHours)));

        // Specific TTL for vulnerability counts
        cacheConfigurations.put("vulnerabilityCounts", defaultCacheConfig
                .entryTtl(Duration.ofHours(vulnerabilityCacheTtlHours)));

        // Chart caches - keeping these shorter since charts are generated from data that might change
        cacheConfigurations.put("chartCache", defaultCacheConfig
                .entryTtl(Duration.ofMinutes(chartCacheTtlMinutes)));

        // Chart data caches
        cacheConfigurations.put("chartDataCache", defaultCacheConfig
                .entryTtl(Duration.ofMinutes(chartCacheTtlMinutes)));

        // Build and return the cache manager
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
} 