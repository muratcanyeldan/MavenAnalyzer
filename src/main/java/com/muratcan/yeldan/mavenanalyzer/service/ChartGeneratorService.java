package com.muratcan.yeldan.mavenanalyzer.service;

import com.muratcan.yeldan.mavenanalyzer.dto.ChartResponse;
import com.muratcan.yeldan.mavenanalyzer.entity.DependencyAnalysis;

import java.util.List;

public interface ChartGeneratorService {

    /**
     * Generate dependency status pie chart
     *
     * @param analysis the dependency analysis entity
     * @return the chart response with path to the generated chart
     */
    ChartResponse generateDependencyStatusChart(DependencyAnalysis analysis);

    /**
     * Generate a comparison chart between two analyses
     *
     * @param previousAnalysis the previous dependency analysis
     * @param currentAnalysis  the current dependency analysis
     * @return the chart response with path to the generated chart
     */
    ChartResponse generateComparisonChart(DependencyAnalysis previousAnalysis, DependencyAnalysis currentAnalysis);

    /**
     * Generate a vulnerability status chart
     *
     * @param analysis the dependency analysis entity
     * @return the chart response with path to the generated chart
     */
    ChartResponse generateVulnerabilityChart(DependencyAnalysis analysis);

    /**
     * Generate a history trend chart for a project
     *
     * @param analyses list of analyses for a project
     * @return the chart response with path to the generated chart
     */
    ChartResponse generateHistoryTrendChart(List<DependencyAnalysis> analyses);
} 