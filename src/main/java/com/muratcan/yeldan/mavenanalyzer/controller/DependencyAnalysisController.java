package com.muratcan.yeldan.mavenanalyzer.controller;

import com.muratcan.yeldan.mavenanalyzer.dto.AnalysisRequest;
import com.muratcan.yeldan.mavenanalyzer.dto.AnalysisResponse;
import com.muratcan.yeldan.mavenanalyzer.dto.DependencyResponse;
import com.muratcan.yeldan.mavenanalyzer.dto.HistoryResponse;
import com.muratcan.yeldan.mavenanalyzer.exception.InactiveProjectException;
import com.muratcan.yeldan.mavenanalyzer.service.DependencyAnalysisService;
import com.muratcan.yeldan.mavenanalyzer.service.VulnerabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/analyses")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Dependency Analysis", description = "API endpoints for analyzing Maven dependencies and checking for security vulnerabilities")
public class DependencyAnalysisController {

    private final DependencyAnalysisService dependencyAnalysisService;
    private final VulnerabilityService vulnerabilityService;

    @ExceptionHandler(InactiveProjectException.class)
    public ResponseEntity<Map<String, String>> handleInactiveProjectException(InactiveProjectException e) {
        log.warn("Attempt to analyze inactive project: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", e.getMessage()));
    }

    @PostMapping
    @Operation(
            summary = "Analyze dependencies",
            description = "Analyze dependencies in a POM file, check for updates, and scan for security vulnerabilities in real-time. " +
                    "Set checkVulnerabilities=true in the request to enable vulnerability scanning."
    )
    public ResponseEntity<AnalysisResponse> analyzeDependencies(@Valid @RequestBody AnalysisRequest request) {
        log.info("Analyzing dependencies for project ID: {}, vulnerability check enabled: {}",
                request.getProjectId(), request.isCheckVulnerabilities());
        AnalysisResponse response = dependencyAnalysisService.analyzeDependencies(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/project/{projectId}")
    @Operation(
            summary = "Analyze dependencies for a specific project",
            description = "Analyze dependencies in a POM file for a specific project and optionally scan for vulnerabilities. " +
                    "When checkVulnerabilities=true, each dependency is checked against a security database in real-time."
    )
    public ResponseEntity<AnalysisResponse> analyzeDependenciesForProject(
            @PathVariable Long projectId,
            @Valid @RequestBody AnalysisRequest request) {
        log.info("Analyzing dependencies for project ID (from path): {}, vulnerability check enabled: {}",
                projectId, request.isCheckVulnerabilities());

        // Ensure the project ID in the request matches the one in the path
        request.setProjectId(projectId);

        AnalysisResponse response = dependencyAnalysisService.analyzeDependencies(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get analysis by ID", description = "Retrieve a dependency analysis by its ID")
    public ResponseEntity<AnalysisResponse> getAnalysisById(@PathVariable Long id) {
        log.info("Fetching analysis with ID: {}", id);
        AnalysisResponse analysis = dependencyAnalysisService.getAnalysisById(id);
        return ResponseEntity.ok(analysis);
    }

    @GetMapping("/project/{projectId}")
    @Operation(summary = "Get analysis history", description = "Retrieve the history of dependency analyses for a project")
    public ResponseEntity<List<HistoryResponse>> getAnalysisHistory(@PathVariable Long projectId) {
        log.info("Fetching analysis history for project ID: {}", projectId);
        List<HistoryResponse> history = dependencyAnalysisService.getAnalysisHistoryByProjectId(projectId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/project/{projectId}/latest")
    @Operation(summary = "Get latest analysis", description = "Retrieve the latest dependency analysis for a project")
    public ResponseEntity<AnalysisResponse> getLatestAnalysis(@PathVariable Long projectId) {
        log.info("Fetching latest analysis for project ID: {}", projectId);
        AnalysisResponse analysis = dependencyAnalysisService.getLatestAnalysisByProjectId(projectId);
        return ResponseEntity.ok(analysis);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an analysis", description = "Delete a dependency analysis by its ID")
    public ResponseEntity<Void> deleteAnalysis(@PathVariable Long id) {
        log.info("Deleting analysis with ID: {}", id);
        dependencyAnalysisService.deleteAnalysis(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/vulnerability-status")
    @Operation(
            summary = "Get vulnerability scanning status",
            description = "Check the current status of vulnerability scanning for an analysis. " +
                    "Returns the vulnerability check status (NOT_STARTED, IN_PROGRESS, or COMPLETED) " +
                    "along with the count of vulnerabilities found so far."
    )
    public ResponseEntity<AnalysisResponse> getVulnerabilityStatus(@PathVariable Long id) {
        log.info("Checking vulnerability status for analysis ID: {}", id);
        AnalysisResponse analysis = dependencyAnalysisService.getAnalysisById(id);
        return ResponseEntity.ok(analysis);
    }

    @GetMapping
    @Operation(summary = "Get all analyses", description = "Retrieve all dependency analyses history across all projects")
    public ResponseEntity<List<HistoryResponse>> getAllAnalyses() {
        log.info("Fetching all analyses history");
        List<HistoryResponse> analyses = dependencyAnalysisService.getAllAnalysisHistory();
        return ResponseEntity.ok(analyses);
    }

    @DeleteMapping("/vulnerability-cache")
    @Operation(
            summary = "Clear all vulnerability caches",
            description = "Clear all cached vulnerability data. This forces fresh vulnerability checks on subsequent requests."
    )
    public ResponseEntity<Map<String, String>> clearAllVulnerabilityCaches() {
        log.info("Clearing all vulnerability caches");
        vulnerabilityService.clearAllVulnerabilityCaches();
        return ResponseEntity.ok(Map.of("message", "All vulnerability caches have been cleared"));
    }

    @DeleteMapping("/vulnerability-cache/{groupId}/{artifactId}/{version}")
    @Operation(
            summary = "Clear vulnerability cache for a specific dependency",
            description = "Clear cached vulnerability data for a specific dependency. " +
                    "This forces a fresh vulnerability check for this dependency on the next request."
    )
    public ResponseEntity<Map<String, String>> clearVulnerabilityCache(
            @PathVariable String groupId,
            @PathVariable String artifactId,
            @PathVariable String version) {
        log.info("Clearing vulnerability cache for dependency: {}:{}:{}", groupId, artifactId, version);
        vulnerabilityService.clearVulnerabilityCache(groupId, artifactId, version);
        return ResponseEntity.ok(Map.of(
                "message", String.format("Vulnerability cache cleared for %s:%s:%s", groupId, artifactId, version)
        ));
    }

    @PatchMapping("/dependencies/{id}/version")
    @Operation(
            summary = "Update dependency version",
            description = "Update the version of a specific dependency. This will not work for BOM managed dependencies."
    )
    public ResponseEntity<DependencyResponse> updateDependencyVersion(
            @PathVariable Long id,
            @RequestParam String newVersion) {
        log.info("Updating version for dependency ID: {} to version: {}", id, newVersion);
        DependencyResponse response = dependencyAnalysisService.updateDependencyVersion(id, newVersion);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/updated-pom")
    @Operation(
            summary = "Get updated POM file",
            description = "Generate an updated POM file with the latest non-BOM-managed dependency versions"
    )
    public ResponseEntity<Resource> getUpdatedPomFile(@PathVariable Long id) {
        log.info("Generating updated POM file for analysis ID: {}", id);
        byte[] pomContent = dependencyAnalysisService.generateUpdatedPomFile(id);

        ByteArrayResource resource = new ByteArrayResource(pomContent);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .header("Content-Disposition", "attachment; filename=pom.xml")
                .body(resource);
    }
} 