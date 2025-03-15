package com.muratcan.yeldan.mavenanalyzer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MavenArtifactInfo {

    private String groupId;
    private String artifactId;
    private String latestVersion;
    private Instant timestamp;
    private Integer totalVersions;
} 