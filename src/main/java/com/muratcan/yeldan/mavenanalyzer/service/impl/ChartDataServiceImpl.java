package com.muratcan.yeldan.mavenanalyzer.service.impl;

import com.muratcan.yeldan.mavenanalyzer.dto.chart.BarChartDataResponse;
import com.muratcan.yeldan.mavenanalyzer.dto.chart.PieChartDataResponse;
import com.muratcan.yeldan.mavenanalyzer.entity.Dependency;
import com.muratcan.yeldan.mavenanalyzer.entity.DependencyAnalysis;
import com.muratcan.yeldan.mavenanalyzer.entity.Vulnerability;
import com.muratcan.yeldan.mavenanalyzer.service.ChartDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of ChartDataService for client-side rendering
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChartDataServiceImpl implements ChartDataService {

    @Override
    public PieChartDataResponse generateDependencyStatusChartData(DependencyAnalysis analysis) {
        log.debug("Generating dependency status chart data for analysis ID: {}", analysis.getId());

        // Create chart entries
        List<PieChartDataResponse.PieChartEntry> entries = createDependencyStatusEntries(analysis);

        // Create a human-readable summary
        int total = analysis.getUpToDateDependencies() + analysis.getOutdatedDependencies() + analysis.getUnidentifiedDependencies();
        String summary = createDependencyStatusSummary(analysis, total);

        return PieChartDataResponse.builder()
                .chartType("pie")
                .title("Dependency Update Status")
                .description("Distribution of dependency update statuses in the project")
                .data(entries)
                .summary(summary)
                .build();
    }

    /**
     * Creates chart entries for dependency status distribution
     */
    private List<PieChartDataResponse.PieChartEntry> createDependencyStatusEntries(DependencyAnalysis analysis) {
        List<PieChartDataResponse.PieChartEntry> entries = new ArrayList<>();

        // Only add entries with non-zero values
        addEntryIfPositiveCount(entries,
                "Up-to-date", "Up-to-date",
                analysis.getUpToDateDependencies(), "#4caf50"); // green

        addEntryIfPositiveCount(entries,
                "Outdated", "Outdated",
                analysis.getOutdatedDependencies(), "#ff9800"); // orange

        addEntryIfPositiveCount(entries,
                "Unknown", "Unknown",
                analysis.getUnidentifiedDependencies(), "#9e9e9e"); // grey

        return entries;
    }

    /**
     * Add a pie chart entry if the count is positive
     */
    private void addEntryIfPositiveCount(List<PieChartDataResponse.PieChartEntry> entries,
                                         String id, String label,
                                         int count, String color) {
        if (count > 0) {
            entries.add(new PieChartDataResponse.PieChartEntry(id, label, count, color));
        }
    }

    /**
     * Creates a human-readable summary of dependency status distribution
     */
    private String createDependencyStatusSummary(DependencyAnalysis analysis, int total) {
        if (total == 0) {
            return "No dependencies found";
        }

        List<String> parts = new ArrayList<>();

        // Add parts for each dependency status
        addStatusPartIfPositive(parts, analysis.getUpToDateDependencies(), "up-to-date");
        addStatusPartIfPositive(parts, analysis.getOutdatedDependencies(), "outdated");
        addStatusPartIfPositive(parts, analysis.getUnidentifiedDependencies(), "unknown");

        return formatSummaryFromParts(parts, "dependencies");
    }

    /**
     * Add a status description part to the parts list if count is positive
     */
    private void addStatusPartIfPositive(List<String> parts, int count, String description) {
        if (count > 0) {
            parts.add(count + " " + description);
        }
    }

    /**
     * Format a summary string from parts list
     */
    private String formatSummaryFromParts(List<String> parts, String itemType) {
        if (parts.isEmpty()) {
            return "No " + itemType + " found";
        }

        StringBuilder summary = new StringBuilder("Found ");

        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                if (i == parts.size() - 1) {
                    summary.append(" and ");
                } else {
                    summary.append(", ");
                }
            }
            summary.append(parts.get(i));
        }

        summary.append(" ").append(itemType);
        return summary.toString();
    }

    @Override
    public PieChartDataResponse generateVulnerabilityStatusChartData(DependencyAnalysis analysis) {
        log.debug("Generating vulnerability status chart data for analysis ID: {}", analysis.getId());

        // Count vulnerabilities
        long vulnerableDependencies = analysis.getDependencies().stream()
                .filter(d -> Boolean.TRUE.equals(d.getIsVulnerable()))
                .count();

        long safeDependencies = analysis.getDependencies().size() - vulnerableDependencies;

        List<PieChartDataResponse.PieChartEntry> entries = new ArrayList<>();

        if (vulnerableDependencies > 0) {
            entries.add(new PieChartDataResponse.PieChartEntry(
                    "Vulnerable",
                    "Vulnerable",
                    vulnerableDependencies,
                    "#f44336" // red
            ));
        }

        if (safeDependencies > 0) {
            entries.add(new PieChartDataResponse.PieChartEntry(
                    "Safe",
                    "Safe",
                    safeDependencies,
                    "#4caf50" // green
            ));
        }

        // Create a human-readable summary
        String summary;
        if (vulnerableDependencies == 0 && safeDependencies == 0) {
            summary = "No dependencies found";
        } else if (vulnerableDependencies == 0) {
            summary = "All " + safeDependencies + " dependencies are safe";
        } else if (safeDependencies == 0) {
            summary = "All " + vulnerableDependencies + " dependencies have vulnerabilities";
        } else {
            summary = "Found " + vulnerableDependencies + " vulnerable dependencies out of " +
                    (vulnerableDependencies + safeDependencies) + " total dependencies";
        }

        return PieChartDataResponse.builder()
                .chartType("pie")
                .title("Vulnerability Status")
                .description("Distribution of vulnerable and safe dependencies")
                .data(entries)
                .summary(summary)
                .build();
    }

    @Override
    public BarChartDataResponse generateVulnerabilitySeverityChartData(DependencyAnalysis analysis) {
        log.debug("Generating vulnerability severity chart data for analysis ID: {}", analysis.getId());

        // Count vulnerabilities by severity
        Map<String, Integer> severityCounts = initializeSeverityCounts();

        // Count the vulnerabilities for each severity level
        int totalVulnerabilities = countVulnerabilitiesBySeverity(analysis, severityCounts);

        log.info("Found a total of {} vulnerabilities for analysis ID: {}", totalVulnerabilities, analysis.getId());

        // Create chart entries
        List<BarChartDataResponse.BarChartEntry> entries = createChartEntries(severityCounts);

        // Create summary text
        String summary = createVulnerabilitySummary(severityCounts);

        return buildChartResponse(entries, summary);
    }

    /**
     * Initialize the severity count map with default values
     */
    private Map<String, Integer> initializeSeverityCounts() {
        Map<String, Integer> severityCounts = new HashMap<>();
        severityCounts.put("Critical", 0);
        severityCounts.put("High", 0);
        severityCounts.put("Medium", 0);
        severityCounts.put("Low", 0);
        severityCounts.put("Unknown", 0);
        return severityCounts;
    }

    /**
     * Count vulnerabilities by severity across all dependencies
     */
    private int countVulnerabilitiesBySeverity(DependencyAnalysis analysis, Map<String, Integer> severityCounts) {
        // Add debug logging to help diagnose issues
        log.debug("Starting vulnerability severity count for analysis ID: {} with {} dependencies",
                analysis.getId(), analysis.getDependencies().size());

        int totalVulnerabilities = 0;

        for (Dependency dependency : analysis.getDependencies()) {
            totalVulnerabilities += processVulnerabilitiesForDependency(dependency, severityCounts);
        }

        return totalVulnerabilities;
    }

    /**
     * Process vulnerabilities for a single dependency
     */
    private int processVulnerabilitiesForDependency(Dependency dependency, Map<String, Integer> severityCounts) {
        // Log dependency info for debugging
        log.debug("Processing dependency {}:{} (isVulnerable={})",
                dependency.getGroupId(), dependency.getArtifactId(), dependency.getIsVulnerable());

        // If the dependency is not marked as vulnerable, skip it
        if (!Boolean.TRUE.equals(dependency.getIsVulnerable())) {
            return 0;
        }

        // Check if vulnerabilities collection is properly initialized
        if (dependency.getVulnerabilities() == null) {
            log.warn("Dependency {}:{} is marked as vulnerable but has null vulnerabilities collection",
                    dependency.getGroupId(), dependency.getArtifactId());
            return 0;
        }

        log.debug("Dependency {}:{} has {} vulnerabilities",
                dependency.getGroupId(), dependency.getArtifactId(), dependency.getVulnerabilities().size());

        int count = 0;

        for (Vulnerability vulnerability : dependency.getVulnerabilities()) {
            count++;
            countVulnerabilityBySeverity(vulnerability, severityCounts);
        }

        return count;
    }

    /**
     * Process a single vulnerability and update severity counts
     */
    private void countVulnerabilityBySeverity(Vulnerability vulnerability, Map<String, Integer> severityCounts) {
        // Normalize severity
        String severity = normalizeSeverity(vulnerability.getSeverity());

        // Skip "None" severity
        if (severity.equals("None")) {
            return;
        }

        // Count the vulnerability
        severityCounts.put(severity, severityCounts.getOrDefault(severity, 0) + 1);

        log.debug("Counted vulnerability {} with severity {}", vulnerability.getName(), severity);
    }

    /**
     * Normalize severity to handle case differences and null values
     */
    private String normalizeSeverity(String severity) {
        if (severity == null || severity.isEmpty() || severity.equalsIgnoreCase("unknown")) {
            return "Unknown";
        } else if (severity.equalsIgnoreCase("critical")) {
            return "Critical";
        } else if (severity.equalsIgnoreCase("high")) {
            return "High";
        } else if (severity.equalsIgnoreCase("medium")) {
            return "Medium";
        } else if (severity.equalsIgnoreCase("low")) {
            return "Low";
        } else if (severity.equalsIgnoreCase("none")) {
            return "None";
        } else {
            // Any unrecognized severity is treated as Unknown
            return "Unknown";
        }
    }

    /**
     * Create chart entries based on severity counts
     */
    private List<BarChartDataResponse.BarChartEntry> createChartEntries(Map<String, Integer> severityCounts) {
        List<BarChartDataResponse.BarChartEntry> entries = new ArrayList<>();

        addEntryIfPositiveCount(entries, "Critical", severityCounts.get("Critical"), "#d32f2f"); // dark red
        addEntryIfPositiveCount(entries, "High", severityCounts.get("High"), "#f44336"); // red
        addEntryIfPositiveCount(entries, "Medium", severityCounts.get("Medium"), "#ff9800"); // orange
        addEntryIfPositiveCount(entries, "Low", severityCounts.get("Low"), "#ffeb3b"); // yellow
        addEntryIfPositiveCount(entries, "Unknown", severityCounts.get("Unknown"), "#9e9e9e"); // gray

        // If no entries were added, add a placeholder entry for "No Vulnerabilities"
        if (entries.isEmpty()) {
            entries.add(new BarChartDataResponse.BarChartEntry(
                    "No Vulnerabilities",
                    0,
                    "#4caf50" // green for no vulnerabilities
            ));
        }

        return entries;
    }

    /**
     * Add a chart entry if the count is positive
     */
    private void addEntryIfPositiveCount(List<BarChartDataResponse.BarChartEntry> entries,
                                         String label, int count, String color) {
        if (count > 0) {
            entries.add(new BarChartDataResponse.BarChartEntry(label, count, color));
        }
    }

    /**
     * Build the final chart response
     */
    private BarChartDataResponse buildChartResponse(List<BarChartDataResponse.BarChartEntry> entries, String summary) {
        return BarChartDataResponse.builder()
                .chartType("bar")
                .title("Vulnerabilities by Severity")
                .description("Count of vulnerabilities by severity level")
                .data(entries)
                .keys(List.of("count"))
                .summary(summary)
                .build();
    }

    /**
     * Create a human-readable summary of vulnerability counts
     */
    private String createVulnerabilitySummary(Map<String, Integer> severityCounts) {
        int total = severityCounts.values().stream().mapToInt(Integer::intValue).sum();

        if (total == 0) {
            return "No vulnerabilities found";
        }

        StringBuilder summary = new StringBuilder("Found ");

        List<String> parts = new ArrayList<>();
        if (severityCounts.get("Critical") > 0) {
            parts.add(severityCounts.get("Critical") + " critical");
        }
        if (severityCounts.get("High") > 0) {
            parts.add(severityCounts.get("High") + " high");
        }
        if (severityCounts.get("Medium") > 0) {
            parts.add(severityCounts.get("Medium") + " medium");
        }
        if (severityCounts.get("Low") > 0) {
            parts.add(severityCounts.get("Low") + " low");
        }
        if (severityCounts.get("Unknown") > 0) {
            parts.add(severityCounts.get("Unknown") + " unspecified severity level");
        }

        if (parts.isEmpty()) {
            return "No vulnerabilities found";
        }

        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                if (i == parts.size() - 1) {
                    summary.append(" and ");
                } else {
                    summary.append(", ");
                }
            }
            summary.append(parts.get(i));
        }

        summary.append(" vulnerabilities");
        return summary.toString();
    }

    @Override
    public PieChartDataResponse generateLicenseDistributionChartData(DependencyAnalysis analysis) {
        log.debug("Generating license distribution chart data for analysis ID: {}", analysis.getId());

        // Count dependencies by license
        Map<String, Integer> licenseCounts = new HashMap<>();

        for (Dependency dependency : analysis.getDependencies()) {
            String license = dependency.getLicense();
            if (license == null || license.trim().isEmpty()) {
                license = "Unknown";
            }
            licenseCounts.put(license, licenseCounts.getOrDefault(license, 0) + 1);
        }

        // Create chart entries
        List<PieChartDataResponse.PieChartEntry> entries = licenseCounts.entrySet().stream()
                .filter(entry -> entry.getValue() > 0) // Only include non-zero values
                .map(entry -> {
                    String license = entry.getKey();
                    // Generate a consistent color based on the license name (simple hash)
                    String color = String.format("#%06x", (0xCCCCCC + license.hashCode()) & 0xFFFFFF);
                    return new PieChartDataResponse.PieChartEntry(
                            license,
                            license,
                            entry.getValue(),
                            color
                    );
                })
                .toList();

        // Create a human-readable summary
        String summary;
        int totalLicenses = licenseCounts.size();
        int totalDependencies = analysis.getDependencies().size();

        if (totalDependencies == 0) {
            summary = "No dependencies found";
        } else if (totalLicenses == 0) {
            summary = "No license information available for any dependencies";
        } else {
            summary = "Found " + totalDependencies + " dependencies with " + totalLicenses + " different license types";

            // Check for unknown licenses
            Integer unknownCount = licenseCounts.get("Unknown");
            if (unknownCount != null && unknownCount > 0) {
                summary += " (" + unknownCount + " with unknown license)";
            }
        }

        return PieChartDataResponse.builder()
                .chartType("pie")
                .title("License Distribution")
                .description("Distribution of licenses among project dependencies")
                .data(entries)
                .summary(summary)
                .build();
    }
} 