package com.muratcan.yeldan.mavenanalyzer.service.impl;

import com.muratcan.yeldan.mavenanalyzer.dto.DependencyInfo;
import com.muratcan.yeldan.mavenanalyzer.service.LicenseEnricherService;
import com.muratcan.yeldan.mavenanalyzer.service.MavenMetadataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Implementation of LicenseEnricherService that uses MavenMetadataService
 * to fetch license information from Maven Central
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LicenseEnricherServiceImpl implements LicenseEnricherService {

    private final MavenMetadataService mavenMetadataService;

    @Override
    public DependencyInfo enrichWithLicenseInfo(DependencyInfo dependencyInfo) {
        // Only enrich if license is missing or unknown
        if (dependencyInfo.getLicense() == null ||
                dependencyInfo.getLicense().isEmpty() ||
                "unknown".equalsIgnoreCase(dependencyInfo.getLicense()) ||
                "null".equalsIgnoreCase(dependencyInfo.getLicense())) {

            // Get the artifacts coordinates
            String groupId = dependencyInfo.getGroupId();
            String artifactId = dependencyInfo.getArtifactId();
            String version = dependencyInfo.getVersion();

            // Only try to fetch if we have a version
            if (version != null && !version.isEmpty()) {
                String cleanVersion = version;
                String licenseSource = "regular version";

                // For BOM-managed with estimation
                if (version.contains("MANAGED_BY_BOM") && dependencyInfo.getEstimatedVersion() != null) {
                    cleanVersion = dependencyInfo.getEstimatedVersion();
                    licenseSource = "estimated version";
                }
                // For resolved BOM versions
                else if (version.contains("resolved from BOM")) {
                    cleanVersion = version.replace(" (resolved from BOM)", "");
                    licenseSource = "resolved BOM version";
                }
                // For versions with other annotations (e.g., "(managed)" or "(from parent)")
                else {
                    cleanVersion = version.replaceAll("\\s+\\(.*\\)$", "");
                }

                // Skip license lookup for bare "MANAGED_BY_BOM" with no estimated version
                if (cleanVersion.equals("MANAGED_BY_BOM")) {
                    log.debug("Skipping license lookup for {}:{} - no usable version available", groupId, artifactId);
                    return dependencyInfo;
                }

                log.debug("Using {} for license lookup: {}:{}:{}", licenseSource, groupId, artifactId, cleanVersion);
                String license = findLicenseInfo(groupId, artifactId, cleanVersion);
                dependencyInfo.setLicense(license);
            }
        }

        return dependencyInfo;
    }

    @Override
    public String findLicenseInfo(String groupId, String artifactId, String version) {
        // Validate parameters
        if (groupId == null || artifactId == null || version == null) {
            log.warn("Null parameters for license lookup: groupId={}, artifactId={}, version={}",
                    groupId, artifactId, version);
            return "unknown";
        }

        if (groupId.isEmpty() || artifactId.isEmpty() || version.isEmpty() || version.equals("MANAGED_BY_BOM")) {
            log.warn("Empty or invalid parameters for license lookup: {}:{}:{}",
                    groupId, artifactId, version);
            return "unknown";
        }

        try {
            log.debug("Fetching license info for {}:{}:{}", groupId, artifactId, version);

            Optional<String> licenseOpt = mavenMetadataService.fetchLicenseInfo(groupId, artifactId, version);

            if (licenseOpt.isPresent()) {
                String license = licenseOpt.get();
                log.debug("Found license for {}:{}:{}: {}", groupId, artifactId, version, license);
                return license;
            } else {
                log.debug("No license found for {}:{}:{} in Maven Central", groupId, artifactId, version);
                return "unknown";
            }
        } catch (Exception e) {
            log.warn("Error fetching license for {}:{}:{}: {}", groupId, artifactId, version, e.getMessage());
            if (log.isDebugEnabled()) {
                log.debug("License lookup exception details:", e);
            }
            return "unknown";
        }
    }
} 