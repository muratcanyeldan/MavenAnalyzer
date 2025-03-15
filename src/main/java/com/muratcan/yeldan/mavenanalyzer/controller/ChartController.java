package com.muratcan.yeldan.mavenanalyzer.controller;

import com.muratcan.yeldan.mavenanalyzer.dto.ChartResponse;
import com.muratcan.yeldan.mavenanalyzer.dto.chart.BarChartDataResponse;
import com.muratcan.yeldan.mavenanalyzer.dto.chart.PieChartDataResponse;
import com.muratcan.yeldan.mavenanalyzer.entity.DependencyAnalysis;
import com.muratcan.yeldan.mavenanalyzer.exception.ResourceNotFoundException;
import com.muratcan.yeldan.mavenanalyzer.repository.DependencyAnalysisRepository;
import com.muratcan.yeldan.mavenanalyzer.service.ChartDataService;
import com.muratcan.yeldan.mavenanalyzer.service.ChartGeneratorService;
import com.muratcan.yeldan.mavenanalyzer.service.DependencyAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/charts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Charts", description = "API endpoints for generating and retrieving dependency analysis charts")
public class ChartController {

    private final ChartGeneratorService chartGeneratorService;
    private final ChartDataService chartDataService;
    private final DependencyAnalysisService dependencyAnalysisService;
    private final DependencyAnalysisRepository dependencyAnalysisRepository;

    //
    // Server-side rendered chart endpoints (original implementation)
    //

    @GetMapping("/dependency-updates/{analysisId}")
    @Operation(summary = "Get dependency updates chart", description = "Generate and retrieve a chart showing dependency update status")
    public ResponseEntity<ChartResponse> getDependencyUpdatesChart(@PathVariable Long analysisId) {
        log.info("Generating dependency updates chart for analysis ID: {}", analysisId);

        // Get the analysis
        DependencyAnalysis analysis = getDependencyAnalysisEntityById(analysisId);

        // Generate the chart
        ChartResponse chartResponse = getOrGenerateDependencyUpdateChart(analysisId, analysis);

        // Extract just the filename from the path
        String fullPath = chartResponse.getChartPath();
        String fileName = fullPath.substring(fullPath.lastIndexOf('/') + 1);
        chartResponse.setChartPath(fileName);

        return ResponseEntity.ok(chartResponse);
    }

    // Cache the chart response directly, not the ResponseEntity
    @Cacheable(value = "chartCache", key = "'dependency-updates-' + #analysisId")
    private ChartResponse getOrGenerateDependencyUpdateChart(Long analysisId, DependencyAnalysis analysis) {
        return chartGeneratorService.generateDependencyStatusChart(analysis);
    }

    @GetMapping("/vulnerabilities/{analysisId}")
    @Operation(summary = "Get vulnerabilities chart", description = "Generate and retrieve a chart showing vulnerability status")
    public ResponseEntity<ChartResponse> getVulnerabilitiesChart(@PathVariable Long analysisId) {
        log.info("Generating vulnerabilities chart for analysis ID: {}", analysisId);

        // Get the analysis
        DependencyAnalysis analysis = getDependencyAnalysisEntityById(analysisId);

        // Generate the chart
        ChartResponse chartResponse = getOrGenerateVulnerabilityChart(analysisId, analysis);

        // Extract just the filename from the path
        String fullPath = chartResponse.getChartPath();
        String fileName = fullPath.substring(fullPath.lastIndexOf('/') + 1);
        chartResponse.setChartPath(fileName);

        return ResponseEntity.ok(chartResponse);
    }

    // Cache the chart response directly, not the ResponseEntity
    @Cacheable(value = "chartCache", key = "'vulnerabilities-' + #analysisId")
    private ChartResponse getOrGenerateVulnerabilityChart(Long analysisId, DependencyAnalysis analysis) {
        return chartGeneratorService.generateVulnerabilityChart(analysis);
    }

    @GetMapping("/license-distribution/{analysisId}")
    @Operation(summary = "Get license distribution chart", description = "Generate and retrieve a chart showing license distribution")
    public ResponseEntity<ChartResponse> getLicenseDistributionChart(@PathVariable Long analysisId) {
        log.info("Generating license distribution chart for analysis ID: {}", analysisId);

        // Get the analysis
        DependencyAnalysis analysis = getDependencyAnalysisEntityById(analysisId);

        // Generate the chart
        ChartResponse chartResponse = getOrGenerateLicenseDistributionChart(analysisId, analysis);
        chartResponse.setTitle("License Distribution");
        chartResponse.setDescription("Distribution of licenses used by dependencies in the project");

        // Extract just the filename from the path
        String fullPath = chartResponse.getChartPath();
        String fileName = fullPath.substring(fullPath.lastIndexOf('/') + 1);
        chartResponse.setChartPath(fileName);

        return ResponseEntity.ok(chartResponse);
    }

    // Cache the chart response directly, not the ResponseEntity
    @Cacheable(value = "chartCache", key = "'license-distribution-' + #analysisId")
    private ChartResponse getOrGenerateLicenseDistributionChart(Long analysisId, DependencyAnalysis analysis) {
        // For demo purposes, we'll reuse the dependency status chart since we don't have a dedicated license chart yet
        return chartGeneratorService.generateDependencyStatusChart(analysis);
    }

    @GetMapping("/image/{fileName:.+}")
    @Operation(summary = "Get chart image", description = "Retrieve a chart image by its file name")
    public ResponseEntity<Resource> getChartImage(@PathVariable String fileName) {
        log.info("Retrieving chart image: {}", fileName);

        try {
            // Get the chart file
            Path filePath = Paths.get("charts").resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_PNG)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                log.warn("Chart image not found: {}", fileName);
                return ResponseEntity.notFound().build();
            }
        } catch (IOException e) {
            log.error("Error retrieving chart image", e);
            return ResponseEntity.badRequest().build();
        }
    }

    //
    // Client-side rendering chart data endpoints (new implementation)
    //

    @GetMapping("/data/dependency-status/{analysisId}")
    @Operation(summary = "Get dependency status chart data",
            description = "Retrieve raw data for client-side rendering of dependency status chart")
    public ResponseEntity<PieChartDataResponse> getDependencyStatusChartData(@PathVariable Long analysisId) {
        log.info("Generating dependency status chart data for analysis ID: {}", analysisId);

        // Get the analysis
        DependencyAnalysis analysis = getDependencyAnalysisEntityById(analysisId);

        // Generate the chart data
        PieChartDataResponse chartData = getOrGenerateDependencyStatusChartData(analysisId, analysis);

        return ResponseEntity.ok(chartData);
    }

    // Cache the chart data directly, not the ResponseEntity
    @Cacheable(value = "chartDataCache", key = "'dependency-status-' + #analysisId")
    private PieChartDataResponse getOrGenerateDependencyStatusChartData(Long analysisId, DependencyAnalysis analysis) {
        return chartDataService.generateDependencyStatusChartData(analysis);
    }

    @GetMapping("/data/vulnerability-status/{analysisId}")
    @Operation(summary = "Get vulnerability status chart data",
            description = "Retrieve raw data for client-side rendering of vulnerability status chart")
    public ResponseEntity<PieChartDataResponse> getVulnerabilityStatusChartData(@PathVariable Long analysisId) {
        log.info("Generating vulnerability status chart data for analysis ID: {}", analysisId);

        // Get the analysis
        DependencyAnalysis analysis = getDependencyAnalysisEntityById(analysisId);

        // Generate the chart data
        PieChartDataResponse chartData = getOrGenerateVulnerabilityStatusChartData(analysisId, analysis);

        return ResponseEntity.ok(chartData);
    }

    // Cache the chart data directly, not the ResponseEntity
    @Cacheable(value = "chartDataCache", key = "'vulnerability-status-' + #analysisId")
    private PieChartDataResponse getOrGenerateVulnerabilityStatusChartData(Long analysisId, DependencyAnalysis analysis) {
        return chartDataService.generateVulnerabilityStatusChartData(analysis);
    }

    @GetMapping("/data/vulnerability-severity/{analysisId}")
    @Operation(summary = "Get vulnerability severity chart data",
            description = "Retrieve raw data for client-side rendering of vulnerability severity chart")
    public ResponseEntity<BarChartDataResponse> getVulnerabilitySeverityChartData(@PathVariable Long analysisId) {
        log.info("Generating vulnerability severity chart data for analysis ID: {}", analysisId);

        // Get the analysis
        DependencyAnalysis analysis = getDependencyAnalysisEntityById(analysisId);

        // Generate the chart data
        BarChartDataResponse chartData = getOrGenerateVulnerabilitySeverityChartData(analysisId, analysis);

        return ResponseEntity.ok(chartData);
    }

    // Cache the chart data directly, not the ResponseEntity
    @Cacheable(value = "chartDataCache", key = "'vulnerability-severity-' + #analysisId")
    private BarChartDataResponse getOrGenerateVulnerabilitySeverityChartData(Long analysisId, DependencyAnalysis analysis) {
        return chartDataService.generateVulnerabilitySeverityChartData(analysis);
    }

    @GetMapping("/data/license-distribution/{analysisId}")
    @Operation(summary = "Get license distribution chart data",
            description = "Retrieve raw data for client-side rendering of license distribution chart")
    public ResponseEntity<PieChartDataResponse> getLicenseDistributionChartData(@PathVariable Long analysisId) {
        log.info("Generating license distribution chart data for analysis ID: {}", analysisId);

        // Get the analysis
        DependencyAnalysis analysis = getDependencyAnalysisEntityById(analysisId);

        // Generate the chart data
        PieChartDataResponse chartData = getOrGenerateLicenseDistributionChartData(analysisId, analysis);

        return ResponseEntity.ok(chartData);
    }

    // Cache the chart data directly, not the ResponseEntity
    @Cacheable(value = "chartDataCache", key = "'license-distribution-' + #analysisId")
    private PieChartDataResponse getOrGenerateLicenseDistributionChartData(Long analysisId, DependencyAnalysis analysis) {
        return chartDataService.generateLicenseDistributionChartData(analysis);
    }

    /**
     * Helper method to get dependency analysis entity by ID
     */
    private DependencyAnalysis getDependencyAnalysisEntityById(Long analysisId) {
        return dependencyAnalysisRepository.findById(analysisId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis not found with ID: " + analysisId));
    }
} 