package com.muratcan.yeldan.mavenanalyzer.service;

import com.muratcan.yeldan.mavenanalyzer.dto.response.ChartResponse;
import com.muratcan.yeldan.mavenanalyzer.entity.DependencyAnalysis;

public interface ChartGeneratorService {

    /**
     * Generate dependency status pie chart
     *
     * @param analysis the dependency analysis entity
     * @return the chart response with path to the generated chart
     */
    ChartResponse generateDependencyStatusChart(DependencyAnalysis analysis);

    /**
     * Generate a vulnerability status chart
     *
     * @param analysis the dependency analysis entity
     * @return the chart response with path to the generated chart
     */
    ChartResponse generateVulnerabilityChart(DependencyAnalysis analysis);
} 