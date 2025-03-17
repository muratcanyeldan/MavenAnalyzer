package com.muratcan.yeldan.mavenanalyzer.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Getter
@Setter
public class DynamicCacheProperties {

    private boolean licenseCacheEnabled;
    private boolean versionEstimateCacheEnabled;
    private boolean vulnerabilityCacheEnabled;

    public DynamicCacheProperties(
            @Value("${license.cache.enabled:true}") boolean licenseCacheEnabled,
            @Value("${version.estimate.cache.enabled:true}") boolean versionEstimateCacheEnabled,
            @Value("${vulnerability.cache.enabled:true}") boolean vulnerabilityCacheEnabled) {

        this.licenseCacheEnabled = licenseCacheEnabled;
        this.versionEstimateCacheEnabled = versionEstimateCacheEnabled;
        this.vulnerabilityCacheEnabled = vulnerabilityCacheEnabled;

        log.info("Initialized dynamic cache properties: license={}, versionEstimate={}, vulnerability={}",
                licenseCacheEnabled, versionEstimateCacheEnabled, vulnerabilityCacheEnabled);
    }

    public void updateAllCacheProperties(boolean enabled) {
        this.licenseCacheEnabled = enabled;
        this.versionEstimateCacheEnabled = enabled;
        this.vulnerabilityCacheEnabled = enabled;

        log.info("Updated all cache properties to: {}", enabled);
    }
} 