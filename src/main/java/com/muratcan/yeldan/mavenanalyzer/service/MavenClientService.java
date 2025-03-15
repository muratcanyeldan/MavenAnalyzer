package com.muratcan.yeldan.mavenanalyzer.service;

import com.muratcan.yeldan.mavenanalyzer.dto.MavenArtifactInfo;

import java.util.Optional;

public interface MavenClientService {

    /**
     * Get the latest version of a Maven artifact
     *
     * @param groupId    the group ID of the artifact
     * @param artifactId the artifact ID of the artifact
     * @return an Optional containing the latest version info, or empty if not found
     */
    Optional<MavenArtifactInfo> getLatestArtifactVersion(String groupId, String artifactId);

    /**
     * Check if there is a newer version available for the given artifact
     *
     * @param groupId        the group ID of the artifact
     * @param artifactId     the artifact ID of the artifact
     * @param currentVersion the current version of the artifact
     * @return true if a newer version is available, false otherwise
     */
    boolean isNewerVersionAvailable(String groupId, String artifactId, String currentVersion);

    /**
     * Calculate how many versions behind the current version is
     *
     * @param groupId        the group ID of the artifact
     * @param artifactId     the artifact ID of the artifact
     * @param currentVersion the current version of the artifact
     * @return the number of versions behind, or 0 if up to date or cannot be determined
     */
    int getVersionsBehind(String groupId, String artifactId, String currentVersion);
} 