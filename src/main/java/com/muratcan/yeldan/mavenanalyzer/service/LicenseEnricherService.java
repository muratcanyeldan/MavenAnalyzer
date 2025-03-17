package com.muratcan.yeldan.mavenanalyzer.service;

import com.muratcan.yeldan.mavenanalyzer.dto.DependencyInfo;

public interface LicenseEnricherService {

    /**
     * Enrich the given dependency info with license information if available
     * This might fetch from Maven Central or other sources
     *
     * @param dependencyInfo The dependency info to enrich
     * @return The enriched dependency info (maybe the same object, modified)
     */
    DependencyInfo enrichWithLicenseInfo(DependencyInfo dependencyInfo);

    /**
     * Find license information for a Maven artifact
     *
     * @param groupId    The Maven artifact group ID
     * @param artifactId The Maven artifact ID
     * @param version    The Maven artifact version
     * @return A license string, or "unknown" if license info couldn't be determined
     */
    String findLicenseInfo(String groupId, String artifactId, String version);
} 