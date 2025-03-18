package com.muratcan.yeldan.mavenanalyzer.service.impl;

import com.muratcan.yeldan.mavenanalyzer.dto.chart.BarChartDataResponse;
import com.muratcan.yeldan.mavenanalyzer.dto.chart.PieChartDataResponse;
import com.muratcan.yeldan.mavenanalyzer.dto.response.ChartResponse;
import com.muratcan.yeldan.mavenanalyzer.entity.DependencyAnalysis;
import com.muratcan.yeldan.mavenanalyzer.service.ChartCacheService;
import com.muratcan.yeldan.mavenanalyzer.service.ChartDataService;
import com.muratcan.yeldan.mavenanalyzer.service.ChartGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChartCacheServiceImpl implements ChartCacheService {

    private final ChartGeneratorService chartGeneratorService;
    private final ChartDataService chartDataService;

    @Override
    @Cacheable(value = "chartCache", key = "'dependency-updates-' + #analysisId")
    public ChartResponse getCachedDependencyUpdateChart(Long analysisId, DependencyAnalysis analysis) {
        log.debug("Generating or retrieving cached dependency update chart for analysis ID: {}", analysisId);
        return chartGeneratorService.generateDependencyStatusChart(analysis);
    }

    @Override
    @Cacheable(value = "chartCache", key = "'vulnerabilities-' + #analysisId")
    public ChartResponse getCachedVulnerabilityChart(Long analysisId, DependencyAnalysis analysis) {
        log.debug("Generating or retrieving cached vulnerability chart for analysis ID: {}", analysisId);
        return chartGeneratorService.generateVulnerabilityChart(analysis);
    }

    @Override
    @Cacheable(value = "chartCache", key = "'license-distribution-' + #analysisId")
    public ChartResponse getCachedLicenseDistributionChart(Long analysisId, DependencyAnalysis analysis) {
        log.debug("Generating or retrieving cached license distribution chart for analysis ID: {}", analysisId);
        return chartGeneratorService.generateDependencyStatusChart(analysis);
    }

    @Override
    @Cacheable(value = "chartDataCache", key = "'dependency-status-' + #analysisId")
    public PieChartDataResponse getCachedDependencyStatusChartData(Long analysisId, DependencyAnalysis analysis) {
        log.debug("Generating or retrieving cached dependency status chart data for analysis ID: {}", analysisId);
        return chartDataService.generateDependencyStatusChartData(analysis);
    }

    @Override
    @Cacheable(value = "chartDataCache", key = "'vulnerability-status-' + #analysisId")
    public PieChartDataResponse getCachedVulnerabilityStatusChartData(Long analysisId, DependencyAnalysis analysis) {
        log.debug("Generating or retrieving cached vulnerability status chart data for analysis ID: {}", analysisId);
        return chartDataService.generateVulnerabilityStatusChartData(analysis);
    }

    @Override
    @Cacheable(value = "chartDataCache", key = "'vulnerability-severity-' + #analysisId")
    public BarChartDataResponse getCachedVulnerabilitySeverityChartData(Long analysisId, DependencyAnalysis analysis) {
        log.debug("Generating or retrieving cached vulnerability severity chart data for analysis ID: {}", analysisId);
        return chartDataService.generateVulnerabilitySeverityChartData(analysis);
    }

    @Override
    @Cacheable(value = "chartDataCache", key = "'license-distribution-' + #analysisId")
    public PieChartDataResponse getCachedLicenseDistributionChartData(Long analysisId, DependencyAnalysis analysis) {
        log.debug("Generating or retrieving cached license distribution chart data for analysis ID: {}", analysisId);
        return chartDataService.generateLicenseDistributionChartData(analysis);
    }
} 