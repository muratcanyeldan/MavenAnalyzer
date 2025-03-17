package com.muratcan.yeldan.mavenanalyzer.service;

import com.muratcan.yeldan.mavenanalyzer.dto.chart.BarChartDataResponse;
import com.muratcan.yeldan.mavenanalyzer.dto.chart.PieChartDataResponse;
import com.muratcan.yeldan.mavenanalyzer.dto.response.ChartResponse;
import com.muratcan.yeldan.mavenanalyzer.entity.DependencyAnalysis;

/**
 * Service to handle caching of chart data and images
 */
public interface ChartCacheService {

    /**
     * Get cached dependency update chart or generate a new one
     */
    ChartResponse getCachedDependencyUpdateChart(Long analysisId, DependencyAnalysis analysis);

    /**
     * Get cached vulnerability chart or generate a new one
     */
    ChartResponse getCachedVulnerabilityChart(Long analysisId, DependencyAnalysis analysis);

    /**
     * Get cached license distribution chart or generate a new one
     */
    ChartResponse getCachedLicenseDistributionChart(Long analysisId, DependencyAnalysis analysis);

    /**
     * Get cached dependency status chart data or generate new data
     */
    PieChartDataResponse getCachedDependencyStatusChartData(Long analysisId, DependencyAnalysis analysis);

    /**
     * Get cached vulnerability status chart data or generate new data
     */
    PieChartDataResponse getCachedVulnerabilityStatusChartData(Long analysisId, DependencyAnalysis analysis);

    /**
     * Get cached vulnerability severity chart data or generate new data
     */
    BarChartDataResponse getCachedVulnerabilitySeverityChartData(Long analysisId, DependencyAnalysis analysis);

    /**
     * Get cached license distribution chart data or generate new data
     */
    PieChartDataResponse getCachedLicenseDistributionChartData(Long analysisId, DependencyAnalysis analysis);
} 