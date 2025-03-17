package com.muratcan.yeldan.mavenanalyzer.controller;

import com.muratcan.yeldan.mavenanalyzer.dto.chart.BarChartDataResponse;
import com.muratcan.yeldan.mavenanalyzer.dto.chart.PieChartDataResponse;
import com.muratcan.yeldan.mavenanalyzer.dto.response.ChartResponse;
import com.muratcan.yeldan.mavenanalyzer.entity.DependencyAnalysis;
import com.muratcan.yeldan.mavenanalyzer.exception.ResourceNotFoundException;
import com.muratcan.yeldan.mavenanalyzer.repository.DependencyAnalysisRepository;
import com.muratcan.yeldan.mavenanalyzer.service.ChartCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private static final String ANALYSIS_NOT_FOUND_WITH_ID = "Analysis not found with ID: ";

    private final DependencyAnalysisRepository dependencyAnalysisRepository;
    private final ChartCacheService chartCacheService;


    @GetMapping("/dependency-updates/{analysisId}")
    @Operation(summary = "Get dependency updates chart", description = "Generate and retrieve a chart showing dependency update status")
    public ResponseEntity<ChartResponse> getDependencyUpdatesChart(@PathVariable Long analysisId) {
        log.info("Generating dependency updates chart for analysis ID: {}", analysisId);

        DependencyAnalysis analysis = getDependencyAnalysisEntityById(analysisId);
        ChartResponse chartResponse = chartCacheService.getCachedDependencyUpdateChart(analysisId, analysis);
        String fullPath = chartResponse.getChartPath();
        String fileName = fullPath.substring(fullPath.lastIndexOf('/') + 1);
        chartResponse.setChartPath(fileName);

        return ResponseEntity.ok(chartResponse);
    }

    @GetMapping("/vulnerabilities/{analysisId}")
    @Operation(summary = "Get vulnerabilities chart", description = "Generate and retrieve a chart showing vulnerability status")
    public ResponseEntity<ChartResponse> getVulnerabilitiesChart(@PathVariable Long analysisId) {
        log.info("Generating vulnerabilities chart for analysis ID: {}", analysisId);

        DependencyAnalysis analysis = getDependencyAnalysisEntityById(analysisId);
        ChartResponse chartResponse = chartCacheService.getCachedVulnerabilityChart(analysisId, analysis);
        String fullPath = chartResponse.getChartPath();
        String fileName = fullPath.substring(fullPath.lastIndexOf('/') + 1);
        chartResponse.setChartPath(fileName);

        return ResponseEntity.ok(chartResponse);
    }

    @GetMapping("/license-distribution/{analysisId}")
    @Operation(summary = "Get license distribution chart", description = "Generate and retrieve a chart showing license distribution")
    public ResponseEntity<ChartResponse> getLicenseDistributionChart(@PathVariable Long analysisId) {
        log.info("Generating license distribution chart for analysis ID: {}", analysisId);

        DependencyAnalysis analysis = getDependencyAnalysisEntityById(analysisId);
        ChartResponse chartResponse = chartCacheService.getCachedLicenseDistributionChart(analysisId, analysis);
        chartResponse.setTitle("License Distribution");
        chartResponse.setDescription("Distribution of licenses used by dependencies in the project");
        String fullPath = chartResponse.getChartPath();
        String fileName = fullPath.substring(fullPath.lastIndexOf('/') + 1);
        chartResponse.setChartPath(fileName);

        return ResponseEntity.ok(chartResponse);
    }

    @GetMapping("/image/{fileName:.+}")
    @Operation(summary = "Get chart image", description = "Retrieve a chart image by its file name")
    public ResponseEntity<Resource> getChartImage(@PathVariable String fileName) {
        log.info("Retrieving chart image: {}", fileName);

        try {
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

    @GetMapping("/data/dependency-status/{analysisId}")
    @Operation(summary = "Get dependency status chart data",
            description = "Retrieve raw data for client-side rendering of dependency status chart")
    public ResponseEntity<PieChartDataResponse> getDependencyStatusChartData(@PathVariable Long analysisId) {
        log.info("Generating dependency status chart data for analysis ID: {}", analysisId);

        DependencyAnalysis analysis = getDependencyAnalysisEntityById(analysisId);
        PieChartDataResponse chartData = chartCacheService.getCachedDependencyStatusChartData(analysisId, analysis);

        return ResponseEntity.ok(chartData);
    }

    @GetMapping("/data/vulnerability-status/{analysisId}")
    @Operation(summary = "Get vulnerability status chart data",
            description = "Retrieve raw data for client-side rendering of vulnerability status chart")
    public ResponseEntity<PieChartDataResponse> getVulnerabilityStatusChartData(@PathVariable Long analysisId) {
        log.info("Generating vulnerability status chart data for analysis ID: {}", analysisId);

        DependencyAnalysis analysis = getDependencyAnalysisEntityById(analysisId);
        PieChartDataResponse chartData = chartCacheService.getCachedVulnerabilityStatusChartData(analysisId, analysis);

        return ResponseEntity.ok(chartData);
    }

    @GetMapping("/data/vulnerability-severity/{analysisId}")
    @Operation(summary = "Get vulnerability severity chart data",
            description = "Retrieve raw data for client-side rendering of vulnerability severity chart")
    public ResponseEntity<BarChartDataResponse> getVulnerabilitySeverityChartData(@PathVariable Long analysisId) {
        log.info("Generating vulnerability severity chart data for analysis ID: {}", analysisId);

        DependencyAnalysis analysis = getDependencyAnalysisEntityById(analysisId);
        BarChartDataResponse chartData = chartCacheService.getCachedVulnerabilitySeverityChartData(analysisId, analysis);

        return ResponseEntity.ok(chartData);
    }

    @GetMapping("/data/license-distribution/{analysisId}")
    @Operation(summary = "Get license distribution chart data",
            description = "Retrieve raw data for client-side rendering of license distribution chart")
    public ResponseEntity<PieChartDataResponse> getLicenseDistributionChartData(@PathVariable Long analysisId) {
        log.info("Generating license distribution chart data for analysis ID: {}", analysisId);

        DependencyAnalysis analysis = getDependencyAnalysisEntityById(analysisId);
        PieChartDataResponse chartData = chartCacheService.getCachedLicenseDistributionChartData(analysisId, analysis);

        return ResponseEntity.ok(chartData);
    }

    private DependencyAnalysis getDependencyAnalysisEntityById(Long analysisId) {
        return dependencyAnalysisRepository.findById(analysisId)
                .orElseThrow(() -> new ResourceNotFoundException(ANALYSIS_NOT_FOUND_WITH_ID + analysisId));
    }
} 