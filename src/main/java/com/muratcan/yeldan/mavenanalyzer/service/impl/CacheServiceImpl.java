package com.muratcan.yeldan.mavenanalyzer.service.impl;

import com.muratcan.yeldan.mavenanalyzer.service.CacheService;
import com.muratcan.yeldan.mavenanalyzer.service.VulnerabilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

/**
 * Implementation of the CacheService interface for managing application caches
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CacheServiceImpl implements CacheService {

    private final CacheManager cacheManager;
    private final VulnerabilityService vulnerabilityService;

    /**
     * Clear all caches in the application
     */
    @Override
    public void clearAllCaches() {
        log.info("Clearing all application caches");
        clearVulnerabilityCaches();
        clearLicenseCaches();
        clearChartCaches();
        clearVersionEstimateCaches();
        log.info("All caches cleared successfully");
    }

    /**
     * Clear only vulnerability caches
     */
    @Override
    public void clearVulnerabilityCaches() {
        log.info("Clearing vulnerability caches");
        vulnerabilityService.clearAllVulnerabilityCaches();
    }

    /**
     * Clear only license caches
     */
    @Override
    @CacheEvict(value = {"licenseCache"}, allEntries = true)
    public void clearLicenseCaches() {
        log.info("Cleared license caches");
    }

    /**
     * Clear only chart caches
     */
    @Override
    @CacheEvict(value = {"chartCache", "chartDataCache"}, allEntries = true)
    public void clearChartCaches() {
        log.info("Cleared chart caches");
    }

    /**
     * Clear only version estimate caches
     */
    @Override
    @CacheEvict(value = {"versionEstimateCache"}, allEntries = true)
    public void clearVersionEstimateCaches() {
        log.info("Cleared version estimate caches");
    }
} 