package com.muratcan.yeldan.mavenanalyzer.controller;

import com.muratcan.yeldan.mavenanalyzer.service.VulnerabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller for managing application caches
 */
@RestController
@RequestMapping("/cache")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Cache Management", description = "API endpoints for managing application caches")
public class CacheController {

    private final VulnerabilityService vulnerabilityService;
    private final CacheManager cacheManager;

    /**
     * Clear all caches in the application
     *
     * @return Response entity with success message
     */
    @DeleteMapping
    @Operation(
            summary = "Clear all caches",
            description = "Clear all application caches including vulnerability, license, chart, and version caches"
    )
    public ResponseEntity<Map<String, String>> clearAllCaches() {
        log.info("Clearing all application caches");

        // Clear vulnerability cache
        vulnerabilityService.clearAllVulnerabilityCaches();

        // Clear license cache
        clearCache("licenseCache");

        // Clear version estimate cache
        clearCache("versionEstimateCache");

        // Clear chart caches
        clearCache("chartCache");
        clearCache("chartDataCache");

        return ResponseEntity.ok(Map.of("message", "All application caches have been cleared"));
    }

    /**
     * Clear only vulnerability caches
     *
     * @return Response entity with success message
     */
    @DeleteMapping("/vulnerability")
    @Operation(
            summary = "Clear vulnerability cache",
            description = "Clear all cached vulnerability data"
    )
    public ResponseEntity<Map<String, String>> clearVulnerabilityCache() {
        log.info("Clearing vulnerability cache");
        vulnerabilityService.clearAllVulnerabilityCaches();
        return ResponseEntity.ok(Map.of("message", "Vulnerability cache has been cleared"));
    }

    /**
     * Clear only license caches
     *
     * @return Response entity with success message
     */
    @DeleteMapping("/license")
    @Operation(
            summary = "Clear license cache",
            description = "Clear all cached license data"
    )
    public ResponseEntity<Map<String, String>> clearLicenseCache() {
        log.info("Clearing license cache");
        clearCache("licenseCache");
        return ResponseEntity.ok(Map.of("message", "License cache has been cleared"));
    }

    /**
     * Clear only chart caches
     *
     * @return Response entity with success message
     */
    @DeleteMapping("/chart")
    @Operation(
            summary = "Clear chart cache",
            description = "Clear all cached chart data"
    )
    public ResponseEntity<Map<String, String>> clearChartCache() {
        log.info("Clearing chart cache");
        clearCache("chartCache");
        clearCache("chartDataCache");
        return ResponseEntity.ok(Map.of("message", "Chart cache has been cleared"));
    }

    /**
     * Clear only version estimate caches
     *
     * @return Response entity with success message
     */
    @DeleteMapping("/version")
    @Operation(
            summary = "Clear version cache",
            description = "Clear all cached version data"
    )
    public ResponseEntity<Map<String, String>> clearVersionCache() {
        log.info("Clearing version cache");
        clearCache("versionEstimateCache");
        return ResponseEntity.ok(Map.of("message", "Version cache has been cleared"));
    }

    /**
     * Helper method to clear a specific cache by name
     *
     * @param cacheName the name of the cache to clear
     */
    private void clearCache(String cacheName) {
        try {
            if (cacheManager.getCache(cacheName) != null) {
                cacheManager.getCache(cacheName).clear();
                log.debug("Cache '{}' cleared successfully", cacheName);
            } else {
                log.warn("Cache '{}' not found in cache manager", cacheName);
            }
        } catch (Exception e) {
            log.error("Error clearing cache '{}': {}", cacheName, e.getMessage(), e);
        }
    }
} 