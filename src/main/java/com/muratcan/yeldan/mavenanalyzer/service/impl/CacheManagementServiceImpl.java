package com.muratcan.yeldan.mavenanalyzer.service.impl;

import com.muratcan.yeldan.mavenanalyzer.service.CacheManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CacheManagementServiceImpl implements CacheManagementService {

    private static final List<String> VULNERABILITY_CACHES = Arrays.asList(
            "vulnerabilities", "vulnerabilityCounts"
    );
    private static final List<String> LICENSE_CACHES = Arrays.asList(
            "licenseCache"
    );
    private static final List<String> CHART_CACHES = Arrays.asList(
            "chartCache", "chartDataCache"
    );
    private static final List<String> VERSION_ESTIMATE_CACHES = Arrays.asList(
            "versionEstimateCache"
    );
    private final CacheManager cacheManager;

    @Override
    public void clearAllCaches() {
        log.info("Clearing all application caches");
        cacheManager.getCacheNames().forEach(this::clearCache);
    }

    @Override
    public void clearVulnerabilityCaches() {
        log.info("Clearing vulnerability caches");
        VULNERABILITY_CACHES.forEach(this::clearCache);
    }

    @Override
    public void clearLicenseCaches() {
        log.info("Clearing license caches");
        LICENSE_CACHES.forEach(this::clearCache);
    }

    @Override
    public void clearChartCaches() {
        log.info("Clearing chart caches");
        CHART_CACHES.forEach(this::clearCache);
    }

    @Override
    public void clearVersionEstimateCaches() {
        log.info("Clearing version estimate caches");
        VERSION_ESTIMATE_CACHES.forEach(this::clearCache);
    }

    private void clearCache(String cacheName) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            log.debug("Clearing cache: {}", cacheName);
            cache.clear();
        } else {
            log.warn("Cache not found: {}", cacheName);
        }
    }
} 