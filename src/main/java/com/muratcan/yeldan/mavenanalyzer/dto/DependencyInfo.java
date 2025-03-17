package com.muratcan.yeldan.mavenanalyzer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DependencyInfo {

    private String groupId;
    private String artifactId;
    private String version;
    private String scope;
    private String license;
    private boolean optional;
    private boolean isOptional;
    private boolean isBomManaged;
    private String originalDefinition;
    private String estimatedVersion;
} 