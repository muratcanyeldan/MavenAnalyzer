package com.muratcan.yeldan.mavenanalyzer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for dependency information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DependencyResponse {

    private Long id;
    private String groupId;
    private String artifactId;
    private String currentVersion;
    private String latestVersion;
    private boolean isOutdated;
    private boolean isVulnerable;
    private Integer versionsBehind;
    private Integer vulnerableCount;
    private String scope;
    private String license;
    private String status; // Will contain a human-readable status like "Up-to-date", "Outdated", "Vulnerable", etc.
    private boolean isBomManaged;
    private String estimatedVersion;
    private List<VulnerabilityResponse> vulnerabilities;
} 