package com.muratcan.yeldan.mavenanalyzer.service;

public interface CacheManagementService {

    void clearAllCaches();

    void clearVulnerabilityCaches();

    void clearLicenseCaches();

    void clearChartCaches();

    void clearVersionEstimateCaches();
} 