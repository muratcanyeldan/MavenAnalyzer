package com.muratcan.yeldan.mavenanalyzer.controller;

import com.muratcan.yeldan.mavenanalyzer.config.DynamicCacheProperties;
import com.muratcan.yeldan.mavenanalyzer.dto.response.ApiResponse;
import com.muratcan.yeldan.mavenanalyzer.service.CacheManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Cache Management", description = "Endpoints for managing application caches")
public class CacheController {

    private final CacheManagementService cacheManagementService;
    private final DynamicCacheProperties cacheProperties;

    @DeleteMapping
    @Operation(summary = "Clear all caches", description = "Clears all application caches")
    public ResponseEntity<ApiResponse> clearAllCaches() {
        log.info("Request to clear all caches");
        cacheManagementService.clearAllCaches();
        return ResponseEntity.ok(new ApiResponse(true, "All caches cleared successfully"));
    }

    @DeleteMapping("/vulnerability")
    @Operation(summary = "Clear vulnerability caches", description = "Clears vulnerability-related caches")
    public ResponseEntity<ApiResponse> clearVulnerabilityCaches() {
        log.info("Request to clear vulnerability caches");
        cacheManagementService.clearVulnerabilityCaches();
        return ResponseEntity.ok(new ApiResponse(true, "Vulnerability caches cleared successfully"));
    }

    @DeleteMapping("/license")
    @Operation(summary = "Clear license caches", description = "Clears license-related caches")
    public ResponseEntity<ApiResponse> clearLicenseCaches() {
        log.info("Request to clear license caches");
        cacheManagementService.clearLicenseCaches();
        return ResponseEntity.ok(new ApiResponse(true, "License caches cleared successfully"));
    }

    @DeleteMapping("/chart")
    @Operation(summary = "Clear chart caches", description = "Clears chart-related caches")
    public ResponseEntity<ApiResponse> clearChartCaches() {
        log.info("Request to clear chart caches");
        cacheManagementService.clearChartCaches();
        return ResponseEntity.ok(new ApiResponse(true, "Chart caches cleared successfully"));
    }

    @DeleteMapping("/version")
    @Operation(summary = "Clear version estimate caches", description = "Clears version estimate caches")
    public ResponseEntity<ApiResponse> clearVersionEstimateCaches() {
        log.info("Request to clear version estimate caches");
        cacheManagementService.clearVersionEstimateCaches();
        return ResponseEntity.ok(new ApiResponse(true, "Version estimate caches cleared successfully"));
    }

    @GetMapping("/status")
    @Operation(summary = "Get cache status", description = "Returns the current status of caching")
    public ResponseEntity<ApiResponse> getCacheStatus() {
        boolean licenseCache = cacheProperties.isLicenseCacheEnabled();
        boolean versionEstimateCache = cacheProperties.isVersionEstimateCacheEnabled();
        boolean vulnerabilityCache = cacheProperties.isVulnerabilityCacheEnabled();

        String status = String.format(
                "Cache Status - License: %s, Version Estimate: %s, Vulnerability: %s",
                licenseCache, versionEstimateCache, vulnerabilityCache);

        log.info("Cache status: {}", status);
        return ResponseEntity.ok(new ApiResponse(true, status));
    }

    @PutMapping("/toggle")
    @Operation(summary = "Toggle caching", description = "Enable or disable all caching")
    public ResponseEntity<ApiResponse> toggleCaching(@RequestParam boolean enabled) {
        log.info("Request to {} caching", enabled ? "enable" : "disable");

        cacheProperties.updateAllCacheProperties(enabled);

        if (!enabled) {
            cacheManagementService.clearAllCaches();
        }

        return ResponseEntity.ok(new ApiResponse(true,
                String.format("Caching %s successfully", enabled ? "enabled" : "disabled")));
    }
} 