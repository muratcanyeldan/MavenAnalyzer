package com.muratcan.yeldan.mavenanalyzer.service;

/**
 * Service interface for managing application caches
 */
public interface CacheService {

    /**
     * Clear all caches in the application
     * This includes:
     * - Vulnerability caches
     * - License caches
     * - Chart caches
     * - Version estimate caches
     */
    void clearAllCaches();

    /**
     * Clear only vulnerability caches
     */
    void clearVulnerabilityCaches();

    /**
     * Clear only license caches
     */
    void clearLicenseCaches();

    /**
     * Clear only chart caches
     */
    void clearChartCaches();

    /**
     * Clear only version estimate caches
     */
    void clearVersionEstimateCaches();
} 