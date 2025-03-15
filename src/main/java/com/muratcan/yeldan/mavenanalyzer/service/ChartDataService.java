package com.muratcan.yeldan.mavenanalyzer.service;

import com.muratcan.yeldan.mavenanalyzer.dto.chart.BarChartDataResponse;
import com.muratcan.yeldan.mavenanalyzer.dto.chart.PieChartDataResponse;
import com.muratcan.yeldan.mavenanalyzer.entity.DependencyAnalysis;

/**
 * Service for generating chart data for client-side rendering
 */
public interface ChartDataService {

    /**
     * Generate dependency status pie chart data
     *
     * @param analysis the dependency analysis entity
     * @return the chart data response
     */
    PieChartDataResponse generateDependencyStatusChartData(DependencyAnalysis analysis);

    /**
     * Generate vulnerability status pie chart data
     *
     * @param analysis the dependency analysis entity
     * @return the chart data response
     */
    PieChartDataResponse generateVulnerabilityStatusChartData(DependencyAnalysis analysis);

    /**
     * Generate vulnerability severity bar chart data
     *
     * @param analysis the dependency analysis entity
     * @return the chart data response
     */
    BarChartDataResponse generateVulnerabilitySeverityChartData(DependencyAnalysis analysis);

    /**
     * Generate license distribution pie chart data
     *
     * @param analysis the dependency analysis entity
     * @return the chart data response
     */
    PieChartDataResponse generateLicenseDistributionChartData(DependencyAnalysis analysis);
} 