package com.muratcan.yeldan.mavenanalyzer.service;

import com.muratcan.yeldan.mavenanalyzer.dto.ChartResponse;
import com.muratcan.yeldan.mavenanalyzer.dto.chart.BarChartDataResponse;
import com.muratcan.yeldan.mavenanalyzer.dto.chart.PieChartDataResponse;
import com.muratcan.yeldan.mavenanalyzer.entity.DependencyAnalysis;
import org.springframework.cache.annotation.Cacheable;

/**
 * Service to handle caching of chart data and images
 */
public interface ChartCacheService {

    /**
     * Get cached dependency update chart or generate a new one
     */
    @Cacheable(value = "chartCache", key = "'dependency-updates-' + #analysisId")
    ChartResponse getCachedDependencyUpdateChart(Long analysisId, DependencyAnalysis analysis);

    /**
     * Get cached vulnerability chart or generate a new one
     */
    @Cacheable(value = "chartCache", key = "'vulnerabilities-' + #analysisId")
    ChartResponse getCachedVulnerabilityChart(Long analysisId, DependencyAnalysis analysis);

    /**
     * Get cached license distribution chart or generate a new one
     */
    @Cacheable(value = "chartCache", key = "'license-distribution-' + #analysisId")
    ChartResponse getCachedLicenseDistributionChart(Long analysisId, DependencyAnalysis analysis);

    /**
     * Get cached dependency status chart data or generate new data
     */
    @Cacheable(value = "chartDataCache", key = "'dependency-status-' + #analysisId")
    PieChartDataResponse getCachedDependencyStatusChartData(Long analysisId, DependencyAnalysis analysis);

    /**
     * Get cached vulnerability status chart data or generate new data
     */
    @Cacheable(value = "chartDataCache", key = "'vulnerability-status-' + #analysisId")
    PieChartDataResponse getCachedVulnerabilityStatusChartData(Long analysisId, DependencyAnalysis analysis);

    /**
     * Get cached vulnerability severity chart data or generate new data
     */
    @Cacheable(value = "chartDataCache", key = "'vulnerability-severity-' + #analysisId")
    BarChartDataResponse getCachedVulnerabilitySeverityChartData(Long analysisId, DependencyAnalysis analysis);

    /**
     * Get cached license distribution chart data or generate new data
     */
    @Cacheable(value = "chartDataCache", key = "'license-distribution-' + #analysisId")
    PieChartDataResponse getCachedLicenseDistributionChartData(Long analysisId, DependencyAnalysis analysis);
} 