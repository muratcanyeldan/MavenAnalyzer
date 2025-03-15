package com.muratcan.yeldan.mavenanalyzer.service;

import java.util.Optional;

/**
 * Service to fetch Maven artifact metadata from Maven repositories
 */
public interface MavenMetadataService {

    /**
     * Fetch license information for a Maven artifact
     *
     * @param groupId    The Maven artifact group ID
     * @param artifactId The Maven artifact ID
     * @param version    The Maven artifact version
     * @return An Optional containing the license name if found, empty otherwise
     */
    Optional<String> fetchLicenseInfo(String groupId, String artifactId, String version);

    /**
     * Estimate the version of a dependency managed by a BOM based on common frameworks
     *
     * @param groupId          The Maven artifact group ID
     * @param artifactId       The Maven artifact ID
     * @param parentGroupId    The parent/BOM group ID (if known)
     * @param parentArtifactId The parent/BOM artifact ID (if known)
     * @param parentVersion    The parent/BOM version (if known)
     * @return An Optional containing the estimated version if it can be determined, empty otherwise
     */
    Optional<String> estimateBomManagedVersion(String groupId, String artifactId,
                                               String parentGroupId, String parentArtifactId, String parentVersion);
} 