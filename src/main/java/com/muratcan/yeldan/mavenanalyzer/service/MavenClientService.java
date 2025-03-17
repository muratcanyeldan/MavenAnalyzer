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
} 