package com.muratcan.yeldan.mavenanalyzer.service.impl;

import com.muratcan.yeldan.mavenanalyzer.dto.AnalysisRequest;
import com.muratcan.yeldan.mavenanalyzer.dto.AnalysisResponse;
import com.muratcan.yeldan.mavenanalyzer.dto.AppSettingsRequest;
import com.muratcan.yeldan.mavenanalyzer.dto.DependencyInfo;
import com.muratcan.yeldan.mavenanalyzer.dto.DependencyResponse;
import com.muratcan.yeldan.mavenanalyzer.dto.HistoryResponse;
import com.muratcan.yeldan.mavenanalyzer.dto.MavenArtifactInfo;
import com.muratcan.yeldan.mavenanalyzer.dto.VulnerabilityResponse;
import com.muratcan.yeldan.mavenanalyzer.entity.Dependency;
import com.muratcan.yeldan.mavenanalyzer.entity.DependencyAnalysis;
import com.muratcan.yeldan.mavenanalyzer.entity.DependencyAnalysis.VulnerabilityCheckStatus;
import com.muratcan.yeldan.mavenanalyzer.entity.Project;
import com.muratcan.yeldan.mavenanalyzer.entity.Vulnerability;
import com.muratcan.yeldan.mavenanalyzer.exception.InactiveProjectException;
import com.muratcan.yeldan.mavenanalyzer.exception.ResourceNotFoundException;
import com.muratcan.yeldan.mavenanalyzer.repository.DependencyAnalysisRepository;
import com.muratcan.yeldan.mavenanalyzer.repository.DependencyRepository;
import com.muratcan.yeldan.mavenanalyzer.repository.ProjectRepository;
import com.muratcan.yeldan.mavenanalyzer.service.AppSettingsService;
import com.muratcan.yeldan.mavenanalyzer.service.ChartGeneratorService;
import com.muratcan.yeldan.mavenanalyzer.service.DependencyAnalysisService;
import com.muratcan.yeldan.mavenanalyzer.service.MavenClientService;
import com.muratcan.yeldan.mavenanalyzer.service.MavenMetadataService;
import com.muratcan.yeldan.mavenanalyzer.service.PomParserService;
import com.muratcan.yeldan.mavenanalyzer.service.VulnerabilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DependencyAnalysisServiceImpl implements DependencyAnalysisService {

    private final ProjectRepository projectRepository;
    private final DependencyAnalysisRepository dependencyAnalysisRepository;
    private final DependencyRepository dependencyRepository;
    private final PomParserService pomParserService;
    private final MavenClientService mavenClientService;
    private final ChartGeneratorService chartGeneratorService;
    private final VulnerabilityService vulnerabilityService;
    private final MavenMetadataService mavenMetadataService;
    private final AppSettingsService appSettingsService;

    @Override
    @Transactional
    public AnalysisResponse analyzeDependencies(AnalysisRequest request) {
        log.info("Analyzing dependencies. Project ID: {}, Check Vulnerabilities: {}, Check Licenses: {}, Notify on completion: {}",
                request.getProjectId(), request.isCheckVulnerabilities(), request.isCheckLicenses(), request.isNotifyOnCompletion());

        try {
            long startTime = System.currentTimeMillis();

            // If project ID is provided, find the project
            Project project = null;
            if (request.getProjectId() != null) {
                project = projectRepository.findById(request.getProjectId())
                        .orElseThrow(() -> new ResourceNotFoundException("Project not found with ID: " + request.getProjectId()));

                // Check if project is active
                if (project.getStatus() == Project.ProjectStatus.INACTIVE) {
                    throw new InactiveProjectException("Cannot analyze dependencies for inactive project: " + project.getName());
                }
            }

            // Parse POM content
            List<DependencyInfo> dependencyInfoList = pomParserService.parsePomDependencies(
                    request.getPomContent(),
                    request.getPomDirectoryPath(),
                    request.isIncludeTransitive()
            );
            log.info("Found {} dependencies in POM", dependencyInfoList.size());

            // Set the initial vulnerability check status
            VulnerabilityCheckStatus initialStatus = request.isCheckVulnerabilities()
                    ? VulnerabilityCheckStatus.IN_PROGRESS
                    : VulnerabilityCheckStatus.NOT_STARTED;

            // Create analysis entity
            DependencyAnalysis analysis = DependencyAnalysis.builder()
                    .project(project) // This can be null for standalone analyses
                    .pomContent(request.getPomContent())
                    .analysisDate(LocalDateTime.now())
                    .totalDependencies(dependencyInfoList.size())
                    .outdatedDependencies(0)
                    .upToDateDependencies(0)
                    .unidentifiedDependencies(0)
                    .vulnerabilityCheckStatus(initialStatus)
                    .notifyOnCompletion(request.isNotifyOnCompletion())
                    .build();

            // Now that we've properly initialized all required fields, save the analysis to get an ID
            analysis = dependencyAnalysisRepository.save(analysis);
            log.info("Created dependency analysis with ID: {}", analysis.getId());

            // Process each dependency
            final List<Dependency> dependencies = new ArrayList<>();

            // If license checking is requested, temporarily enable it in settings if not already enabled
            boolean originalLicenseCheckingState = false;
            if (request.isCheckLicenses()) {
                originalLicenseCheckingState = appSettingsService.isLicenseCheckingEnabled();
                if (!originalLicenseCheckingState) {
                    // Temporarily enable license checking for this analysis
                    AppSettingsRequest enableLicenseChecking = new AppSettingsRequest();
                    enableLicenseChecking.setLicenseCheckingEnabled(true);
                    appSettingsService.updateSettings(enableLicenseChecking);
                    log.info("Temporarily enabled license checking for analysis ID: {}", analysis.getId());
                }
            }

            for (DependencyInfo dependencyInfo : dependencyInfoList) {
                Dependency dependency = processDependency(dependencyInfo, analysis, request.isCheckVulnerabilities());
                dependencies.add(dependency);

                // Update dependency counters
                if (Boolean.TRUE.equals(dependency.getIsOutdated())) {
                    analysis.setOutdatedDependencies(analysis.getOutdatedDependencies() + 1);
                } else if (dependency.getLatestVersion() != null) {
                    analysis.setUpToDateDependencies(analysis.getUpToDateDependencies() + 1);
                } else {
                    analysis.setUnidentifiedDependencies(analysis.getUnidentifiedDependencies() + 1);
                }
            }

            // Save all dependencies in batch
            dependencyRepository.saveAll(dependencies);

            // Generate chart if needed
            if (!dependencies.isEmpty()) {
                String chartPath = chartGeneratorService.generateDependencyStatusChart(analysis).getChartPath();
                analysis.setChartPath(chartPath);
            }

            // Save the updated analysis with statistics
            analysis = dependencyAnalysisRepository.save(analysis);

            // If license checking was temporarily enabled, restore the original state
            if (request.isCheckLicenses() && !originalLicenseCheckingState) {
                AppSettingsRequest restoreLicenseChecking = new AppSettingsRequest();
                restoreLicenseChecking.setLicenseCheckingEnabled(originalLicenseCheckingState);
                appSettingsService.updateSettings(restoreLicenseChecking);
                log.info("Restored license checking state to: {} for analysis ID: {}",
                        originalLicenseCheckingState, analysis.getId());
            }

            // Important: Map to response DTO before starting vulnerability checks
            // so we can return immediately
            AnalysisResponse response = mapToAnalysisResponse(analysis);

            // If vulnerability checking is enabled, start the asynchronous vulnerability scanning process
            if (Boolean.TRUE.equals(request.isCheckVulnerabilities())) {
                log.info("Scheduling asynchronous vulnerability scanning for analysis ID: {} (will run after transaction commit)", analysis.getId());

                // Save a final reference to the analysis for use in the callback
                final DependencyAnalysis finalAnalysis = analysis;

                // Register a synchronization callback that will execute after the transaction successfully commits
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCommit() {
                        try {
                            log.info("Transaction committed: initiating vulnerability scans for analysis ID: {}", finalAnalysis.getId());

                            // Use the batch processing method instead of individual checks
                            vulnerabilityService.processDependenciesInBatches(finalAnalysis.getId());

                            log.info("Finished initiating batch vulnerability scans for analysis ID: {}", finalAnalysis.getId());
                        } catch (Exception e) {
                            log.error("Error in vulnerability scanning after transaction commit", e);
                        }
                    }
                });
            } else {
                // If we're not checking for vulnerabilities, mark it as completed
                analysis.setVulnerabilityCheckStatus(VulnerabilityCheckStatus.COMPLETED);
                dependencyAnalysisRepository.save(analysis);
            }

            long endTime = System.currentTimeMillis();
            log.info("Analysis completed in {} ms. Analysis ID: {}, Check Vulnerabilities: {}",
                    (endTime - startTime), analysis.getId(), request.isCheckVulnerabilities());

            log.info("Analysis completed successfully for ID: {}", analysis.getId());
            return response;
        } catch (Exception e) {
            log.error("Error analyzing dependencies", e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AnalysisResponse getAnalysisById(Long id) {
        log.debug("Fetching analysis with ID: {}", id);

        DependencyAnalysis analysis = dependencyAnalysisRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis not found with ID: " + id));

        return mapToAnalysisResponse(analysis);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistoryResponse> getAnalysisHistoryByProjectId(Long projectId) {
        log.debug("Fetching analysis history for project ID: {}", projectId);

        // Check if project exists
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project not found with ID: " + projectId);
        }

        List<DependencyAnalysis> analyses = dependencyAnalysisRepository.findByProjectIdOrderByAnalysisDateDesc(projectId);

        return analyses.stream()
                .map(this::mapToHistoryResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AnalysisResponse getLatestAnalysisByProjectId(Long projectId) {
        log.debug("Fetching latest analysis for project ID: {}", projectId);

        // Check if project exists
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project not found with ID: " + projectId);
        }

        DependencyAnalysis analysis = dependencyAnalysisRepository.findTopByProjectIdOrderByAnalysisDateDesc(projectId);

        if (analysis == null) {
            throw new ResourceNotFoundException("No analysis found for project ID: " + projectId);
        }

        return mapToAnalysisResponse(analysis);
    }

    @Override
    @Transactional
    public void deleteAnalysis(Long id) {
        log.debug("Deleting analysis with ID: {}", id);

        if (!dependencyAnalysisRepository.existsById(id)) {
            throw new ResourceNotFoundException("Analysis not found with ID: " + id);
        }

        dependencyAnalysisRepository.deleteById(id);
        log.info("Analysis deleted with ID: {}", id);
    }

    /**
     * Process a single dependency, check for updates and vulnerabilities
     */
    private Dependency processDependency(DependencyInfo dependencyInfo, DependencyAnalysis analysis, boolean checkVulnerabilities) {
        String groupId = dependencyInfo.getGroupId();
        String artifactId = dependencyInfo.getArtifactId();
        String currentVersion = prepareDependencyVersion(dependencyInfo);
        String scope = dependencyInfo.getScope() != null ? dependencyInfo.getScope() : "compile";

        log.debug("Processing dependency: {}:{} ({})", groupId, artifactId, currentVersion);

        // Process license information
        String license = processLicenseInformation(dependencyInfo, groupId, artifactId, currentVersion);

        // Process BOM managed dependencies
        DependencyVersionInfo versionInfo = processBomManagedDependency(dependencyInfo, currentVersion);

        // Build the dependency entity and check for latest version
        Dependency dependency = buildDependencyAndCheckLatestVersion(
                groupId, artifactId, currentVersion, scope, license,
                analysis, versionInfo.bomManaged(), versionInfo.estimatedVersion());

        return dependencyRepository.save(dependency);
    }

    /**
     * Prepare the dependency version, using a placeholder if necessary
     */
    private String prepareDependencyVersion(DependencyInfo dependencyInfo) {
        String version = dependencyInfo.getVersion();
        if (version == null) {
            log.debug("Version not specified for {}:{}, using placeholder version",
                    dependencyInfo.getGroupId(), dependencyInfo.getArtifactId());
            return "MANAGED_BY_BOM";
        }
        return version;
    }

    /**
     * Process license information from POM or Maven Central
     */
    private String processLicenseInformation(DependencyInfo dependencyInfo, String groupId,
                                             String artifactId, String currentVersion) {
        // Get license from the dependency info first (from POM)
        String license = dependencyInfo.getLicense() != null ? dependencyInfo.getLicense() : "Unknown";

        // If license not available in POM or is unknown, fetch from Maven Central
        if (license.isEmpty() || "Unknown".equalsIgnoreCase(license)) {
            String cleanVersion = extractCleanVersion(currentVersion, dependencyInfo);

            // Only try to fetch if we have a usable version
            if (!cleanVersion.equals("MANAGED_BY_BOM")) {
                license = fetchLicenseFromMavenCentral(groupId, artifactId, cleanVersion, license);
            }
        }

        return license;
    }

    /**
     * Extract clean version from a version string with annotations
     */
    private String extractCleanVersion(String version, DependencyInfo dependencyInfo) {
        String cleanVersion = version;

        // For BOM-managed with estimation
        if (version.contains("MANAGED_BY_BOM") && dependencyInfo.getEstimatedVersion() != null) {
            cleanVersion = dependencyInfo.getEstimatedVersion();
            log.debug("Using estimated version for license lookup: {}:{}:{}",
                    dependencyInfo.getGroupId(), dependencyInfo.getArtifactId(), cleanVersion);
        }
        // For resolved BOM versions  
        else if (version.contains("resolved from BOM")) {
            cleanVersion = version.replace(" (resolved from BOM)", "");
            log.debug("Using resolved BOM version for license lookup: {}:{}:{}",
                    dependencyInfo.getGroupId(), dependencyInfo.getArtifactId(), cleanVersion);
        }
        // For other versions with annotations
        else {
            cleanVersion = version.replaceAll("\\s+\\(.*\\)$", "");
        }

        return cleanVersion;
    }

    /**
     * Fetch license information from Maven Central
     */
    private String fetchLicenseFromMavenCentral(String groupId, String artifactId, String version, String defaultLicense) {
        try {
            Optional<String> fetchedLicense = mavenMetadataService.fetchLicenseInfo(groupId, artifactId, version);

            if (fetchedLicense.isPresent()) {
                String license = fetchedLicense.get();
                log.debug("Found license for {}:{}:{}: {}", groupId, artifactId, version, license);
                return license;
            } else {
                log.debug("No license found for {}:{}:{}", groupId, artifactId, version);
            }
        } catch (Exception e) {
            log.warn("Error fetching license for {}:{}:{}: {}", groupId, artifactId, version, e.getMessage());
        }

        return defaultLicense;
    }

    /**
     * Process BOM managed dependency information
     */
    private DependencyVersionInfo processBomManagedDependency(DependencyInfo dependencyInfo, String currentVersion) {
        boolean isBomManaged = false;
        String estimatedVersion = null;

        if (currentVersion.contains("MANAGED_BY_BOM")) {
            isBomManaged = true;

            // No longer trying to extract estimated versions from the version string
            // since we're not putting specific version estimates in there anymore
            if (dependencyInfo.getEstimatedVersion() != null) {
                estimatedVersion = dependencyInfo.getEstimatedVersion();
                log.debug("Using provided estimated version for {}:{}: {}",
                        dependencyInfo.getGroupId(), dependencyInfo.getArtifactId(), estimatedVersion);
            }
        } else if (currentVersion.contains("resolved from BOM")) {
            // For resolved BOM versions, extract the actual version
            isBomManaged = true;
            estimatedVersion = currentVersion.replace(" (resolved from BOM)", "");
            log.debug("Using resolved BOM version for {}:{}: {}",
                    dependencyInfo.getGroupId(), dependencyInfo.getArtifactId(), estimatedVersion);
        }

        return new DependencyVersionInfo(isBomManaged, estimatedVersion);
    }

    /**
     * Build the dependency entity and check for the latest version
     */
    private Dependency buildDependencyAndCheckLatestVersion(String groupId, String artifactId,
                                                            String currentVersion, String scope,
                                                            String license, DependencyAnalysis analysis,
                                                            boolean isBomManaged, String estimatedVersion) {
        // Build the dependency entity
        Dependency.DependencyBuilder builder = Dependency.builder()
                .analysis(analysis)
                .groupId(groupId)
                .artifactId(artifactId)
                .currentVersion(currentVersion)
                .isOutdated(false)
                .isVulnerable(false)
                .versionsBehind(0)
                .vulnerableCount(0)
                .scope(scope)
                .license(license == null || "null".equals(license) ? "Unknown" : license);

        // Try additional license lookup for BOM-managed dependencies with estimated version
        if (isBomManaged && estimatedVersion != null && (license == null || "Unknown".equalsIgnoreCase(license) || "null".equals(license))) {
            String updatedLicense = tryLicenseLookupWithEstimatedVersion(groupId, artifactId, estimatedVersion, license);
            if (updatedLicense == null || "null".equals(updatedLicense)) {
                updatedLicense = "Unknown";
            }
            if (!updatedLicense.equals(license)) {
                builder.license(updatedLicense);
            }
        }

        try {
            // Update with latest version information
            checkAndUpdateLatestVersion(builder, groupId, artifactId, currentVersion);

            // Determine the status (without considering vulnerabilities)
            setDependencyStatus(builder);

            if (isBomManaged) {
                builder.isBomManaged(true);
                if (estimatedVersion != null) {
                    builder.estimatedVersion(estimatedVersion);
                }
            }

        } catch (Exception e) {
            log.error("Error processing dependency {}:{}", groupId, artifactId, e);
            builder.status("Error");
        }

        return builder.build();
    }

    /**
     * Try to lookup license information using estimated version
     */
    private String tryLicenseLookupWithEstimatedVersion(String groupId, String artifactId,
                                                        String estimatedVersion, String defaultLicense) {
        try {
            Optional<String> betterLicense = mavenMetadataService.fetchLicenseInfo(groupId, artifactId, estimatedVersion);
            if (betterLicense.isPresent()) {
                String license = betterLicense.get();
                log.info("Found license using estimated/resolved version for {}:{}:{}: {}",
                        groupId, artifactId, estimatedVersion, license);
                return license;
            }
        } catch (Exception e) {
            log.warn("Error fetching license with estimated version for {}:{}:{}: {}",
                    groupId, artifactId, estimatedVersion, e.getMessage());
        }

        // Ensure we don't return "null" as a string
        if (defaultLicense == null || "null".equals(defaultLicense)) {
            return "Unknown";
        }
        return defaultLicense;
    }

    /**
     * Check for the latest version and update dependency information accordingly
     */
    private void checkAndUpdateLatestVersion(Dependency.DependencyBuilder builder,
                                             String groupId, String artifactId, String currentVersion) {
        Optional<MavenArtifactInfo> latestVersionInfo = mavenClientService.getLatestArtifactVersion(groupId, artifactId);

        if (latestVersionInfo.isPresent()) {
            String latestVersion = latestVersionInfo.get().getLatestVersion();
            builder.latestVersion(latestVersion);

            // Skip version comparison for unresolved BOM dependencies
            if (currentVersion.equals("MANAGED_BY_BOM") || currentVersion.contains("MANAGED_BY_BOM")) {
                builder.isOutdated(false);
                builder.versionsBehind(0);
            } else {
                updateVersionComparisonInformation(builder, groupId, artifactId, currentVersion, latestVersion);
            }
        } else {
            log.debug("Could not find latest version for {}:{}", groupId, artifactId);
        }
    }

    /**
     * Update version comparison information (outdated status, versions behind)
     */
    private void updateVersionComparisonInformation(Dependency.DependencyBuilder builder,
                                                    String groupId, String artifactId,
                                                    String currentVersion, String latestVersion) {
        // Clean the current version string to remove annotations like "(from parent)" or "(resolved from BOM)"
        String cleanedCurrentVersion = currentVersion.replaceAll("\\s+\\(.*\\)$", "");

        boolean isOutdated = !cleanedCurrentVersion.equals(latestVersion);
        builder.isOutdated(isOutdated);

        if (isOutdated) {
            int versionsBehind = mavenClientService.getVersionsBehind(groupId, artifactId, cleanedCurrentVersion);
            builder.versionsBehind(versionsBehind);
            log.debug("Dependency {}:{} is outdated. Current: {} (cleaned: {}), Latest: {}, Versions behind: {}",
                    groupId, artifactId, currentVersion, cleanedCurrentVersion, latestVersion, versionsBehind);
        }
    }

    /**
     * Set the dependency status based on its attributes
     */
    private void setDependencyStatus(Dependency.DependencyBuilder builder) {
        String status;
        Dependency dependency = builder.build();

        // Check if it's a BOM-managed dependency first
        if (dependency.getCurrentVersion().contains("MANAGED_BY_BOM")) {
            status = "BOM Managed";
        } else if (Boolean.TRUE.equals(dependency.getIsOutdated())) {
            status = "Outdated";
        } else if (dependency.getLatestVersion() != null) {
            status = "Up-to-date";
        } else {
            status = "Unknown";
        }
        builder.status(status);
    }

    /**
     * Map DependencyAnalysis entity to AnalysisResponse DTO
     */
    private AnalysisResponse mapToAnalysisResponse(DependencyAnalysis analysis) {
        List<DependencyResponse> dependencyResponses = analysis.getDependencies().stream()
                .map(this::mapToDependencyResponse)
                .collect(Collectors.toList());

        // Count vulnerable dependencies
        int vulnerableCount = (int) analysis.getDependencies().stream()
                .filter(d -> d.getVulnerabilities() != null && !d.getVulnerabilities().isEmpty())
                .count();

        // Count license issues
        int licenseIssues = calculateLicenseIssues(analysis);

        Project project = analysis.getProject();
        String projectName = project != null ? project.getName() : "Standalone Analysis";

        return AnalysisResponse.builder()
                .id(analysis.getId())
                .projectId(project != null ? project.getId() : null)
                .projectName(projectName)
                .analysisDate(analysis.getAnalysisDate())
                .totalDependencies(analysis.getTotalDependencies())
                .outdatedDependencies(analysis.getOutdatedDependencies())
                .upToDateDependencies(analysis.getUpToDateDependencies())
                .unidentifiedDependencies(analysis.getUnidentifiedDependencies())
                .vulnerabilityCheckStatus(analysis.getVulnerabilityCheckStatus())
                .vulnerableCount(vulnerableCount)
                .licenseIssues(licenseIssues)
                .chartPath(analysis.getChartPath())
                .notifyOnCompletion(analysis.isNotifyOnCompletion())
                .dependencies(dependencyResponses)
                .build();
    }

    /**
     * Calculates the number of license issues by checking dependencies against restricted licenses.
     *
     * @param analysis The dependency analysis entity
     * @return The count of dependencies with license issues
     */
    private int calculateLicenseIssues(DependencyAnalysis analysis) {
        // If license checking is not enabled, return 0
        if (!appSettingsService.isLicenseCheckingEnabled()) {
            return 0;
        }

        // Get the list of restricted licenses from app settings
        List<String> restrictedLicenses = appSettingsService.getRestrictedLicenses();
        if (restrictedLicenses.isEmpty()) {
            return 0;
        }

        // Count dependencies with restricted licenses
        return (int) analysis.getDependencies().stream()
                .filter(dependency -> {
                    String license = dependency.getLicense();
                    // Check if license is in the restricted list (case-insensitive)
                    return license != null && restrictedLicenses.stream()
                            .anyMatch(restricted -> license.toLowerCase().contains(restricted.toLowerCase()));
                })
                .count();
    }

    /**
     * Map Dependency entity to DependencyResponse DTO
     */
    private DependencyResponse mapToDependencyResponse(Dependency dependency) {
        List<VulnerabilityResponse> vulnerabilityResponses = dependency.getVulnerabilities().stream()
                .map(this::mapToVulnerabilityResponse)
                .toList();

        return DependencyResponse.builder()
                .id(dependency.getId())
                .groupId(dependency.getGroupId())
                .artifactId(dependency.getArtifactId())
                .currentVersion(dependency.getCurrentVersion())
                .latestVersion(dependency.getLatestVersion())
                .isOutdated(Boolean.TRUE.equals(dependency.getIsOutdated()))
                .isVulnerable(Boolean.TRUE.equals(dependency.getIsVulnerable()))
                .versionsBehind(dependency.getVersionsBehind())
                .vulnerableCount(dependency.getVulnerableCount())
                .scope(dependency.getScope())
                .license(dependency.getLicense())
                .status(dependency.getStatus())
                .isBomManaged(Boolean.TRUE.equals(dependency.getIsBomManaged()))
                .estimatedVersion(dependency.getEstimatedVersion())
                .vulnerabilities(vulnerabilityResponses)
                .build();
    }

    /**
     * Map Vulnerability entity to VulnerabilityResponse DTO
     */
    private VulnerabilityResponse mapToVulnerabilityResponse(Vulnerability vulnerability) {
        return VulnerabilityResponse.builder()
                .id(vulnerability.getId())
                .name(vulnerability.getName())
                .description(vulnerability.getDescription())
                .severity(vulnerability.getSeverity())
                .affectedVersions(vulnerability.getAffectedVersions())
                .fixedInVersion(vulnerability.getFixedInVersion())
                .build();
    }

    /**
     * Map DependencyAnalysis entity to HistoryResponse DTO
     */
    private HistoryResponse mapToHistoryResponse(DependencyAnalysis analysis) {
        Project project = analysis.getProject();
        String projectName = project != null ? project.getName() : "Standalone Analysis";

        // Count vulnerable dependencies
        int vulnerableCount = (int) analysis.getDependencies().stream()
                .filter(d -> d.getVulnerabilities() != null && !d.getVulnerabilities().isEmpty())
                .count();

        // Count license issues
        int licenseIssues = calculateLicenseIssues(analysis);

        return HistoryResponse.builder()
                .analysisId(analysis.getId())
                .projectId(project != null ? project.getId() : null)
                .projectName(projectName)
                .analysisDate(analysis.getAnalysisDate())
                .totalDependencies(analysis.getTotalDependencies())
                .outdatedDependencies(analysis.getOutdatedDependencies())
                .upToDateDependencies(analysis.getUpToDateDependencies())
                .unidentifiedDependencies(analysis.getUnidentifiedDependencies())
                .vulnerableCount(vulnerableCount)
                .licenseIssues(licenseIssues)
                .chartPath(analysis.getChartPath())
                .build();
    }

    @Override
    public List<HistoryResponse> getAllAnalysisHistory() {
        log.info("Retrieving all analysis history");
        List<DependencyAnalysis> analyses = dependencyAnalysisRepository.findAll();
        log.info("Found {} analyses in total", analyses.size());

        return analyses.stream()
                .map(this::mapToHistoryResponse)
                .toList();
    }

    @Override
    @Transactional
    public DependencyResponse updateDependencyVersion(Long dependencyId, String newVersion) {
        log.info("Updating dependency {} to version {}", dependencyId, newVersion);

        Dependency dependency = dependencyRepository.findById(dependencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Dependency not found with ID: " + dependencyId));

        // Check if dependency is BOM managed
        if (dependency.getIsBomManaged() != null && dependency.getIsBomManaged()) {
            throw new IllegalStateException("Cannot update version of BOM managed dependency: " +
                    dependency.getGroupId() + ":" + dependency.getArtifactId());
        }

        // Update the version
        dependency.setCurrentVersion(newVersion);

        // Re-check latest version information
        try {
            checkAndUpdateLatestVersion(Dependency.builder(), dependency.getGroupId(),
                    dependency.getArtifactId(), newVersion);
        } catch (Exception e) {
            log.error("Error checking latest version for updated dependency", e);
        }

        // Save the updated dependency
        dependency = dependencyRepository.save(dependency);

        return mapToDependencyResponse(dependency);
    }

    @Override
    public byte[] generateUpdatedPomFile(Long analysisId) {
        log.info("Generating updated POM file for analysis ID: {}", analysisId);

        DependencyAnalysis analysis = dependencyAnalysisRepository.findById(analysisId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis not found with ID: " + analysisId));

        try {
            // Parse the original POM content
            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model model = reader.read(new StringReader(analysis.getPomContent()));

            // Map to track property updates
            Map<String, String> propertiesToUpdate = new HashMap<>();

            // Check parent POM version
            if (model.getParent() != null) {
                String parentGroupId = model.getParent().getGroupId();
                String parentArtifactId = model.getParent().getArtifactId();
                String currentParentVersion = model.getParent().getVersion();

                // Try to get latest parent version from Maven Central
                Optional<MavenArtifactInfo> latestVersionInfo = mavenClientService.getLatestArtifactVersion(
                        parentGroupId, parentArtifactId);

                if (latestVersionInfo.isPresent()) {
                    String latestParentVersion = latestVersionInfo.get().getLatestVersion();
                    ComparableVersion current = new ComparableVersion(currentParentVersion);
                    ComparableVersion latest = new ComparableVersion(latestParentVersion);

                    if (latest.compareTo(current) > 0) {
                        log.info("Updating parent POM version for {}.{} from {} to {}",
                                parentGroupId, parentArtifactId, currentParentVersion, latestParentVersion);
                        model.getParent().setVersion(latestParentVersion);
                    } else {
                        log.info("Parent POM {}.{} is already using the latest version: {}",
                                parentGroupId, parentArtifactId, currentParentVersion);
                    }
                } else {
                    log.warn("Could not find latest version for parent POM {}.{}", parentGroupId, parentArtifactId);
                }
            }

            // First pass: identify properties that need updates and respect parent-managed dependencies
            model.getDependencies().forEach(dep -> {
                String version = dep.getVersion();
                String groupId = dep.getGroupId();
                String artifactId = dep.getArtifactId();

                // Skip dependencies that don't have version - they are managed by parent POM
                if (version == null || version.isEmpty()) {
                    log.info("Skipping version-less dependency {}.{} - it's managed by parent POM",
                            groupId, artifactId);
                    return;
                }

                analysis.getDependencies().stream()
                        .filter(d -> d.getGroupId().equals(groupId)
                                && d.getArtifactId().equals(artifactId)
                                && !Boolean.TRUE.equals(d.getIsBomManaged())
                                && d.getLatestVersion() != null)
                        .findFirst()
                        .ifPresent(d -> {
                            // Check if version uses property reference ${property.name}
                            if (version.startsWith("${") && version.endsWith("}")) {
                                String propertyName = version.substring(2, version.length() - 1);
                                propertiesToUpdate.put(propertyName, d.getLatestVersion());
                            }
                        });
            });

            // Update properties
            if (model.getProperties() != null && !propertiesToUpdate.isEmpty()) {
                propertiesToUpdate.forEach((key, value) -> {
                    if (model.getProperties().containsKey(key)) {
                        log.info("Updating property {} from {} to {}",
                                key, model.getProperties().getProperty(key), value);
                        model.getProperties().setProperty(key, value);
                    }
                });
            }

            // Second pass: update direct versions for dependencies not using properties
            // and not managed by parent
            model.getDependencies().forEach(dep -> {
                String version = dep.getVersion();

                // Skip dependencies without version (managed by parent)
                // or with property references (handled above)
                if (version == null || version.isEmpty() ||
                        (version.startsWith("${") && version.endsWith("}"))) {
                    return;
                }

                analysis.getDependencies().stream()
                        .filter(d -> d.getGroupId().equals(dep.getGroupId())
                                && d.getArtifactId().equals(dep.getArtifactId())
                                && !Boolean.TRUE.equals(d.getIsBomManaged())
                                && d.getLatestVersion() != null)
                        .findFirst()
                        .ifPresent(d -> {
                            log.info("Directly updating dependency {}.{} from {} to {}",
                                    dep.getGroupId(), dep.getArtifactId(), dep.getVersion(), d.getLatestVersion());
                            dep.setVersion(d.getLatestVersion());
                        });
            });

            // Write the updated model to a byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MavenXpp3Writer writer = new MavenXpp3Writer();
            writer.write(outputStream, model);

            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Error generating updated POM file", e);
            throw new RuntimeException("Failed to generate updated POM file", e);
        }
    }

    /**
     * Simple class to hold version information for a dependency
     */
    private record DependencyVersionInfo(boolean bomManaged, String estimatedVersion) {

    }
} 