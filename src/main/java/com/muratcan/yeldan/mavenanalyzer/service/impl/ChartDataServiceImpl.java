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

@Service
@RequiredArgsConstructor
@Slf4j
public class ChartDataServiceImpl implements ChartDataService {

    // Constants for severity levels
    private static final String SEVERITY_CRITICAL = "Critical";
    private static final String SEVERITY_HIGH = "High";
    private static final String SEVERITY_MEDIUM = "Medium";
    private static final String SEVERITY_LOW = "Low";
    private static final String SEVERITY_UNKNOWN = "Unknown";
    private static final String SEVERITY_NONE = "None";

    // Constants for severity text in summaries
    private static final String TEXT_CRITICAL = "critical";
    private static final String TEXT_HIGH = "high";
    private static final String TEXT_MEDIUM = "medium";
    private static final String TEXT_LOW = "low";
    private static final String TEXT_UNKNOWN = "unknown";

    // Constants for license information
    private static final String LICENSE_UNKNOWN = "Unknown";

    // Constants for colors
    private static final String COLOR_CRITICAL = "#d32f2f"; // dark red
    private static final String COLOR_HIGH = "#f44336"; // red
    private static final String COLOR_MEDIUM = "#ff9800"; // orange
    private static final String COLOR_LOW = "#ffeb3b"; // yellow
    private static final String COLOR_UNKNOWN = "#9e9e9e"; // gray
    private static final String COLOR_NO_VULNERABILITIES = "#4caf50"; // green

    // Constants for summary messages
    private static final String NO_DEPENDENCIES_FOUND = "No dependencies found";
    private static final String NO_VULNERABILITIES_FOUND = "No vulnerabilities found";
    private static final String FOUND_PREFIX = "Found ";
    private static final String NO_VULNERABILITIES_PLACEHOLDER = "No Vulnerabilities";

    private static String getSummary(long vulnerableDependencies, long safeDependencies) {
        String summary;
        if (vulnerableDependencies == 0 && safeDependencies == 0) {
            summary = NO_DEPENDENCIES_FOUND;
        } else if (vulnerableDependencies == 0) {
            summary = "All " + safeDependencies + " dependencies are safe";
        } else if (safeDependencies == 0) {
            summary = "All " + vulnerableDependencies + " dependencies have vulnerabilities";
        } else {
            summary = FOUND_PREFIX + vulnerableDependencies + " vulnerable dependencies out of " +
                    (vulnerableDependencies + safeDependencies) + " total dependencies";
        }
        return summary;
    }

    @Override
    public PieChartDataResponse generateDependencyStatusChartData(DependencyAnalysis analysis) {
        log.debug("Generating dependency status chart data for analysis ID: {}", analysis.getId());

        List<PieChartDataResponse.PieChartEntry> entries = createDependencyStatusEntries(analysis);
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

    private List<PieChartDataResponse.PieChartEntry> createDependencyStatusEntries(DependencyAnalysis analysis) {
        List<PieChartDataResponse.PieChartEntry> entries = new ArrayList<>();

        addEntryIfPositiveCount(entries,
                "Up-to-date", "Up-to-date",
                analysis.getUpToDateDependencies(), COLOR_NO_VULNERABILITIES); // green

        addEntryIfPositiveCount(entries,
                "Outdated", "Outdated",
                analysis.getOutdatedDependencies(), COLOR_MEDIUM); // orange

        addEntryIfPositiveCount(entries,
                SEVERITY_UNKNOWN, SEVERITY_UNKNOWN,
                analysis.getUnidentifiedDependencies(), COLOR_UNKNOWN); // grey

        return entries;
    }

    private void addEntryIfPositiveCount(List<PieChartDataResponse.PieChartEntry> entries,
                                         String id, String label,
                                         int count, String color) {
        if (count > 0) {
            entries.add(new PieChartDataResponse.PieChartEntry(id, label, count, color));
        }
    }

    private String createDependencyStatusSummary(DependencyAnalysis analysis, int total) {
        if (total == 0) {
            return NO_DEPENDENCIES_FOUND;
        }

        List<String> parts = new ArrayList<>();

        addStatusPartIfPositive(parts, analysis.getUpToDateDependencies(), "up-to-date");
        addStatusPartIfPositive(parts, analysis.getOutdatedDependencies(), "outdated");
        addStatusPartIfPositive(parts, analysis.getUnidentifiedDependencies(), TEXT_UNKNOWN);

        return formatSummaryFromParts(parts, "dependencies");
    }

    private void addStatusPartIfPositive(List<String> parts, int count, String description) {
        if (count > 0) {
            parts.add(count + " " + description);
        }
    }

    private String formatSummaryFromParts(List<String> parts, String itemType) {
        if (parts.isEmpty()) {
            return "No " + itemType + " found";
        }

        StringBuilder summary = new StringBuilder(FOUND_PREFIX);

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
                    COLOR_HIGH
            ));
        }

        if (safeDependencies > 0) {
            entries.add(new PieChartDataResponse.PieChartEntry(
                    "Safe",
                    "Safe",
                    safeDependencies,
                    COLOR_NO_VULNERABILITIES
            ));
        }

        String summary = getSummary(vulnerableDependencies, safeDependencies);

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

        Map<String, Integer> severityCounts = initializeSeverityCounts();

        int totalVulnerabilities = countVulnerabilitiesBySeverity(analysis, severityCounts);

        log.info("Found a total of {} vulnerabilities for analysis ID: {}", totalVulnerabilities, analysis.getId());

        List<BarChartDataResponse.BarChartEntry> entries = createChartEntries(severityCounts);

        String summary = createVulnerabilitySummary(severityCounts);

        return buildChartResponse(entries, summary);
    }

    private Map<String, Integer> initializeSeverityCounts() {
        Map<String, Integer> severityCounts = new HashMap<>();
        severityCounts.put(SEVERITY_CRITICAL, 0);
        severityCounts.put(SEVERITY_HIGH, 0);
        severityCounts.put(SEVERITY_MEDIUM, 0);
        severityCounts.put(SEVERITY_LOW, 0);
        severityCounts.put(SEVERITY_UNKNOWN, 0);
        return severityCounts;
    }

    private int countVulnerabilitiesBySeverity(DependencyAnalysis analysis, Map<String, Integer> severityCounts) {
        log.debug("Starting vulnerability severity count for analysis ID: {} with {} dependencies",
                analysis.getId(), analysis.getDependencies().size());

        int totalVulnerabilities = 0;

        for (Dependency dependency : analysis.getDependencies()) {
            totalVulnerabilities += processVulnerabilitiesForDependency(dependency, severityCounts);
        }

        return totalVulnerabilities;
    }

    private int processVulnerabilitiesForDependency(Dependency dependency, Map<String, Integer> severityCounts) {
        log.debug("Processing dependency {}:{} (isVulnerable={})",
                dependency.getGroupId(), dependency.getArtifactId(), dependency.getIsVulnerable());

        if (!Boolean.TRUE.equals(dependency.getIsVulnerable())) {
            return 0;
        }

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

    private void countVulnerabilityBySeverity(Vulnerability vulnerability, Map<String, Integer> severityCounts) {
        String severity = normalizeSeverity(vulnerability.getSeverity());

        if (severity.equals(SEVERITY_NONE)) {
            return;
        }

        severityCounts.put(severity, severityCounts.getOrDefault(severity, 0) + 1);

        log.debug("Counted vulnerability {} with severity {}", vulnerability.getName(), severity);
    }

    private String normalizeSeverity(String severity) {
        if (severity == null || severity.isEmpty() || severity.equalsIgnoreCase(SEVERITY_UNKNOWN)) {
            return SEVERITY_UNKNOWN;
        } else if (severity.equalsIgnoreCase(SEVERITY_CRITICAL)) {
            return SEVERITY_CRITICAL;
        } else if (severity.equalsIgnoreCase(SEVERITY_HIGH)) {
            return SEVERITY_HIGH;
        } else if (severity.equalsIgnoreCase(SEVERITY_MEDIUM)) {
            return SEVERITY_MEDIUM;
        } else if (severity.equalsIgnoreCase(SEVERITY_LOW)) {
            return SEVERITY_LOW;
        } else if (severity.equalsIgnoreCase(SEVERITY_NONE)) {
            return SEVERITY_NONE;
        } else {
            return SEVERITY_UNKNOWN;
        }
    }

    private List<BarChartDataResponse.BarChartEntry> createChartEntries(Map<String, Integer> severityCounts) {
        List<BarChartDataResponse.BarChartEntry> entries = new ArrayList<>();

        addEntryIfPositiveCount(entries, SEVERITY_CRITICAL, severityCounts.get(SEVERITY_CRITICAL), COLOR_CRITICAL);
        addEntryIfPositiveCount(entries, SEVERITY_HIGH, severityCounts.get(SEVERITY_HIGH), COLOR_HIGH);
        addEntryIfPositiveCount(entries, SEVERITY_MEDIUM, severityCounts.get(SEVERITY_MEDIUM), COLOR_MEDIUM);
        addEntryIfPositiveCount(entries, SEVERITY_LOW, severityCounts.get(SEVERITY_LOW), COLOR_LOW);
        addEntryIfPositiveCount(entries, SEVERITY_UNKNOWN, severityCounts.get(SEVERITY_UNKNOWN), COLOR_UNKNOWN);

        if (entries.isEmpty()) {
            entries.add(new BarChartDataResponse.BarChartEntry(
                    NO_VULNERABILITIES_PLACEHOLDER,
                    0,
                    COLOR_NO_VULNERABILITIES
            ));
        }

        return entries;
    }

    private void addEntryIfPositiveCount(List<BarChartDataResponse.BarChartEntry> entries,
                                         String label, int count, String color) {
        if (count > 0) {
            entries.add(new BarChartDataResponse.BarChartEntry(label, count, color));
        }
    }

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

    private String createVulnerabilitySummary(Map<String, Integer> severityCounts) {
        List<String> parts = new ArrayList<>();

        if (severityCounts.get(SEVERITY_CRITICAL) > 0) {
            parts.add(severityCounts.get(SEVERITY_CRITICAL) + " " + TEXT_CRITICAL);
        }

        if (severityCounts.get(SEVERITY_HIGH) > 0) {
            parts.add(severityCounts.get(SEVERITY_HIGH) + " " + TEXT_HIGH);
        }

        if (severityCounts.get(SEVERITY_MEDIUM) > 0) {
            parts.add(severityCounts.get(SEVERITY_MEDIUM) + " " + TEXT_MEDIUM);
        }

        if (severityCounts.get(SEVERITY_LOW) > 0) {
            parts.add(severityCounts.get(SEVERITY_LOW) + " " + TEXT_LOW);
        }

        if (severityCounts.get(SEVERITY_UNKNOWN) > 0) {
            parts.add(severityCounts.get(SEVERITY_UNKNOWN) + " " + TEXT_UNKNOWN);
        }

        if (parts.isEmpty()) {
            return NO_VULNERABILITIES_FOUND;
        }

        return FOUND_PREFIX + String.join(", ", parts) + " severity vulnerabilities";
    }

    @Override
    public PieChartDataResponse generateLicenseDistributionChartData(DependencyAnalysis analysis) {
        log.debug("Generating license distribution chart data for analysis ID: {}", analysis.getId());

        Map<String, Integer> licenseCounts = new HashMap<>();

        for (Dependency dependency : analysis.getDependencies()) {
            String license = dependency.getLicense();
            if (license == null || license.trim().isEmpty()) {
                license = LICENSE_UNKNOWN;
            }
            licenseCounts.put(license, licenseCounts.getOrDefault(license, 0) + 1);
        }

        List<PieChartDataResponse.PieChartEntry> entries = licenseCounts.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .map(entry -> {
                    String license = entry.getKey();
                    String color = String.format("#%06x", (0xCCCCCC + license.hashCode()) & 0xFFFFFF);
                    return new PieChartDataResponse.PieChartEntry(
                            license,
                            license,
                            entry.getValue(),
                            color
                    );
                })
                .toList();

        String summary;
        int totalLicenses = licenseCounts.size();
        int totalDependencies = analysis.getDependencies().size();

        if (totalDependencies == 0) {
            summary = NO_DEPENDENCIES_FOUND;
        } else if (totalLicenses == 0) {
            summary = "No license information available for any dependencies";
        } else {
            summary = FOUND_PREFIX + totalDependencies + " dependencies with " + totalLicenses + " different license types";

            Integer unknownCount = licenseCounts.get(LICENSE_UNKNOWN);
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