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

    private static final String LICENSE_UNKNOWN = "unknown";
    private static final String MANAGED_BY_BOM = "MANAGED_BY_BOM";
    private final MavenMetadataService mavenMetadataService;

    @Override
    public DependencyInfo enrichWithLicenseInfo(DependencyInfo dependencyInfo) {
        if (dependencyInfo.getLicense() == null ||
                dependencyInfo.getLicense().isEmpty() ||
                LICENSE_UNKNOWN.equalsIgnoreCase(dependencyInfo.getLicense()) ||
                "null".equalsIgnoreCase(dependencyInfo.getLicense())) {

            String groupId = dependencyInfo.getGroupId();
            String artifactId = dependencyInfo.getArtifactId();
            String version = dependencyInfo.getVersion();
            String cleanVersion = version;

            if (version.contains(MANAGED_BY_BOM) && dependencyInfo.getEstimatedVersion() != null) {
                cleanVersion = dependencyInfo.getEstimatedVersion();
                log.debug("Using estimated version for license lookup: {}:{}:{}",
                        groupId, artifactId, cleanVersion);
            } else if (cleanVersion.equals(MANAGED_BY_BOM)) {
                return dependencyInfo;
            } else if (version.contains("resolved from BOM")) {
                cleanVersion = version.replace(" (resolved from BOM)", "");
            } else {
                cleanVersion = version.replaceAll("\\s+\\(.*\\)$", "");
            }

            log.debug("Using {} for license lookup: {}:{}:{}", "regular version", groupId, artifactId, cleanVersion);
            String license = findLicenseInfo(groupId, artifactId, cleanVersion);
            dependencyInfo.setLicense(license);
        }

        return dependencyInfo;
    }

    @Override
    public String findLicenseInfo(String groupId, String artifactId, String version) {
        if (groupId == null || artifactId == null || version == null) {
            log.warn("Null parameters for license lookup: groupId={}, artifactId={}, version={}",
                    groupId, artifactId, version);
            return LICENSE_UNKNOWN;
        }

        if (groupId.isEmpty() || artifactId.isEmpty() || version.isEmpty() || version.equals(MANAGED_BY_BOM)) {
            log.warn("Empty or invalid parameters for license lookup: {}:{}:{}",
                    groupId, artifactId, version);
            return LICENSE_UNKNOWN;
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
                return LICENSE_UNKNOWN;
            }
        } catch (Exception e) {
            log.warn("Error fetching license for {}:{}:{}: {}", groupId, artifactId, version, e.getMessage());
            if (log.isDebugEnabled()) {
                log.debug("License lookup exception details:", e);
            }
            return LICENSE_UNKNOWN;
        }
    }
} 